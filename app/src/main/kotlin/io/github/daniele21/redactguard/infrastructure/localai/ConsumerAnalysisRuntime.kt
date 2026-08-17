package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.contracts.ConsumerCapabilityErrorCode
import io.github.daniele21.localllm.contracts.ConsumerCapabilityResult
import io.github.daniele21.localllm.contracts.ConsumerContentType
import io.github.daniele21.localllm.contracts.ConsumerErrorCode
import io.github.daniele21.localllm.contracts.ConsumerExecutionIdentity
import io.github.daniele21.localllm.contracts.ConsumerFailure
import io.github.daniele21.localllm.contracts.ConsumerGenerationEvent
import io.github.daniele21.localllm.contracts.ConsumerGenerationHandle
import io.github.daniele21.localllm.contracts.ConsumerGenerationInput
import io.github.daniele21.localllm.contracts.ConsumerGenerationListener
import io.github.daniele21.localllm.contracts.ConsumerGenerationRequest
import io.github.daniele21.localllm.contracts.ConsumerGenerationStartResult
import io.github.daniele21.localllm.contracts.ConsumerLocalLlmClient
import io.github.daniele21.localllm.contracts.ConsumerOutputConstraint
import io.github.daniele21.localllm.contracts.ConsumerOutputConstraintKind
import io.github.daniele21.localllm.contracts.ConsumerPrepareRequest
import io.github.daniele21.localllm.contracts.ConsumerPrepareResult
import io.github.daniele21.localllm.contracts.ConsumerPreparedSelection
import io.github.daniele21.localllm.contracts.ConsumerReasoningCapability
import io.github.daniele21.localllm.contracts.ConsumerSessionResult
import io.github.daniele21.localllm.contracts.EffectiveConsumerReasoningMode
import io.github.daniele21.localllm.contracts.InferencePresetRef
import io.github.daniele21.localllm.contracts.RequestId
import io.github.daniele21.localllm.contracts.SessionId
import io.github.daniele21.localllm.contracts.SessionKind
import io.github.daniele21.localllm.contracts.UseCaseCapabilities
import io.github.daniele21.localllm.contracts.UseCaseId
import io.github.daniele21.localllm.contracts.UseCaseReadiness
import io.github.daniele21.redactguard.domain.analysis.AnalysisChunk
import io.github.daniele21.redactguard.domain.analysis.AnalysisLimits
import io.github.daniele21.redactguard.domain.analysis.AnalysisOperationId
import io.github.daniele21.redactguard.domain.analysis.AnalysisProtocol
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeException
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeFailureCode
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimePort
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

