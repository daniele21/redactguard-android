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
import io.github.daniele21.redactguard.domain.analysis.LocalAiRuntimeState
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Production RedactGuard Local AI composition over the published Harness Consumer SDK. */
internal class BinderAnalysisRuntimeComposition private constructor(
    private val client: BinderConsumerLocalLlmClient,
    private val lifecycleExecutor: ExecutorService,
    private val onStateChanged: (LocalAiRuntimeState) -> Unit,
) : AnalysisRuntimePort,
    AutoCloseable {
    private val configurationReady = AtomicBoolean(false)
    private val transportConnected = {
        client.connectionSnapshot.state == SharedRuntimeConnectionState.CONNECTED
    }
    private val presetSelection = ProcessLocalPresetSelection()
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
        )
    private val delegate =
        ControlPlaneAnalysisRuntime(
            delegate = consumerRuntime,
            controlPlane = controlPlane,
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

    fun selectPresetAt(index: Int): Boolean {
        val option =
            presetSelection.state.value.options
                .getOrNull(index) ?: return false
        return presetSelection.select(option.preset)
    }

    /**
     * Side-effect-free consumer-safe discovery for progressive readiness UI. Analysis still repeats
     * the authoritative discovery/activation handshake. Discovery must never load a model.
     */
    fun refreshPresetSelection() {
        if (!transportConnected()) return
        val wasReady = configurationReady.get()
        if (!wasReady) onStateChanged(LocalAiRuntimeState.CONNECTING)
        try {
            lifecycleExecutor.execute {
                runCatching { controlPlane.refreshPresetSelection() }
                    .fold(
                        onSuccess = {
                            configurationReady.set(true)
                            if (!wasReady) onStateChanged(LocalAiRuntimeState.CONNECTED)
                        },
                        onFailure = { failure ->
                            configurationReady.set(false)
                            onStateChanged(failure.toDiscoveryState())
                        },
                    )
            }
        } catch (_: RejectedExecutionException) {
            configurationReady.set(false)
            onStateChanged(LocalAiRuntimeState.DISCONNECTED)
        }
    }

    internal fun onTransportStateChanged(state: SharedRuntimeConnectionState) {
        if (state == SharedRuntimeConnectionState.CONNECTED) {
            onStateChanged(LocalAiRuntimeState.CONNECTING)
            refreshPresetSelection()
            return
        }
        configurationReady.set(false)
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
        try {
            client.connect()
        } catch (_: SecurityException) {
            configurationReady.set(false)
            onStateChanged(LocalAiRuntimeState.PERMISSION_DENIED)
        } catch (_: RuntimeException) {
            configurationReady.set(false)
            onStateChanged(LocalAiRuntimeState.DISCONNECTED)
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
        client.close()
        lifecycleExecutor.shutdownNow()
    }

    companion object {
        fun create(
            context: Context,
            onStateChanged: (LocalAiRuntimeState) -> Unit = {},
        ): BinderAnalysisRuntimeComposition {
            val compositionRef = AtomicReference<BinderAnalysisRuntimeComposition?>(null)
            val observer =
                SharedRuntimeConnectionObserver { snapshot ->
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
                onStateChanged = onStateChanged,
            ).also(compositionRef::set)
        }
    }
}

private fun Throwable.toDiscoveryState(): LocalAiRuntimeState =
    when ((this as? AnalysisRuntimeException)?.code) {
        AnalysisRuntimeFailureCode.DISCONNECTED -> LocalAiRuntimeState.DISCONNECTED

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
