package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.contracts.ConsumerCapabilityResult
import io.github.daniele21.localllm.contracts.ConsumerErrorCode
import io.github.daniele21.localllm.contracts.ConsumerExecutionIdentity
import io.github.daniele21.localllm.contracts.ConsumerFailure
import io.github.daniele21.localllm.contracts.ConsumerGenerationListener
import io.github.daniele21.localllm.contracts.ConsumerGenerationRequest
import io.github.daniele21.localllm.contracts.ConsumerGenerationStartResult
import io.github.daniele21.localllm.contracts.ConsumerInferenceJobId
import io.github.daniele21.localllm.contracts.ConsumerInferenceJobResponse
import io.github.daniele21.localllm.contracts.ConsumerInferenceJobSnapshot
import io.github.daniele21.localllm.contracts.ConsumerInferenceJobState
import io.github.daniele21.localllm.contracts.ConsumerLimits
import io.github.daniele21.localllm.contracts.ConsumerLocalLlmClient
import io.github.daniele21.localllm.contracts.ConsumerLogicalJobClient
import io.github.daniele21.localllm.contracts.ConsumerLogicalJobRequestId
import io.github.daniele21.localllm.contracts.ConsumerLogicalJobSubmitRequest
import io.github.daniele21.localllm.contracts.ConsumerOutputConstraintKind
import io.github.daniele21.localllm.contracts.ConsumerPrepareRequest
import io.github.daniele21.localllm.contracts.ConsumerPrepareResult
import io.github.daniele21.localllm.contracts.ConsumerPreparedId
import io.github.daniele21.localllm.contracts.ConsumerPreparedSelection
import io.github.daniele21.localllm.contracts.ConsumerPresetOption
import io.github.daniele21.localllm.contracts.ConsumerReasoningCapability
import io.github.daniele21.localllm.contracts.ConsumerRuntimeSessionId
import io.github.daniele21.localllm.contracts.ConsumerSessionResult
import io.github.daniele21.localllm.contracts.EffectiveConsumerReasoningMode
import io.github.daniele21.localllm.contracts.InferencePresetId
import io.github.daniele21.localllm.contracts.InferencePresetRef
import io.github.daniele21.localllm.contracts.SessionId
import io.github.daniele21.localllm.contracts.SessionKind
import io.github.daniele21.localllm.contracts.UseCaseCapabilities
import io.github.daniele21.localllm.contracts.UseCaseId
import io.github.daniele21.localllm.contracts.UseCaseReadiness
import io.github.daniele21.redactguard.domain.analysis.AnalysisChunk
import io.github.daniele21.redactguard.domain.analysis.AnalysisOperationId
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeDiagnostic
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeException
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeFailureCode
import io.github.daniele21.redactguard.domain.analysis.AnalysisSegmentData
import io.github.daniele21.redactguard.domain.pii.PiiDefinition
import io.github.daniele21.redactguard.domain.pii.PiiDefinitionSource
import io.github.daniele21.redactguard.domain.pii.PiiSemanticCategory
import io.github.daniele21.redactguard.domain.pii.PiiTypeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.concurrent.Executor

class ConsumerAnalysisRuntimeFailureMatrixTest {
    @Test
    fun `every consumer error code keeps the stable mapping when connected and disconnected`() {
        listOf(true, false).forEach { connected ->
            ConsumerErrorCode.entries.forEach { consumerCode ->
                val failure = ConsumerFailure(consumerCode, "Synthetic consumer failure")

                assertEquals(
                    "Unexpected mapping for $consumerCode connected=$connected",
                    expectedRuntimeFailure(consumerCode, connected),
                    failure.toAnalysisFailureCode { connected },
                )
            }
        }
    }

    @Test
    fun `every consumer error code has the stable logical submit mapping when connected`() {
        ConsumerErrorCode.entries.forEach { consumerCode ->
            val client = FailureMatrixConsumerClient(logicalSubmitFailure = consumerCode)
            val failure = generationFailure(client, "submit-${consumerCode.name.lowercase()}")

            assertEquals(expectedRuntimeFailure(consumerCode, connected = true), failure.code)
            assertEquals(
                AnalysisRuntimeDiagnostic(
                    step = "consumer.logical-job.submit",
                    type = "Consumer:${consumerCode.name}",
                ),
                failure.diagnostic,
            )
        }
    }

