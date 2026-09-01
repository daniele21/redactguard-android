package io.github.daniele21.redactguard.infrastructure.localai

import android.content.Context
import io.github.daniele21.localllm.transport.binder.client.BinderConsumerLocalLlmClient
import io.github.daniele21.localllm.transport.binder.client.SharedRuntimeConnectionObserver
import io.github.daniele21.localllm.transport.binder.client.SharedRuntimeConnectionState
import io.github.daniele21.localllm.transport.binder.client.SharedRuntimeHostConfig
import io.github.daniele21.redactguard.BuildConfig
import io.github.daniele21.redactguard.domain.analysis.AnalysisChunk
import io.github.daniele21.redactguard.domain.analysis.AnalysisLimits
import io.github.daniele21.redactguard.domain.analysis.AnalysisOperationId
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeException
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeFailureCode
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimePort
import io.github.daniele21.redactguard.domain.analysis.LocalAiExecutionState
import io.github.daniele21.redactguard.domain.analysis.LocalAiRuntimeState
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Production RedactGuard Local AI composition over the published Harness Consumer SDK. */
internal class BinderAnalysisRuntimeComposition private constructor(
    private val client: BinderConsumerLocalLlmClient,
    private val lifecycleExecutor: ExecutorService,
    private val readinessExecutor: ScheduledExecutorService,
    private val onStateChanged: (LocalAiRuntimeState) -> Unit,
    private val onExecutionStateChanged: (AnalysisOperationId, LocalAiExecutionState) -> Unit,
    private val technicalDiagnostics: LocalAiTechnicalDiagnostics,
) : AnalysisRuntimePort,
    AutoCloseable {
    private val configurationReady = AtomicBoolean(false)
    private val transportConnected = {
        client.connectionSnapshot.state == SharedRuntimeConnectionState.CONNECTED
    }
    private val reconnectController =
        SharedRuntimeReconnectController(
            currentState = { client.connectionSnapshot.state },
            connect = client::connect,
            schedule = { delayMillis, task ->
                readinessExecutor.schedule(task, delayMillis, TimeUnit.MILLISECONDS)
            },
            onSynchronousFailure = ::handleSynchronousConnectFailure,
        )
    private val presetSelection = ProcessLocalPresetSelection()
    private val setupProjection = LocalAiSetupStateProjection()
    private val selectedPreset = { presetSelection.selectedPreset }
    private val consumerRuntime =
        ConsumerAnalysisRuntime(
            client = client,
            lifecycleExecutor = lifecycleExecutor,
            transportConnected = transportConnected,
            selectedPreset = selectedPreset,
        )
    private val controlPlane =
        ConsumerControlPlaneCoordinator(
            client = client,
            transportConnected = transportConnected,
            presetSelection = presetSelection,
            technicalDiagnostics = technicalDiagnostics,
        )
    private val readinessObserver =
        ConsumerRuntimeReadinessObserver(
            client = client,
            scheduler = readinessExecutor,
            transportConnected = transportConnected,
            onStateChanged = { operationId, state ->
                setupProjection.onExecutionState(state)
                onExecutionStateChanged(operationId, state)
            },
        )
    private val delegate =
        ControlPlaneAnalysisRuntime(
            delegate = consumerRuntime,
            controlPlane = controlPlane,
            readinessObserver = readinessObserver,
            lifecycleExecutor = lifecycleExecutor,
            selectedPreset = selectedPreset,
        )

    val connectionState: LocalAiRuntimeState
        get() =
            when (client.connectionSnapshot.state) {
                SharedRuntimeConnectionState.CONNECTED -> {
                    if (configurationReady.get()) LocalAiRuntimeState.CONNECTED else LocalAiRuntimeState.CONNECTING
                }

                else -> {
                    client.connectionSnapshot.state.toAppState()
                }
            }

    val presetSelectionState: StateFlow<LocalAiPresetSelectionState>
        get() = presetSelection.state

    val setupState: StateFlow<LocalAiSetupState>
        get() = setupProjection.state

    fun selectPresetAt(index: Int): Boolean {
        val option =
            presetSelection.state.value.options
                .getOrNull(index) ?: return false
        if (!presetSelection.select(option.preset)) return false
        configurationReady.set(false)
        setupProjection.onPresetSelected(option.preset)
        refreshPresetSelection()
        return true
    }

    /**
     * Consumer-safe setup inspection for progressive readiness UI. The Host read is passive: it
     * never activates, prepares or loads a model. Analysis still repeats the authoritative fresh
     * inspection immediately before activation.
     */
    fun refreshPresetSelection() {
        if (!transportConnected()) return
        val wasReady = configurationReady.get()
        if (!wasReady) onStateChanged(LocalAiRuntimeState.CONNECTING)
        try {
            lifecycleExecutor.execute {
                runCatching { controlPlane.inspectSetup(selectedPreset()) }
                    .fold(
                        onSuccess = { inspection -> applyInspectedSetup(inspection, wasReady) },
                        onFailure = { failure -> applySetupInspectionFailure(failure) },
                    )
            }
        } catch (_: RejectedExecutionException) {
            technicalDiagnostics.record(
                LocalAiTechnicalEvent(
                    step = "control-plane.discovery",
                    result = "FAILED",
                    reason = "EXECUTOR_REJECTED",
                ),
            )
            configurationReady.set(false)
            setupProjection.onSetupFailure(AnalysisRuntimeFailureCode.DISCONNECTED)
            onStateChanged(LocalAiRuntimeState.DISCONNECTED)
        }
    }

    internal fun onTransportStateChanged(state: SharedRuntimeConnectionState) {
        reconnectController.onStateChanged(state)
        if (state == SharedRuntimeConnectionState.CONNECTED) {
            setupProjection.onTransportConnected()
            onStateChanged(LocalAiRuntimeState.CONNECTING)
            refreshPresetSelection()
            return
        }
        configurationReady.set(false)
        setupProjection.onTransportDisconnected()
        onStateChanged(state.toAppState())
    }

    /**
     * Product-level safety boundary around the external Host connection.
     *
     * The Consumer SDK should already convert expected Binder failures into typed connection
     * states. RedactGuard still fails closed here so a synchronous platform/security failure can
     * never escape into Activity/ViewModel startup and terminate the process.
     */
    fun connect() {
        reconnectController.enable()
        try {
            client.connect()
        } catch (error: SecurityException) {
            handleSynchronousConnectFailure(error)
            reconnectController.onConnectFailure(error)
        } catch (error: RuntimeException) {
            handleSynchronousConnectFailure(error)
            reconnectController.onConnectFailure(error)
        }
    }

    override fun prepare(
        operationId: AnalysisOperationId,
        onResult: (Result<AnalysisLimits>) -> Unit,
    ) = delegate.prepare(operationId, onResult)

    override fun generate(
        operationId: AnalysisOperationId,
        chunk: AnalysisChunk,
        onResult: (Result<String>) -> Unit,
    ) = delegate.generate(operationId, chunk, onResult)

    override fun cancel(
        operationId: AnalysisOperationId,
        onCancelled: () -> Unit,
    ) = delegate.cancel(operationId, onCancelled)

    override fun close(operationId: AnalysisOperationId) = delegate.close(operationId)

    override fun close() {
        configurationReady.set(false)
        setupProjection.onTransportDisconnected()
        reconnectController.close()
        readinessExecutor.shutdownNow()
        client.close()
        lifecycleExecutor.shutdownNow()
    }

    private fun handleSynchronousConnectFailure(error: RuntimeException) {
        val permissionDenied = error is SecurityException
        technicalDiagnostics.record(
            LocalAiTechnicalEvent(
                step = "transport.connect",
                result = "FAILED",
                reason = if (permissionDenied) "SecurityException" else "RuntimeException",
            ),
        )
        configurationReady.set(false)
        setupProjection.onTransportDisconnected()
        onStateChanged(
            if (permissionDenied) {
                LocalAiRuntimeState.PERMISSION_DENIED
            } else {
                LocalAiRuntimeState.DISCONNECTED
            },
        )
    }

    private fun applyInspectedSetup(
        inspection: ConsumerControlPlaneSetupInspection,
        wasReady: Boolean,
    ) {
        val latestPreset = selectedPreset()
        if (latestPreset != null && latestPreset != inspection.selectedPreset.preset) return
        val committed = presetSelection.resolve(inspection.availablePresets, inspection.selectedPreset.preset)
        if (committed == null) {
            applySetupInspectionFailure(AnalysisRuntimeException(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE))
            return
        }
        setupProjection.onSetupResolved(inspection)
        configurationReady.set(true)
        if (!wasReady) onStateChanged(LocalAiRuntimeState.CONNECTED)
    }

    private fun applySetupInspectionFailure(failure: Throwable) {
        configurationReady.set(false)
        setupProjection.onSetupFailure((failure as? AnalysisRuntimeException)?.code)
        onStateChanged(failure.toDiscoveryState())
    }

    companion object {
        fun create(
            context: Context,
            onStateChanged: (LocalAiRuntimeState) -> Unit = {},
            onExecutionStateChanged: (AnalysisOperationId, LocalAiExecutionState) -> Unit = { _, _ -> },
        ): BinderAnalysisRuntimeComposition {
            val compositionRef = AtomicReference<BinderAnalysisRuntimeComposition?>(null)
            val technicalDiagnostics = AndroidLocalAiTechnicalDiagnostics
            val observer =
                SharedRuntimeConnectionObserver { snapshot ->
                    technicalDiagnostics.record(snapshot.toTechnicalEvent())
                    compositionRef.get()?.onTransportStateChanged(snapshot.state)
                        ?: onStateChanged(snapshot.state.toPreCompositionState())
                }
            val client =
                BinderConsumerLocalLlmClient.create(
                    context = context.applicationContext,
                    hostConfig =
                        SharedRuntimeHostConfig.create(
                            BuildConfig.SHARED_RUNTIME_HOST_PACKAGE,
                            BuildConfig.SHARED_RUNTIME_HOST_SERVICE,
                        ),
                    clientBuildId = "redactguard-${BuildConfig.VERSION_NAME}",
                    observer = observer,
                )
            return BinderAnalysisRuntimeComposition(
                client = client,
                lifecycleExecutor = Executors.newSingleThreadExecutor(),
                readinessExecutor = Executors.newSingleThreadScheduledExecutor(),
                onStateChanged = onStateChanged,
                onExecutionStateChanged = onExecutionStateChanged,
                technicalDiagnostics = technicalDiagnostics,
            ).also(compositionRef::set)
        }
    }
}

