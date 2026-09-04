package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.contracts.ConsumerInferenceJobId
import io.github.daniele21.localllm.contracts.ConsumerInferenceJobResponse
import io.github.daniele21.localllm.contracts.ConsumerLogicalJobClient
import io.github.daniele21.localllm.contracts.ConsumerLogicalJobSubmitRequest
import io.github.daniele21.localllm.contracts.UseCaseId
import io.github.daniele21.redactguard.domain.analysis.AnalysisChunk
import io.github.daniele21.redactguard.domain.analysis.AnalysisLimits
import io.github.daniele21.redactguard.domain.analysis.AnalysisOperationId
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimePort

/** Diagnostic-only boundary tracing for the explicit-cancel Two-APK investigation. */
internal class CancelTracingAnalysisRuntimePort(
    private val delegate: AnalysisRuntimePort,
) : AnalysisRuntimePort {
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
    ) {
        println("HARNEX_CANCEL_TRACE stage=redactguard_consumer_cancel_enter")
        delegate.cancel(operationId) {
            println("HARNEX_CANCEL_TRACE stage=redactguard_consumer_cancel_ack")
            onCancelled()
        }
    }

    override fun close(operationId: AnalysisOperationId) = delegate.close(operationId)
}

/**
 * Preserves the published Consumer logical-job contract while exposing whether RedactGuard really
 * reaches the SDK cancel boundary. Job IDs are contractually privacy-safe identifiers.
 */
internal class CancelTracingConsumerLogicalJobClient(
    private val delegate: ConsumerLogicalJobClient,
    private val transportConnected: () -> Boolean,
) : ConsumerLogicalJobClient {
    override fun submitLogicalGeneration(request: ConsumerLogicalJobSubmitRequest): ConsumerInferenceJobResponse =
        delegate.submitLogicalGeneration(request)

    override fun logicalJob(
        jobId: ConsumerInferenceJobId,
        useCaseId: UseCaseId,
    ): ConsumerInferenceJobResponse = delegate.logicalJob(jobId, useCaseId)

    override fun logicalJobResult(
        jobId: ConsumerInferenceJobId,
        useCaseId: UseCaseId,
    ): ConsumerInferenceJobResponse = delegate.logicalJobResult(jobId, useCaseId)

    override fun cancelLogicalJob(
        jobId: ConsumerInferenceJobId,
        useCaseId: UseCaseId,
    ) {
        val connected = transportConnected()
        println("HARNEX_CANCEL_TRACE stage=redactguard_sdk_cancel_call job_id=${jobId.value} connected=$connected")
        val outcome = runCatching { delegate.cancelLogicalJob(jobId, useCaseId) }
        outcome.fold(
            onSuccess = {
                println("HARNEX_CANCEL_TRACE stage=redactguard_sdk_cancel_return job_id=${jobId.value} connected=$connected")
            },
            onFailure = { failure ->
                val exception = failure::class.simpleName ?: "Throwable"
                println(
                    "HARNEX_CANCEL_TRACE stage=redactguard_sdk_cancel_exception job_id=${jobId.value} " +
                        "connected=$connected exception=$exception",
                )
            },
        )
        outcome.getOrThrow()
    }
}