    @Test
    fun `every consumer error code has the stable logical result mapping when connected`() {
        ConsumerErrorCode.entries.forEach { consumerCode ->
            val client = FailureMatrixConsumerClient(logicalResultFailure = consumerCode)
            val failure = generationFailure(client, "result-${consumerCode.name.lowercase()}")

            assertEquals(expectedRuntimeFailure(consumerCode, connected = true), failure.code)
            assertEquals(
                AnalysisRuntimeDiagnostic(
                    step = "consumer.logical-job.result",
                    type = "Consumer:${consumerCode.name}",
                ),
                failure.diagnostic,
            )
        }
    }

    @Test
    fun `prepare failed uses generation family only while transport is connected`() {
        listOf(true, false).forEach { connected ->
            val client = FailureMatrixConsumerClient(prepareFailure = ConsumerErrorCode.PREPARE_FAILED)
            val runtime = runtime(client, connected)
            var prepareResult: Result<io.github.daniele21.redactguard.domain.analysis.AnalysisLimits>? = null

            runtime.prepare(AnalysisOperationId("prepare-$connected")) { prepareResult = it }

            val failure = prepareResult!!.exceptionOrNull() as AnalysisRuntimeException
            assertEquals(
                if (connected) AnalysisRuntimeFailureCode.GENERATION_FAILED else AnalysisRuntimeFailureCode.DISCONNECTED,
                failure.code,
            )
            assertEquals(1, client.prepareCalls)
        }
    }

    @Test
    fun `typed Harness free-form logical failure detail never crosses privacy-safe runtime exception`() {
        val secretDetail = "Synthetic raw detail containing alice@example.test and document text"
        val client =
            FailureMatrixConsumerClient(
                logicalResultFailure = ConsumerErrorCode.RUNTIME_FAILURE,
                failureMessage = secretDetail,
            )
        val failure = generationFailure(client, "privacy-safe-consumer-failure")

        assertEquals(AnalysisRuntimeFailureCode.GENERATION_FAILED, failure.code)
        assertFalse(
            failure.diagnostic
                ?.step
                .orEmpty()
                .contains(secretDetail),
        )
        assertFalse(
            failure.diagnostic
                ?.type
                .orEmpty()
                .contains(secretDetail),
        )
        assertFalse(failure.message.orEmpty().contains(secretDetail))
        assertFalse(failure.toString().contains("alice@example.test"))
    }

    @Test
    fun `unchecked logical submit exception becomes local AI internal with safe step and type`() {
        val client =
            FailureMatrixConsumerClient(
                logicalSubmitThrowable = IllegalStateException("sensitive fixture content alice@example.test"),
            )
        val failure = generationFailure(client, "unchecked-logical-submit")

        assertEquals(AnalysisRuntimeFailureCode.INTERNAL_FAILURE, failure.code)
        assertEquals(
            AnalysisRuntimeDiagnostic(step = "consumer.logical-job.submit", type = "IllegalStateException"),
            failure.diagnostic,
        )
        assertFalse(failure.message.orEmpty().contains("alice@example.test"))
    }

    @Test
    fun `failed final logical job invokes terminal callback exactly once`() {
        val client = FailureMatrixConsumerClient(failedFinalCode = ConsumerErrorCode.RUNTIME_FAILURE)
        val runtime = runtime(client, connected = true)
        val operationId = AnalysisOperationId("terminal-once")
        runtime.prepare(operationId) { it.getOrThrow() }
        var callbacks = 0
        var result: Result<String>? = null

        runtime.generate(operationId, chunk()) {
            callbacks += 1
            result = it
        }

        assertEquals(1, callbacks)
        val failure = result!!.exceptionOrNull() as AnalysisRuntimeException
        assertEquals(AnalysisRuntimeFailureCode.GENERATION_FAILED, failure.code)
    }