private fun Throwable.toDiscoveryState(): LocalAiRuntimeState =
    when ((this as? AnalysisRuntimeException)?.code) {
        AnalysisRuntimeFailureCode.DISCONNECTED,
        AnalysisRuntimeFailureCode.HOST_PROCESS_LOST,
        -> LocalAiRuntimeState.DISCONNECTED

        AnalysisRuntimeFailureCode.HOST_UNAVAILABLE,
        AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE,
        AnalysisRuntimeFailureCode.GENERATION_FAILED,
        AnalysisRuntimeFailureCode.INTERNAL_FAILURE,
        AnalysisRuntimeFailureCode.CANCELLED,
        null,
        -> LocalAiRuntimeState.INCOMPATIBLE
    }

private fun SharedRuntimeConnectionState.toPreCompositionState(): LocalAiRuntimeState =
    if (this == SharedRuntimeConnectionState.CONNECTED) LocalAiRuntimeState.CONNECTING else toAppState()

private fun SharedRuntimeConnectionState.toAppState(): LocalAiRuntimeState =
    when (this) {
        SharedRuntimeConnectionState.CONNECTED -> LocalAiRuntimeState.CONNECTING

        SharedRuntimeConnectionState.BINDING,
        SharedRuntimeConnectionState.NEGOTIATING,
        -> LocalAiRuntimeState.CONNECTING

        SharedRuntimeConnectionState.PERMISSION_DENIED -> LocalAiRuntimeState.PERMISSION_DENIED

        SharedRuntimeConnectionState.INCOMPATIBLE -> LocalAiRuntimeState.INCOMPATIBLE

        SharedRuntimeConnectionState.HOST_NOT_INSTALLED -> LocalAiRuntimeState.HOST_NOT_INSTALLED

        SharedRuntimeConnectionState.DISCONNECTED,
        SharedRuntimeConnectionState.CONNECTION_LOST,
        SharedRuntimeConnectionState.CLOSED,
        -> LocalAiRuntimeState.DISCONNECTED
    }
