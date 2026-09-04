package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.contracts.ConsumerCapabilityErrorCode
import io.github.daniele21.localllm.contracts.ConsumerCapabilityResult
import io.github.daniele21.localllm.contracts.ConsumerErrorCode
import io.github.daniele21.localllm.contracts.ConsumerExecutionIdentity
import io.github.daniele21.localllm.contracts.ConsumerFailure
import io.github.daniele21.localllm.contracts.ConsumerGenerationInput
import io.github.daniele21.localllm.contracts.ConsumerInferenceJobId
import io.github.daniele21.localllm.contracts.ConsumerInferenceJobResponse
import io.github.daniele21.localllm.contracts.ConsumerInferenceJobState
import io.github.daniele21.localllm.contracts.ConsumerLocalLlmClient
import io.github.daniele21.localllm.contracts.ConsumerLogicalJobClient
import io.github.daniele21.localllm.contracts.ConsumerLogicalJobRequestId
import io.github.daniele21.localllm.contracts.ConsumerLogicalJobSubmitRequest
import io.github.daniele21.localllm.contracts.ConsumerOutputConstraint
import io.github.daniele21.localllm.contracts.ConsumerOutputConstraintKind
import io.github.daniele21.localllm.contracts.ConsumerPrepareRequest
import io.github.daniele21.localllm.contracts.ConsumerPrepareResult
import io.github.daniele21.localllm.contracts.ConsumerPreparedSelection
import io.github.daniele21.localllm.contracts.ConsumerReasoningCapability
import io.github.daniele21.localllm.contracts.ConsumerReasoningPreference
import io.github.daniele21.localllm.contracts.ConsumerSelectionRequest
import io.github.daniele21.localllm.contracts.EffectiveConsumerReasoningMode
import io.github.daniele21.localllm.contracts.InferencePresetRef
import io.github.daniele21.localllm.contracts.SessionKind
import io.github.daniele21.localllm.contracts.TaskDefinition
import io.github.daniele21.localllm.contracts.UseCaseCapabilities
import io.github.daniele21.localllm.contracts.UseCaseId
import io.github.daniele21.localllm.contracts.UseCaseReadiness
import io.github.daniele21.localllm.contracts.toExecutionIdentity
import io.github.daniele21.redactguard.domain.analysis.AnalysisChunk
import io.github.daniele21.redactguard.domain.analysis.AnalysisLimits
import io.github.daniele21.redactguard.domain.analysis.AnalysisOperationId
import io.github.daniele21.redactguard.domain.analysis.AnalysisProtocol
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeDiagnostic
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeException
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeFailureCode
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimePort
import io.github.daniele21.redactguard.domain.pii.PiiDefinition
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/** Strict Consumer API adapter. Model/configuration choice remains host-owned. */
internal class ConsumerAnalysisRuntime(
    private val client: ConsumerLocalLlmClient,
    private val lifecycleExecutor: Executor,
    private val transportConnected: () -> Boolean = { true },
    private val useCaseId: UseCaseId = DOCUMENT_PII_USE_CASE,
    private val selectedPreset: () -> InferencePresetRef? = { null },
    private val logicalJobs: ConsumerLogicalJobClient =
        requireNotNull(client as? ConsumerLogicalJobClient) {
            "Consumer client must support durable logical jobs"
        },
    private val pollDelayMillis: Long = DEFAULT_POLL_DELAY_MILLIS,
    private val sleeper: (Long) -> Unit = Thread::sleep,
) : AnalysisRuntimePort {
    private val operations = ConcurrentHashMap<AnalysisOperationId, ConsumerOperation>()
    private val transportInvalidationEpoch = AtomicLong(0L)

    init {
        require(pollDelayMillis >= 0L) { "pollDelayMillis must not be negative" }
    }

    internal fun onTransportConnectionInvalidated() {
        transportInvalidationEpoch.incrementAndGet()
    }

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
                val prepared = operation.preparedSelection ?: return@synchronized null
                if (operation.cancelled || operation.activeGeneration != null || !fits(chunk, limits)) {
                    return@synchronized null
                }
                ActiveGeneration(
                    clientRequestId = logicalRequestId(operationId, chunk.ordinal),
                    expectedExecution = prepared.toExecutionIdentity(),
                    onResult = onResult,
                ).also { operation.activeGeneration = it }
            }
        if (generation == null) {
            onResult(Result.failure(runtimeFailure(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)))
            return
        }

        try {
            lifecycleExecutor.execute { executeLogicalGeneration(operation, generation, chunk) }
        } catch (_: RejectedExecutionException) {
            finishGeneration(
                operation,
                generation,
                Result.failure(runtimeFailure(AnalysisRuntimeFailureCode.DISCONNECTED)),
            )
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
        val jobId = active?.jobId
        if (jobId != null) {
            runCatching { logicalJobs.cancelLogicalJob(jobId, useCaseId) }
        } else if (active == null && !operation.preparing) {
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
        active?.jobId?.let { jobId ->
            runCatching { logicalJobs.cancelLogicalJob(jobId, useCaseId) }
        }
    }

    private fun prepareOnExecutor(
        operationId: AnalysisOperationId,
        operation: ConsumerOperation,
    ) {
        val prepared = runCatching { localAiBoundary(STEP_PREPARE_PIPELINE, ::prepareConsumer) }
        val value = prepared.getOrNull()
        val cancelled =
            synchronized(operation) {
                operation.preparing = false
                if (value != null) {
                    operation.preparedSelection = value.selection
                    operation.limits = value.limits
                }
                operation.cancelled
            }
        if (cancelled) {
            operations.remove(operationId, operation)
            takeCancellationAcknowledgement(operation)?.invoke()
            return
        }
        if (value == null) {
            operations.remove(operationId, operation)
            operation.onPrepared(
                Result.failure(prepared.exceptionOrNull() ?: runtimeFailure(disconnectedOrGenerationFailure())),
            )
        } else {
            operation.onPrepared(Result.success(value.limits))
        }
    }

    private fun prepareConsumer(): PreparedOperation {
        val capabilities =
            when (val result = localAiBoundary(STEP_CAPABILITIES) { client.capabilities(useCaseId) }) {
                is ConsumerCapabilityResult.Available -> result.capabilities
                is ConsumerCapabilityResult.Rejected -> throw runtimeFailure(mapCapabilityFailure(result.code))
            }
        validateCapabilities(capabilities)
        val requestedPreset = resolveRequestedPreset(capabilities)
        val selection =
            when (
                val result =
                    localAiBoundary(STEP_PREPARE) {
                        client.prepare(
                            ConsumerPrepareRequest(
                                useCaseId = useCaseId,
                                selection =
                                    ConsumerSelectionRequest(
                                        capabilityRevision = capabilities.capabilityRevision,
                                        preset = requestedPreset,
                                        reasoning = ConsumerReasoningPreference.DISABLED,
                                        outputConstraint = ConsumerOutputConstraintKind.JSON_SCHEMA,
                                        sessionKind = SessionKind.STATELESS,
                                    ),
                            ),
                        )
                    }
            ) {
                is ConsumerPrepareResult.Prepared -> {
                    result.selection
                }

                is ConsumerPrepareResult.Rejected -> {
                    throw result.failure.toAnalysisRuntimeException(STEP_PREPARE, transportConnected)
                }
            }
        validatePreparedSelection(selection, capabilities, requestedPreset)
        return PreparedOperation(
            selection = selection,
            limits = AnalysisLimits(capabilities.limits.maxInputCharacters, capabilities.limits.maxJsonSchemaCharacters),
        )
    }

    private fun executeLogicalGeneration(
        operation: ConsumerOperation,
        generation: ActiveGeneration,
        chunk: AnalysisChunk,
    ) {
        val prepared = synchronized(operation) { operation.preparedSelection }
        if (prepared == null) {
            finishGeneration(
                operation,
                generation,
                Result.failure(runtimeFailure(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)),
            )
            return
        }
        val request =
            ConsumerLogicalJobSubmitRequest(
                clientRequestId = generation.clientRequestId,
                useCaseId = useCaseId,
                preparedId = prepared.preparedId,
                expectedExecution = generation.expectedExecution,
                input = ConsumerGenerationInput.Text(composeInput(chunk)),
                outputConstraint = ConsumerOutputConstraint.JsonSchema(AnalysisProtocol.outputJsonSchema),
                taskDefinitions = chunk.definitions.map(::toTaskDefinition),
            )
        val result =
            try {
                runLogicalJob(operation, generation, request)
            } catch (failure: AnalysisRuntimeException) {
                Result.failure(failure)
            } catch (failure: RuntimeException) {
                Result.failure(unexpectedLocalAiFailure(STEP_LOGICAL_RESULT, failure))
            }
        finishGeneration(operation, generation, result)
    }

    private fun runLogicalJob(
        operation: ConsumerOperation,
        generation: ActiveGeneration,
        request: ConsumerLogicalJobSubmitRequest,
    ): Result<String> {
        var transportWasLost = false
        var observedTransportInvalidationEpoch = transportInvalidationEpoch.get()
        var uncertainConnectedSubmitRetries = 0
        while (true) {
            val jobId = generation.jobId
            if (operation.cancelled && jobId == null && !generation.submitAttempted) {
                return Result.failure(runtimeFailure(AnalysisRuntimeFailureCode.CANCELLED))
            }
            if (operation.cancelled && jobId != null && transportConnected()) {
                runCatching { logicalJobs.cancelLogicalJob(jobId, useCaseId) }
            }

            val response =
                try {
                    if (jobId == null) {
                        generation.submitAttempted = true
                        logicalJobs.submitLogicalGeneration(request)
                    } else {
                        logicalJobs.logicalJobResult(jobId, useCaseId)
                    }
                } catch (failure: RuntimeException) {
                    val latestTransportInvalidationEpoch = transportInvalidationEpoch.get()
                    val transportInvalidated = latestTransportInvalidationEpoch != observedTransportInvalidationEpoch
                    if (transportInvalidated || !transportConnected()) {
                        transportWasLost = true
                        observedTransportInvalidationEpoch = latestTransportInvalidationEpoch
                        waitBeforeRetry()
                        continue
                    }
                    return Result.failure(unexpectedLocalAiFailure(logicalStep(jobId), failure))
                }

            val latestTransportInvalidationEpoch = transportInvalidationEpoch.get()
            val transportInvalidated = latestTransportInvalidationEpoch != observedTransportInvalidationEpoch
            transportWasLost = transportWasLost || transportInvalidated
            observedTransportInvalidationEpoch = latestTransportInvalidationEpoch

            when (response) {
                is ConsumerInferenceJobResponse.Rejected -> {
                    val transportFailure = response.failure.code == ConsumerErrorCode.RUNTIME_FAILURE
                    if (transportFailure && (transportInvalidated || !transportConnected())) {
                        transportWasLost = true
                        waitBeforeRetry()
                        continue
                    }
                    if (transportWasLost) {
                        return Result.failure(hostProcessLost(logicalStep(jobId)))
                    }
                    if (
                        jobId == null &&
                        transportFailure &&
                        uncertainConnectedSubmitRetries < MAX_CONNECTED_SUBMIT_RETRIES
                    ) {
                        uncertainConnectedSubmitRetries += 1
                        waitBeforeRetry()
                        continue
                    }
                    return Result.failure(
                        response.failure.toAnalysisRuntimeException(logicalStep(jobId), transportConnected),
                    )
                }

                is ConsumerInferenceJobResponse.Available -> {
                    val snapshot = response.snapshot
                    val validationFailure =
                        validateLogicalSnapshot(
                            generation = generation,
                            jobId = snapshot.jobId,
                            clientRequestId = snapshot.clientRequestId,
                            execution = snapshot.execution,
                            snapshotUseCaseId = snapshot.useCaseId,
                            revision = snapshot.revision,
                        )
                    if (validationFailure != null) return Result.failure(validationFailure)
                    if (generation.jobId == null) generation.jobId = snapshot.jobId

                    when (snapshot.state) {
                        ConsumerInferenceJobState.SUCCEEDED -> {
                            val output = response.output
                            if (output == null) {
                                waitBeforeRetry()
                                continue
                            }
                            if (!output.surfacedReasoning.isNullOrEmpty()) {
                                return Result.failure(runtimeFailure(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE))
                            }
                            return Result.success(output.answer)
                        }

                        ConsumerInferenceJobState.CANCELLED -> {
                            return Result.failure(runtimeFailure(AnalysisRuntimeFailureCode.CANCELLED))
                        }

                        ConsumerInferenceJobState.FAILED_FINAL -> {
                            return Result.failure(snapshot.errorCode.toLogicalJobFailure())
                        }

                        ConsumerInferenceJobState.INTERRUPTED -> {
                            return Result.failure(hostProcessLost(STEP_LOGICAL_RESULT))
                        }

                        ConsumerInferenceJobState.QUEUED,
                        ConsumerInferenceJobState.PREPARING,
                        ConsumerInferenceJobState.RUNNING,
                        ConsumerInferenceJobState.CANCEL_REQUESTED,
                        ConsumerInferenceJobState.FAILED_RETRYABLE,
                        ConsumerInferenceJobState.RECOVERING,
                        -> {
                            waitBeforeRetry()
                        }
                    }
                }
            }
        }
    }

    private fun validateLogicalSnapshot(
        generation: ActiveGeneration,
        jobId: ConsumerInferenceJobId,
        clientRequestId: ConsumerLogicalJobRequestId,
        execution: ConsumerExecutionIdentity,
        snapshotUseCaseId: UseCaseId,
        revision: Long,
    ): AnalysisRuntimeException? {
        val expectedJobId = generation.jobId
        val compatible =
            clientRequestId == generation.clientRequestId &&
                snapshotUseCaseId == useCaseId &&
                execution == generation.expectedExecution &&
                (expectedJobId == null || expectedJobId == jobId) &&
                revision >= generation.lastRevision
        if (!compatible) return runtimeFailure(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)
        generation.lastRevision = revision
        return null
    }

    private fun ConsumerErrorCode?.toLogicalJobFailure(): AnalysisRuntimeException {
        val code = this ?: ConsumerErrorCode.RUNTIME_FAILURE
        return ConsumerFailure(code, "Logical job failed")
            .toAnalysisRuntimeException(STEP_LOGICAL_RESULT, transportConnected)
    }

    private fun waitBeforeRetry() {
        if (pollDelayMillis == 0L) return
        try {
            sleeper(pollDelayMillis)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            throw runtimeFailure(AnalysisRuntimeFailureCode.DISCONNECTED)
        }
    }

    private fun validateCapabilities(capabilities: UseCaseCapabilities) {
        when (capabilities.readiness) {
            UseCaseReadiness.READY,
            UseCaseReadiness.AVAILABLE_REQUIRES_PREPARATION,
            -> Unit

            UseCaseReadiness.UNAVAILABLE_MODEL -> throw runtimeFailure(AnalysisRuntimeFailureCode.HOST_UNAVAILABLE)

            UseCaseReadiness.UNAVAILABLE_HOST_POLICY,
            UseCaseReadiness.INCOMPATIBLE,
            -> throw runtimeFailure(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)
        }
        val defaultPreset = capabilities.defaultPreset
        val defaultMetadataCompatible =
            if (defaultPreset == null) {
                capabilities.presets.none { it.isDefault }
            } else {
                capabilities.presets.count { it.isDefault } == 1 &&
                    capabilities.presets.any { it.isDefault && it.ref == defaultPreset }
            }
        val compatible =
            capabilities.useCaseId == useCaseId &&
                capabilities.presets.isNotEmpty() &&
                capabilities.presets
                    .map { it.ref }
                    .distinct()
                    .size == capabilities.presets.size &&
                defaultMetadataCompatible &&
                capabilities.outputConstraints == setOf(ConsumerOutputConstraintKind.JSON_SCHEMA) &&
                capabilities.defaultOutputConstraint == ConsumerOutputConstraintKind.JSON_SCHEMA &&
                capabilities.sessionKinds == setOf(SessionKind.STATELESS) &&
                capabilities.defaultSessionKind == SessionKind.STATELESS &&
                capabilities.reasoning == ConsumerReasoningCapability.NOT_SUPPORTED
        if (!compatible) throw runtimeFailure(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)
    }

    private fun resolveRequestedPreset(capabilities: UseCaseCapabilities): InferencePresetRef {
        val requested = selectedPreset() ?: throw runtimeFailure(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)
        if (capabilities.presets.none { it.ref == requested }) {
            throw runtimeFailure(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)
        }
        return requested
    }

    private fun validatePreparedSelection(
        selection: ConsumerPreparedSelection,
        capabilities: UseCaseCapabilities,
        requestedPreset: InferencePresetRef,
    ) {
        val compatible =
            selection.useCaseId == useCaseId &&
                selection.capabilityRevision == capabilities.capabilityRevision &&
                selection.preset == requestedPreset &&
                selection.reasoningMode == EffectiveConsumerReasoningMode.DISABLED &&
                selection.outputConstraint == ConsumerOutputConstraintKind.JSON_SCHEMA &&
                selection.sessionKind == SessionKind.STATELESS
        if (!compatible) throw runtimeFailure(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)
    }

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
        if (cancelled) {
            takeCancellationAcknowledgement(operation)?.invoke()
        } else {
            generation.onResult(result)
        }
    }

    private fun closeCancelledOperation(
        operationId: AnalysisOperationId,
        operation: ConsumerOperation,
    ) {
        operations.remove(operationId, operation)
        takeCancellationAcknowledgement(operation)?.invoke()
    }

    private fun takeCancellationAcknowledgement(operation: ConsumerOperation): (() -> Unit)? =
        synchronized(operation) {
            operation.cancelAcknowledgement.also { operation.cancelAcknowledgement = null }
        }

    private fun mapCapabilityFailure(code: ConsumerCapabilityErrorCode): AnalysisRuntimeFailureCode =
        when (code) {
            ConsumerCapabilityErrorCode.MODEL_UNAVAILABLE -> AnalysisRuntimeFailureCode.HOST_UNAVAILABLE
            ConsumerCapabilityErrorCode.CAPABILITY_INCOMPATIBLE -> disconnectedOrCapabilityFailure()
            else -> AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE
        }

    private fun disconnectedOrCapabilityFailure(): AnalysisRuntimeFailureCode =
        if (transportConnected()) {
            AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE
        } else {
            AnalysisRuntimeFailureCode.DISCONNECTED
        }

    private fun disconnectedOrGenerationFailure(): AnalysisRuntimeFailureCode =
        if (transportConnected()) AnalysisRuntimeFailureCode.GENERATION_FAILED else AnalysisRuntimeFailureCode.DISCONNECTED

    private fun fits(
        chunk: AnalysisChunk,
        limits: AnalysisLimits,
    ): Boolean =
        composeInput(chunk).length <= limits.maxInputCharacters &&
            AnalysisProtocol.outputJsonSchema.length <= limits.maxJsonSchemaCharacters

    private fun composeInput(chunk: AnalysisChunk): String = AnalysisProtocol.instruction + DATA_SEPARATOR + chunk.dataPayload

    private fun logicalRequestId(
        operationId: AnalysisOperationId,
        ordinal: Int,
    ): ConsumerLogicalJobRequestId = ConsumerLogicalJobRequestId("redactguard:${operationId.value}:$ordinal")

    private fun logicalStep(jobId: ConsumerInferenceJobId?): String = if (jobId == null) STEP_LOGICAL_SUBMIT else STEP_LOGICAL_RESULT

    private fun toTaskDefinition(definition: PiiDefinition): TaskDefinition =
        TaskDefinition(
            id = definition.id.value,
            description = definition.definition,
            example = definition.example,
        )

    private data class PreparedOperation(
        val selection: ConsumerPreparedSelection,
        val limits: AnalysisLimits,
    )

    private class ConsumerOperation(
        val onPrepared: (Result<AnalysisLimits>) -> Unit,
    ) {
        var preparing = true

        @Volatile
        var cancelled = false
        var preparedSelection: ConsumerPreparedSelection? = null
        var limits: AnalysisLimits? = null
        var activeGeneration: ActiveGeneration? = null
        var cancelAcknowledgement: (() -> Unit)? = null
    }

    private class ActiveGeneration(
        val clientRequestId: ConsumerLogicalJobRequestId,
        val expectedExecution: ConsumerExecutionIdentity,
        val onResult: (Result<String>) -> Unit,
    ) {
        val terminal = AtomicBoolean(false)

        @Volatile
        var jobId: ConsumerInferenceJobId? = null

        @Volatile
        var submitAttempted = false
        var lastRevision = -1L
    }

    private companion object {
        val DOCUMENT_PII_USE_CASE = UseCaseId("document-pii-detection")
        const val DATA_SEPARATOR = "\n\nDATA:\n"
        const val STEP_PREPARE_PIPELINE = "consumer.prepare-pipeline"
        const val STEP_CAPABILITIES = "consumer.capabilities"
        const val STEP_PREPARE = "consumer.prepare"
        const val STEP_LOGICAL_SUBMIT = "consumer.logical-job.submit"
        const val STEP_LOGICAL_RESULT = "consumer.logical-job.result"
        const val DEFAULT_POLL_DELAY_MILLIS = 100L
        const val MAX_CONNECTED_SUBMIT_RETRIES = 2
    }
}

private fun runtimeFailure(code: AnalysisRuntimeFailureCode): AnalysisRuntimeException = AnalysisRuntimeException(code)

private fun hostProcessLost(step: String): AnalysisRuntimeException =
    AnalysisRuntimeException(
        code = AnalysisRuntimeFailureCode.HOST_PROCESS_LOST,
        diagnostic = AnalysisRuntimeDiagnostic(step = step, type = "HostProcessLost"),
    )