    private fun generationFailure(
        client: FailureMatrixConsumerClient,
        operationValue: String,
    ): AnalysisRuntimeException {
        val runtime = runtime(client, connected = true)
        val operationId = AnalysisOperationId(operationValue)
        runtime.prepare(operationId) { it.getOrThrow() }
        var result: Result<String>? = null

        runtime.generate(operationId, chunk()) { result = it }

        return result!!.exceptionOrNull() as AnalysisRuntimeException
    }

    private fun runtime(
        client: FailureMatrixConsumerClient,
        connected: Boolean,
    ): ConsumerAnalysisRuntime =
        ConsumerAnalysisRuntime(
            client = client,
            lifecycleExecutor = Executor(Runnable::run),
            transportConnected = { connected },
            selectedPreset = { client.defaultPreset },
            pollDelayMillis = 0,
        )

    private fun chunk(): AnalysisChunk {
        val definition =
            PiiDefinition(
                id = PiiTypeId.parse("email"),
                label = "Email",
                definition = "Personal email address",
                example = "alice@example.test",
                source = PiiDefinitionSource.BUILT_IN,
                semanticCategory = PiiSemanticCategory.CONTACT,
            )
        return AnalysisChunk(
            ordinal = 0,
            segments = listOf(AnalysisSegmentData("p0001-b0001", "synthetic text")),
            dataPayload =
                "{\"definitionSetVersion\":2,\"selectedTypeIds\":[\"email\"]," +
                    "\"segments\":[{\"segmentId\":\"p0001-b0001\",\"text\":\"synthetic text\"}]}",
            definitions = listOf(definition),
        )
    }

    private fun expectedRuntimeFailure(
        consumerCode: ConsumerErrorCode,
        connected: Boolean,
    ): AnalysisRuntimeFailureCode =
        when (consumerCode) {
            ConsumerErrorCode.MODEL_UNAVAILABLE -> {
                AnalysisRuntimeFailureCode.MODEL_UNAVAILABLE
            }

            ConsumerErrorCode.CANCELLED -> {
                AnalysisRuntimeFailureCode.CANCELLED
            }

            ConsumerErrorCode.RUNTIME_FAILURE,
            ConsumerErrorCode.PREPARE_FAILED,
            ConsumerErrorCode.SESSION_NOT_FOUND,
            -> {
                if (connected) AnalysisRuntimeFailureCode.GENERATION_FAILED else AnalysisRuntimeFailureCode.DISCONNECTED
            }

            ConsumerErrorCode.CAPABILITY_INCOMPATIBLE -> {
                if (connected) AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE else AnalysisRuntimeFailureCode.DISCONNECTED
            }

            else -> {
                AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE
            }
        }
}

