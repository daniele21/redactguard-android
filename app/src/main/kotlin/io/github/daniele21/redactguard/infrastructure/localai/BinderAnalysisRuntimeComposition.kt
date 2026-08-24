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
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimePort
import io.github.daniele21.redactguard.domain.analysis.LocalAiRuntimeState
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

/** Production RedactGuard Local AI composition over the published Harness Consumer SDK. */
internal class BinderAnalysisRuntimeComposition private constructor(
    private val client: BinderConsumerLocalLlmClient,
    private val activation: ConsumerControlPlaneActivation,
    private val lifecycleExecutor: ExecutorService,
    private val onStateChanged: (LocalAiRuntimeState) -> Unit,
) : AnalysisRuntimePort,
    AutoCloseable {
    private val activatedClient = ActivationAwareConsumerLocalLlmClient(client, activation)
    private val delegate =
        ConsumerAnalysisRuntime(
            client = activatedClient,
            lifecycleExecutor = lifecycleExecutor,
            transportConnected = { client.connectionSnapshot.state == SharedRuntimeConnectionState.CONNECTED },
            selectedPreset = { activation.activePreset },
        )

    val connectionState: LocalAiRuntimeState
        get() = client.connectionSnapshot.state.toAppState()

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
            onStateChanged(LocalAiRuntimeState.PERMISSION_DENIED)
        } catch (_: RuntimeException) {
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
        runCatching { activation.deactivate() }
        client.close()
        lifecycleExecutor.shutdownNow()
    }

    companion object {
        fun create(
            context: Context,
            onStateChanged: (LocalAiRuntimeState) -> Unit = {},
        ): BinderAnalysisRuntimeComposition {
            val activationRef = AtomicReference<ConsumerControlPlaneActivation?>()
            val observer =
                SharedRuntimeConnectionObserver { snapshot ->
                    if (snapshot.state != SharedRuntimeConnectionState.CONNECTED) {
                        activationRef.get()?.invalidate()
                    }
                    onStateChanged(snapshot.state.toAppState())
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
            val activation = ConsumerControlPlaneActivation(client)
            activationRef.set(activation)
            return BinderAnalysisRuntimeComposition(
                client = client,
                activation = activation,
                lifecycleExecutor = Executors.newSingleThreadExecutor(),
                onStateChanged = onStateChanged,
            )
        }
    }
}

private fun SharedRuntimeConnectionState.toAppState(): LocalAiRuntimeState =
    when (this) {
        SharedRuntimeConnectionState.CONNECTED -> LocalAiRuntimeState.CONNECTED

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