/** Strict Consumer API adapter. Model choice remains host-owned. */
internal class ConsumerAnalysisRuntime(
    private val client: ConsumerLocalLlmClient,
    private val lifecycleExecutor: Executor,
    private val transportConnected: () -> Boolean = { true },
    private val useCaseId: UseCaseId = DOCUMENT_PII_USE_CASE,
) : AnalysisRuntimePort {
    private val operations = ConcurrentHashMap<AnalysisOperationId, ConsumerOperation>()

    override fun prepare(
        operationId: AnalysisOperationId,
        onResult: (Result<AnalysisLimits>) -> Unit,
    ) {
        val operation = ConsumerOperation(onPrepared = onResult)
        check(operations.putIfAbsent(operationId, operation) == null) { "Duplicate analysis operation ID" }
        try {
            lifecycleExecutor.execute { prepareOnExecutor(operationId, operation) }
        } catch (_: RejectedExecutionException) {
            operations.remove(operationId, operation)
            onResult(Result.failure(runtimeFailure(AnalysisRuntimeFailureCode.DISCONNECTED)))
        }
    }

    override fun generate(
        operationId: AnalysisOperationId,
        chunk: AnalysisChunk,
        onResult: (Result<String>) -> Unit,
    ) {
        val operation = operations[operationId]
        if (operation == null) {
            onResult(Result.failure(runtimeFailure(AnalysisRuntimeFailureCode.DISCONNECTED)))
            return
        }
        val generation =
            synchronized(operation) {
                val limits = operation.limits ?: return@synchronized null
                val sessionId = operation.sessionId ?: return@synchronized null
                if (operation.cancelled || operation.activeGeneration != null || !fits(chunk, limits)) {
                    return@synchronized null
                }
                ActiveGeneration(requestId(operationId, chunk.ordinal), onResult).also {
                    operation.activeGeneration = it
                    operation.sessionId = sessionId
                }
            }
        if (generation == null) {
            onResult(Result.failure(runtimeFailure(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)))
            return
        }

        val sessionId = requireNotNull(operation.sessionId)
        val start =
            runCatching {
                client.generate(
                    ConsumerGenerationRequest(
                        requestId = generation.requestId,
                        sessionId = sessionId,
                        input = ConsumerGenerationInput.Text(composeInput(chunk)),
                        outputConstraint = ConsumerOutputConstraint.JsonSchema(AnalysisProtocol.outputJsonSchema),
                    ),
                    ConsumerGenerationListener { event -> handleEvent(operation, generation, event) },
                )
            }.getOrElse {
                finishGeneration(operation, generation, Result.failure(runtimeFailure(disconnectedOrGenerationFailure())))
                return
            }

        when (start) {
            is ConsumerGenerationStartResult.Accepted -> {
                val cancelImmediately =
                    synchronized(operation) {
                        generation.handle = start.handle
                        generation.terminal.get() || operation.cancelled
                    }
                if (cancelImmediately) start.handle.cancel()
            }

            is ConsumerGenerationStartResult.Rejected -> {
                finishGeneration(
                    operation,
                    generation,
                    Result.failure(runtimeFailure(mapConsumerFailure(start.failure))),
                )
            }
        }
    }

    override fun cancel(
        operationId: AnalysisOperationId,
        onCancelled: () -> Unit,
    ) {
        val operation = operations[operationId]
        if (operation == null) {
            onCancelled()
            return
        }
        val active =
            synchronized(operation) {
                operation.cancelled = true
                operation.cancelAcknowledgement = onCancelled
                operation.activeGeneration
            }
        if (active != null) {
            active.handle?.cancel()
        } else if (!operation.preparing) {
            closeCancelledOperation(operationId, operation)
        }
    }

    override fun close(operationId: AnalysisOperationId) {
        val operation = operations.remove(operationId) ?: return
        val active =
            synchronized(operation) {
                operation.cancelled = true
                operation.activeGeneration.also { operation.activeGeneration = null }
            }
        active?.handle?.cancel()
        operation.sessionId?.let { runCatching { client.closeSession(it) } }
    }

    private fun prepareOnExecutor(
        operationId: AnalysisOperationId,
        operation: ConsumerOperation,
    ) {
        val prepared = runCatching(::prepareConsumer)
        val value = prepared.getOrNull()
        val cancelled =
            synchronized(operation) {
                operation.preparing = false
                if (value != null) {
                    operation.sessionId = value.sessionId
                    operation.limits = value.limits
                    operation.capabilityRevision = value.capabilityRevision
                    operation.preset = value.preset
                }
                operation.cancelled
            }
        if (cancelled) {
            value?.sessionId?.let { runCatching { client.closeSession(it) } }
            operations.remove(operationId, operation)
            takeCancellationAcknowledgement(operation)?.invoke()
            return
        }
        if (value == null) {
            operations.remove(operationId, operation)
            operation.onPrepared(Result.failure(prepared.exceptionOrNull() ?: runtimeFailure(disconnectedOrGenerationFailure())))
        } else {
            operation.onPrepared(Result.success(value.limits))
        }
    }

    private fun prepareConsumer(): PreparedOperation {
        val capabilities =
            when (val result = client.capabilities(useCaseId)) {
                is ConsumerCapabilityResult.Available -> result.capabilities
                is ConsumerCapabilityResult.Rejected -> throw runtimeFailure(mapCapabilityFailure(result.code))
            }
        validateCapabilities(capabilities)
        val selection =
            when (val result = client.prepare(ConsumerPrepareRequest(useCaseId))) {
                is ConsumerPrepareResult.Prepared -> result.selection
                is ConsumerPrepareResult.Rejected -> throw runtimeFailure(mapConsumerFailure(result.failure))
            }
        validatePreparedSelection(selection, capabilities)
        val sessionId =
            when (val result = client.createSession(selection.preparedId)) {
                is ConsumerSessionResult.Created -> result.sessionId
                is ConsumerSessionResult.Rejected -> throw runtimeFailure(mapConsumerFailure(result.failure))
            }
        return PreparedOperation(
            sessionId = sessionId,
            limits = AnalysisLimits(capabilities.limits.maxInputCharacters, capabilities.limits.maxJsonSchemaCharacters),
            capabilityRevision = capabilities.capabilityRevision,
            preset = requireNotNull(selection.preset),
        )
    }

    private fun validateCapabilities(capabilities: UseCaseCapabilities) {
        val readinessFailure =
            when (capabilities.readiness) {
                UseCaseReadiness.READY, UseCaseReadiness.AVAILABLE_REQUIRES_PREPARATION -> null
                UseCaseReadiness.UNAVAILABLE_MODEL -> AnalysisRuntimeFailureCode.HOST_UNAVAILABLE
                UseCaseReadiness.UNAVAILABLE_HOST_POLICY, UseCaseReadiness.INCOMPATIBLE ->
                    AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE
            }
        readinessFailure?.let { throw runtimeFailure(it) }
        val compatible =
            capabilities.useCaseId == useCaseId &&
                capabilities.presets.size == 1 &&
                capabilities.presets.single().isDefault &&
                capabilities.presets.single().ref == capabilities.defaultPreset &&
                capabilities.outputConstraints == setOf(ConsumerOutputConstraintKind.JSON_SCHEMA) &&
                capabilities.defaultOutputConstraint == ConsumerOutputConstraintKind.JSON_SCHEMA &&
                capabilities.sessionKinds == setOf(SessionKind.STATELESS) &&
                capabilities.defaultSessionKind == SessionKind.STATELESS &&
                capabilities.reasoning == ConsumerReasoningCapability.NOT_SUPPORTED
        if (!compatible) throw runtimeFailure(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)
    }

    private fun validatePreparedSelection(
        selection: ConsumerPreparedSelection,
        capabilities: UseCaseCapabilities,
    ) {
        val compatible =
            selection.useCaseId == useCaseId &&
                selection.capabilityRevision == capabilities.capabilityRevision &&
                selection.preset == capabilities.defaultPreset &&
                selection.reasoningMode == EffectiveConsumerReasoningMode.DISABLED &&
                selection.outputConstraint == ConsumerOutputConstraintKind.JSON_SCHEMA &&
                selection.sessionKind == SessionKind.STATELESS
        if (!compatible) throw runtimeFailure(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)
    }

    private fun handleEvent(
        operation: ConsumerOperation,
        generation: ActiveGeneration,
        event: ConsumerGenerationEvent,
    ) {
        if (event.requestId != generation.requestId) {
            generation.handle?.cancel()
            finishGeneration(operation, generation, Result.failure(runtimeFailure(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)))
            return
        }
        when (event) {
            is ConsumerGenerationEvent.Queued, is ConsumerGenerationEvent.Started -> Unit
            is ConsumerGenerationEvent.Prepared -> {
                if (!executionMatches(event.execution, operation)) {
                    generation.handle?.cancel()
                    finishGeneration(
                        operation,
                        generation,
                        Result.failure(runtimeFailure(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)),
                    )
                }
            }

            is ConsumerGenerationEvent.ContentDelta -> {
                if (event.contentType == ConsumerContentType.REASONING) {
                    generation.handle?.cancel()
                    finishGeneration(
                        operation,
                        generation,
                        Result.failure(runtimeFailure(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)),
                    )
                }
            }

            is ConsumerGenerationEvent.Completed -> {
                val valid = event.result.surfacedReasoning.isNullOrEmpty() && executionMatches(event.result.execution, operation)
                finishGeneration(
                    operation,
                    generation,
                    if (valid) Result.success(event.result.answer) else {
                        Result.failure(runtimeFailure(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE))
                    },
                )
            }

            is ConsumerGenerationEvent.Failed -> {
                finishGeneration(operation, generation, Result.failure(runtimeFailure(mapConsumerFailure(event.failure))))
            }
        }
    }

    private fun executionMatches(
        execution: ConsumerExecutionIdentity,
        operation: ConsumerOperation,
    ): Boolean =
        execution.useCaseId == useCaseId &&
            execution.capabilityRevision == operation.capabilityRevision &&
            execution.preset == operation.preset &&
            execution.reasoningMode == EffectiveConsumerReasoningMode.DISABLED &&
            execution.outputConstraint == ConsumerOutputConstraintKind.JSON_SCHEMA &&
            execution.sessionKind == SessionKind.STATELESS

    private fun finishGeneration(
        operation: ConsumerOperation,
        generation: ActiveGeneration,
        result: Result<String>,
    ) {
        if (!generation.terminal.compareAndSet(false, true)) return
        val cancelled =
            synchronized(operation) {
                if (operation.activeGeneration === generation) operation.activeGeneration = null
                operation.cancelled
            }
        if (cancelled) takeCancellationAcknowledgement(operation)?.invoke() else generation.onResult(result)
    }

    private fun closeCancelledOperation(
        operationId: AnalysisOperationId,
        operation: ConsumerOperation,
    ) {
        operations.remove(operationId, operation)
        operation.sessionId?.let { runCatching { client.closeSession(it) } }
        takeCancellationAcknowledgement(operation)?.invoke()
    }

    private fun mapCapabilityFailure(code: ConsumerCapabilityErrorCode): AnalysisRuntimeFailureCode =
        when (code) {
            ConsumerCapabilityErrorCode.MODEL_UNAVAILABLE -> AnalysisRuntimeFailureCode.HOST_UNAVAILABLE
            ConsumerCapabilityErrorCode.CAPABILITY_INCOMPATIBLE -> disconnectedOrCapabilityFailure()
            else -> AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE
        }

    private fun mapConsumerFailure(failure: ConsumerFailure): AnalysisRuntimeFailureCode =
        when (failure.code) {
            ConsumerErrorCode.MODEL_UNAVAILABLE -> AnalysisRuntimeFailureCode.HOST_UNAVAILABLE
            ConsumerErrorCode.CANCELLED -> AnalysisRuntimeFailureCode.CANCELLED
            ConsumerErrorCode.RUNTIME_FAILURE, ConsumerErrorCode.PREPARE_FAILED, ConsumerErrorCode.SESSION_NOT_FOUND ->
                disconnectedOrGenerationFailure()
            ConsumerErrorCode.CAPABILITY_INCOMPATIBLE -> disconnectedOrCapabilityFailure()
            else -> AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE
        }

    private fun disconnectedOrCapabilityFailure(): AnalysisRuntimeFailureCode =
        if (transportConnected()) AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE else AnalysisRuntimeFailureCode.DISCONNECTED

    private fun disconnectedOrGenerationFailure(): AnalysisRuntimeFailureCode =
        if (transportConnected()) AnalysisRuntimeFailureCode.GENERATION_FAILED else AnalysisRuntimeFailureCode.DISCONNECTED

    private fun fits(
        chunk: AnalysisChunk,
        limits: AnalysisLimits,
    ): Boolean =
        composeInput(chunk).length <= limits.maxInputCharacters &&
            AnalysisProtocol.outputJsonSchema.length <= limits.maxJsonSchemaCharacters

    private fun composeInput(chunk: AnalysisChunk): String = AnalysisProtocol.instruction + DATA_SEPARATOR + chunk.dataPayload

    private fun requestId(
        operationId: AnalysisOperationId,
        ordinal: Int,
    ): RequestId = RequestId("redactguard-${operationId.value}-$ordinal")

    private data class PreparedOperation(
        val sessionId: SessionId,
        val limits: AnalysisLimits,
        val capabilityRevision: String,
        val preset: InferencePresetRef,
    )

    private class ConsumerOperation(
        val onPrepared: (Result<AnalysisLimits>) -> Unit,
    ) {
        var preparing = true
        var cancelled = false
        var sessionId: SessionId? = null
        var limits: AnalysisLimits? = null
        var capabilityRevision: String? = null
        var preset: InferencePresetRef? = null
        var activeGeneration: ActiveGeneration? = null
        var cancelAcknowledgement: (() -> Unit)? = null
    }

    private class ActiveGeneration(
        val requestId: RequestId,
        val onResult: (Result<String>) -> Unit,
    ) {
        val terminal = AtomicBoolean(false)
        var handle: ConsumerGenerationHandle? = null
    }

    private companion object {
        val DOCUMENT_PII_USE_CASE = UseCaseId("document-pii-detection")
        const val DATA_SEPARATOR = "\n\nDATA:\n"
    }
}

private fun runtimeFailure(code: AnalysisRuntimeFailureCode): AnalysisRuntimeException = AnalysisRuntimeException(code)

private fun takeCancellationAcknowledgement(operation: ConsumerAnalysisRuntime.ConsumerOperation): (() -> Unit)? =
    synchronized(operation) {
        operation.cancelAcknowledgement.also { operation.cancelAcknowledgement = null }
    }