private class FailureMatrixConsumerClient(
    private val prepareFailure: ConsumerErrorCode? = null,
    private val logicalSubmitFailure: ConsumerErrorCode? = null,
    private val logicalResultFailure: ConsumerErrorCode? = null,
    private val failedFinalCode: ConsumerErrorCode? = null,
    private val failureMessage: String = "Synthetic consumer failure",
    private val logicalSubmitThrowable: RuntimeException? = null,
) : ConsumerLocalLlmClient,
    ConsumerLogicalJobClient {
    private val useCaseId = UseCaseId("document-pii-detection")
    val defaultPreset = InferencePresetRef(InferencePresetId("qwen35-json"), 1)
    private val revision = "failure-matrix-revision"
    private val jobId = ConsumerInferenceJobId("failure-matrix-job")
    private var lastClientRequestId = ConsumerLogicalJobRequestId("failure-matrix-request")
    private val execution =
        ConsumerExecutionIdentity(
            useCaseId = useCaseId,
            capabilityRevision = revision,
            preset = defaultPreset,
            reasoningMode = EffectiveConsumerReasoningMode.DISABLED,
            outputConstraint = ConsumerOutputConstraintKind.JSON_SCHEMA,
            sessionKind = SessionKind.STATELESS,
        )
    var prepareCalls = 0

    override fun capabilities(useCaseId: UseCaseId): ConsumerCapabilityResult =
        ConsumerCapabilityResult.Available(
            UseCaseCapabilities(
                useCaseId = this.useCaseId,
                readiness = UseCaseReadiness.READY,
                presets = listOf(ConsumerPresetOption(defaultPreset, true)),
                defaultPreset = defaultPreset,
                reasoning = ConsumerReasoningCapability.NOT_SUPPORTED,
                outputConstraints = setOf(ConsumerOutputConstraintKind.JSON_SCHEMA),
                defaultOutputConstraint = ConsumerOutputConstraintKind.JSON_SCHEMA,
                sessionKinds = setOf(SessionKind.STATELESS),
                defaultSessionKind = SessionKind.STATELESS,
                limits = ConsumerLimits(8_000, 1, 4_000),
                capabilityRevision = revision,
            ),
        )

    override fun prepare(request: ConsumerPrepareRequest): ConsumerPrepareResult {
        prepareCalls += 1
        prepareFailure?.let { return ConsumerPrepareResult.Rejected(failure(it)) }
        return ConsumerPrepareResult.Prepared(
            ConsumerPreparedSelection(
                preparedId = ConsumerPreparedId("prepared-1"),
                useCaseId = useCaseId,
                capabilityRevision = revision,
                preset = request.selection.preset,
                reasoningMode = EffectiveConsumerReasoningMode.DISABLED,
                outputConstraint = ConsumerOutputConstraintKind.JSON_SCHEMA,
                sessionKind = SessionKind.STATELESS,
            ),
        )
    }

    override fun createSession(preparedId: ConsumerPreparedId): ConsumerSessionResult = error("Legacy session must not be used")

    override fun generate(
        request: ConsumerGenerationRequest,
        listener: ConsumerGenerationListener,
    ): ConsumerGenerationStartResult = error("Legacy generation must not be used")

    override fun closeSession(sessionId: SessionId) = Unit

    override fun submitLogicalGeneration(request: ConsumerLogicalJobSubmitRequest): ConsumerInferenceJobResponse {
        logicalSubmitThrowable?.let { throw it }
        lastClientRequestId = request.clientRequestId
        logicalSubmitFailure?.let { return ConsumerInferenceJobResponse.Rejected(failure(it)) }
        return available(ConsumerInferenceJobState.RUNNING, revision = 1)
    }

    override fun logicalJob(
        jobId: ConsumerInferenceJobId,
        useCaseId: UseCaseId,
    ): ConsumerInferenceJobResponse = available(ConsumerInferenceJobState.RUNNING, revision = 1)

    override fun logicalJobResult(
        jobId: ConsumerInferenceJobId,
        useCaseId: UseCaseId,
    ): ConsumerInferenceJobResponse {
        logicalResultFailure?.let { return ConsumerInferenceJobResponse.Rejected(failure(it)) }
        failedFinalCode?.let { return available(ConsumerInferenceJobState.FAILED_FINAL, revision = 2, errorCode = it) }
        error("Logical result was not configured")
    }

    override fun cancelLogicalJob(
        jobId: ConsumerInferenceJobId,
        useCaseId: UseCaseId,
    ) = Unit

    private fun available(
        state: ConsumerInferenceJobState,
        revision: Long,
        errorCode: ConsumerErrorCode? = null,
    ): ConsumerInferenceJobResponse.Available =
        ConsumerInferenceJobResponse.Available(
            ConsumerInferenceJobSnapshot(
                jobId = jobId,
                clientRequestId = lastClientRequestId,
                useCaseId = useCaseId,
                execution = execution,
                state = state,
                revision = revision,
                attempt = 1,
                runtimeSessionId = ConsumerRuntimeSessionId("failure-matrix-runtime"),
                resultAvailable = false,
                errorCode = errorCode,
            ),
        )

    private fun failure(code: ConsumerErrorCode): ConsumerFailure = ConsumerFailure(code, failureMessage)
}
