package io.github.daniele21.redactguard.infrastructure.localai

import android.content.Context
import io.github.daniele21.localllm.transport.binder.client.BinderConsumerLocalLlmClient
import io.github.daniele21.localllm.transport.binder.client.SharedRuntimeConnectionObserver
import io.github.daniele21.localllm.transport.binder.client.SharedRuntimeConnectionSnapshot
import io.github.daniele21.localllm.transport.binder.client.SharedRuntimeConnectionState
import io.github.daniele21.localllm.transport.binder.client.SharedRuntimeHostConfig
import io.github.daniele21.redactguard.BuildConfig
import io.github.daniele21.redactguard.domain.analysis.AnalysisChunk
import io.github.daniele21.redactguard.domain.analysis.AnalysisLimits
import io.github.daniele21.redactguard.domain.analysis.AnalysisOperationId
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimePort
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Production RedactGuard Local AI composition over the published Harness Consumer SDK. */
internal class BinderAnalysisRuntimeComposition private constructor(
    private val client: BinderConsumerLocalLlmClient,
    private val lifecycleExecutor: ExecutorService,
) : AnalysisRuntimePort,
    AutoCloseable {
    private val delegate =
        ConsumerAnalysisRuntime(
            client = client,
            lifecycleExecutor = lifecycleExecutor,
            transportConnected = { client.connectionSnapshot.state == SharedRuntimeConnectionState.CONNECTED },
        )

    val connectionSnapshot: SharedRuntimeConnectionSnapshot
        get() = client.connectionSnapshot

    fun connect() = client.connect()

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
        client.close()
        lifecycleExecutor.shutdownNow()
    }

    companion object {
        fun create(
            context: Context,
            observer: SharedRuntimeConnectionObserver = SharedRuntimeConnectionObserver {},
        ): BinderAnalysisRuntimeComposition {
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
            return BinderAnalysisRuntimeComposition(client, Executors.newSingleThreadExecutor())
        }
    }
}
