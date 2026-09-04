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
import org.junit.Test
import java.util.concurrent.Executor

class ConsumerAnalysisRuntimeDiagnosticStepTest {
    @Test
    fun `prepare rejection preserves prepare step and typed reason`() {
        val failure = prepareFailure(DiagnosticFailurePoint.PREPARE)

        assertEquals(AnalysisRuntimeFailureCode.GENERATION_FAILED, failure.code)
        assertEquals(
            AnalysisRuntimeDiagnostic("consumer.prepare", "Consumer:PREPARE_FAILED"),
            failure.diagnostic,
        )
    }

    @Test
    fun `logical submit rejection preserves submit step and typed reason`() {
        val client = DiagnosticConsumerClient(DiagnosticFailurePoint.LOGICAL_SUBMIT)
        val failure = generationFailure(client, "diagnostic-logical-submit")

        assertEquals(AnalysisRuntimeFailureCode.GENERATION_FAILED, failure.code)
        assertEquals(
            AnalysisRuntimeDiagnostic("consumer.logical-job.submit", "Consumer:SESSION_NOT_FOUND"),
            failure.diagnostic,
        )
    }

    @Test
    fun `logical result rejection preserves result step and typed reason`() {
        val client = DiagnosticConsumerClient(DiagnosticFailurePoint.LOGICAL_RESULT)
        val failure = generationFailure(client, "diagnostic-logical-result")

        assertEquals(AnalysisRuntimeFailureCode.GENERATION_FAILED, failure.code)
        assertEquals(
            AnalysisRuntimeDiagnostic("consumer.logical-job.result", "Consumer:RUNTIME_FAILURE"),
            failure.diagnostic,
        )
    }

    private fun generationFailure(
        client: DiagnosticConsumerClient,
        operationValue: String,
    ): AnalysisRuntimeException {
        val runtime = runtime(client)
        val operationId = AnalysisOperationId(operationValue)
        runtime.prepare(operationId) { it.getOrThrow() }
        var result: Result<String>? = null

        runtime.generate(operationId, chunk()) { result = it }

        return result!!.exceptionOrNull() as AnalysisRuntimeException
    }

    private fun prepareFailure(point: DiagnosticFailurePoint): AnalysisRuntimeException {
        val client = DiagnosticConsumerClient(point)
        val runtime = runtime(client)
        var result: Result<io.github.daniele21.redactguard.domain.analysis.AnalysisLimits>? = null

        runtime.prepare(AnalysisOperationId("diagnostic-${point.name.lowercase()}")) { result = it }

        return result!!.exceptionOrNull() as AnalysisRuntimeException
    }

    private fun runtime(client: DiagnosticConsumerClient): ConsumerAnalysisRuntime =
        ConsumerAnalysisRuntime(
            client = client,
            lifecycleExecutor = Executor(Runnable::run),
            transportConnected = { true },
            selectedPreset = { client.preset },
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
}

private enum class DiagnosticFailurePoint {
    PREPARE,
    LOGICAL_SUBMIT,
    LOGICAL_RESULT,
}

private class DiagnosticConsumerClient(
    private val failurePoint: DiagnosticFailurePoint,
) : ConsumerLocalLlmClient,
    ConsumerLogicalJobClient {
    private val useCaseId = UseCaseId("document-pii-detection")
    val preset = InferencePresetRef(InferencePresetId("qwen35-json"), 1)
    private val revision = "diagnostic-revision"
    private val jobId = ConsumerInferenceJobId("diagnostic-job")
    private var lastRequestId = ConsumerLogicalJobRequestId("diagnostic-request")
    private val execution =
        ConsumerExecutionIdentity(
            useCaseId = useCaseId,
            capabilityRevision = revision,
            preset = preset,
            reasoningMode = EffectiveConsumerReasoningMode.DISABLED,
            outputConstraint = ConsumerOutputConstraintKind.JSON_SCHEMA,
            sessionKind = SessionKind.STATELESS,
        )

    override fun capabilities(useCaseId: UseCaseId): ConsumerCapabilityResult =
        ConsumerCapabilityResult.Available(
            UseCaseCapabilities(
                useCaseId = this.useCaseId,
                readiness = UseCaseReadiness.READY,
                presets = listOf(ConsumerPresetOption(preset, true)),
                defaultPreset = preset,
                reasoning = ConsumerReasoningCapability.NOT_SUPPORTED,
                outputConstraints = setOf(ConsumerOutputConstraintKind.JSON_SCHEMA),
                defaultOutputConstraint = ConsumerOutputConstraintKind.JSON_SCHEMA,
                sessionKinds = setOf(SessionKind.STATELESS),
                defaultSessionKind = SessionKind.STATELESS,
                limits = ConsumerLimits(8_000, 1, 4_000),
                capabilityRevision = revision,
            ),
        )

    override fun prepare(request: ConsumerPrepareRequest): ConsumerPrepareResult =
        if (failurePoint == DiagnosticFailurePoint.PREPARE) {
            ConsumerPrepareResult.Rejected(ConsumerFailure(ConsumerErrorCode.PREPARE_FAILED, "raw prepare detail"))
        } else {
            ConsumerPrepareResult.Prepared(
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
        lastRequestId = request.clientRequestId
        return if (failurePoint == DiagnosticFailurePoint.LOGICAL_SUBMIT) {
            ConsumerInferenceJobResponse.Rejected(
                ConsumerFailure(ConsumerErrorCode.SESSION_NOT_FOUND, "raw logical-submit detail"),
            )
        } else {
            running()
        }
    }

    override fun logicalJob(
        jobId: ConsumerInferenceJobId,
        useCaseId: UseCaseId,
    ): ConsumerInferenceJobResponse = running()

    override fun logicalJobResult(
        jobId: ConsumerInferenceJobId,
        useCaseId: UseCaseId,
    ): ConsumerInferenceJobResponse =
        if (failurePoint == DiagnosticFailurePoint.LOGICAL_RESULT) {
            ConsumerInferenceJobResponse.Rejected(
                ConsumerFailure(ConsumerErrorCode.RUNTIME_FAILURE, "raw logical-result detail"),
            )
        } else {
            error("Logical result is not expected for $failurePoint")
        }

    override fun cancelLogicalJob(
        jobId: ConsumerInferenceJobId,
        useCaseId: UseCaseId,
    ) = Unit

    private fun running(): ConsumerInferenceJobResponse.Available =
        ConsumerInferenceJobResponse.Available(
            ConsumerInferenceJobSnapshot(
                jobId = jobId,
                clientRequestId = lastRequestId,
                useCaseId = useCaseId,
                execution = execution,
                state = ConsumerInferenceJobState.RUNNING,
                revision = 1,
                attempt = 1,
                runtimeSessionId = ConsumerRuntimeSessionId("diagnostic-runtime"),
                resultAvailable = false,
            ),
        )
}
