package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.contracts.ConsumerCapabilityResult
import io.github.daniele21.localllm.contracts.ConsumerErrorCode
import io.github.daniele21.localllm.contracts.ConsumerExecutionIdentity
import io.github.daniele21.localllm.contracts.ConsumerFailure
import io.github.daniele21.localllm.contracts.ConsumerGenerationHandle
import io.github.daniele21.localllm.contracts.ConsumerGenerationInput
import io.github.daniele21.localllm.contracts.ConsumerGenerationListener
import io.github.daniele21.localllm.contracts.ConsumerGenerationRequest
import io.github.daniele21.localllm.contracts.ConsumerGenerationStartResult
import io.github.daniele21.localllm.contracts.ConsumerInferenceJobId
import io.github.daniele21.localllm.contracts.ConsumerInferenceJobOutput
import io.github.daniele21.localllm.contracts.ConsumerInferenceJobResponse
import io.github.daniele21.localllm.contracts.ConsumerInferenceJobSnapshot
import io.github.daniele21.localllm.contracts.ConsumerInferenceJobState
import io.github.daniele21.localllm.contracts.ConsumerInferenceMetrics
import io.github.daniele21.localllm.contracts.ConsumerLimits
import io.github.daniele21.localllm.contracts.ConsumerLocalLlmClient
import io.github.daniele21.localllm.contracts.ConsumerLogicalJobClient
import io.github.daniele21.localllm.contracts.ConsumerLogicalJobRequestId
import io.github.daniele21.localllm.contracts.ConsumerLogicalJobSubmitRequest
import io.github.daniele21.localllm.contracts.ConsumerOutputConstraint
import io.github.daniele21.localllm.contracts.ConsumerOutputConstraintKind
import io.github.daniele21.localllm.contracts.ConsumerPrepareRequest
import io.github.daniele21.localllm.contracts.ConsumerPrepareResult
import io.github.daniele21.localllm.contracts.ConsumerPreparedId
import io.github.daniele21.localllm.contracts.ConsumerPreparedSelection
import io.github.daniele21.localllm.contracts.ConsumerPresetOption
import io.github.daniele21.localllm.contracts.ConsumerReasoningCapability
import io.github.daniele21.localllm.contracts.ConsumerReasoningPreference
import io.github.daniele21.localllm.contracts.ConsumerRuntimeSessionId
import io.github.daniele21.localllm.contracts.ConsumerSessionResult
import io.github.daniele21.localllm.contracts.ConsumerStopReason
import io.github.daniele21.localllm.contracts.EffectiveConsumerReasoningMode
import io.github.daniele21.localllm.contracts.InferencePresetId
import io.github.daniele21.localllm.contracts.InferencePresetRef
import io.github.daniele21.localllm.contracts.RequestId
import io.github.daniele21.localllm.contracts.SessionId
import io.github.daniele21.localllm.contracts.SessionKind
import io.github.daniele21.localllm.contracts.UseCaseCapabilities
import io.github.daniele21.localllm.contracts.UseCaseId
import io.github.daniele21.localllm.contracts.UseCaseReadiness
import io.github.daniele21.redactguard.domain.analysis.AnalysisChunk
import io.github.daniele21.redactguard.domain.analysis.AnalysisOperationId
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeException
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeFailureCode
import io.github.daniele21.redactguard.domain.analysis.AnalysisSegmentData
import io.github.daniele21.redactguard.domain.pii.PiiDefinition
import io.github.daniele21.redactguard.domain.pii.PiiDefinitionSource
import io.github.daniele21.redactguard.domain.pii.PiiSemanticCategory
import io.github.daniele21.redactguard.domain.pii.PiiTypeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

class ConsumerAnalysisRuntimeTest {
    @Test
    fun `prepare retains exact prepared selection without opening a legacy session`() {
        val client = FakeConsumerClient()
        val runtime = runtime(client)
        var result: Result<io.github.daniele21.redactguard.domain.analysis.AnalysisLimits>? = null

        runtime.prepare(AnalysisOperationId("op-prepare")) { result = it }

        assertEquals(8_000, result!!.getOrThrow().maxInputCharacters)
        assertEquals(1, client.prepareCalls)
        assertEquals(0, client.sessionCalls)
        assertEquals(client.defaultPreset, client.lastPrepareRequest?.selection?.preset)
        assertEquals(ConsumerReasoningPreference.DISABLED, client.lastPrepareRequest?.selection?.reasoning)
    }

    @Test
    fun `generation uses stable logical job identity and exact prepared execution`() {
        val client = FakeConsumerClient()
        val runtime = runtime(client)
        val operationId = AnalysisOperationId("op-logical")
        runtime.prepare(operationId) { it.getOrThrow() }
        var answer: Result<String>? = null

        runtime.generate(operationId, chunk()) { answer = it }

        val request = client.submitRequests.single()
        assertEquals(ConsumerLogicalJobRequestId("redactguard:op-logical:0"), request.clientRequestId)
        assertEquals(ConsumerPreparedId("prepared-1"), request.preparedId)
        assertEquals(client.execution, request.expectedExecution)
        assertTrue(request.outputConstraint is ConsumerOutputConstraint.JsonSchema)
        val input = (request.input as ConsumerGenerationInput.Text).value
        assertTrue(input.contains("\"selectedTypeIds\":[\"email\"]"))
        assertFalse(input.contains("Personal email address"))
        assertEquals("Personal email address", request.taskDefinitions.single().description)
        assertEquals("{\"schemaVersion\":1,\"findings\":[]}", answer!!.getOrThrow())
        assertEquals(0, client.legacyGenerateCalls)
        assertEquals(0, client.closedSessions.size)
    }

    @Test
    fun `uncertain submit retries with the same idempotency key`() {
        val client = FakeConsumerClient()
        var submitCount = 0
        client.submitHandler = { request ->
            submitCount += 1
            if (submitCount == 1) rejectedRuntime() else client.succeeded(request.clientRequestId, revision = 1)
        }
        val runtime = runtime(client)
        val operationId = AnalysisOperationId("op-ack-loss")
        runtime.prepare(operationId) { it.getOrThrow() }
        var answer: Result<String>? = null

        runtime.generate(operationId, chunk()) { answer = it }

        assertEquals(2, client.submitRequests.size)
        assertEquals(client.submitRequests[0].clientRequestId, client.submitRequests[1].clientRequestId)
        assertEquals("{\"schemaVersion\":1,\"findings\":[]}", answer!!.getOrThrow())
    }

    @Test
    fun `temporary binder loss reconciles the same accepted job after reconnect`() {
        val connected = AtomicBoolean(true)
        val client = FakeConsumerClient()
        var resultCall = 0
        client.resultHandler = { jobId ->
            resultCall += 1
            if (resultCall == 1) {
                connected.set(false)
                rejectedRuntime()
            } else {
                connected.set(true)
                client.succeeded(client.lastClientRequestId, jobId = jobId, revision = 2)
            }
        }
        val runtime = runtime(client, connected::get)
        val operationId = AnalysisOperationId("op-reconnect")
        runtime.prepare(operationId) { it.getOrThrow() }
        var answer: Result<String>? = null

        runtime.generate(operationId, chunk()) { answer = it }

        assertEquals(client.jobId, client.resultJobIds.distinct().single())
        assertEquals("{\"schemaVersion\":1,\"findings\":[]}", answer!!.getOrThrow())
    }

    @Test
    fun `lost accepted job after reconnect maps to host process lost`() {
        val connected = AtomicBoolean(true)
        val client = FakeConsumerClient()
        var resultCall = 0
        client.resultHandler = {
            resultCall += 1
            connected.set(resultCall != 1)
            rejectedRuntime()
        }
        val runtime = runtime(client, connected::get)
        val operationId = AnalysisOperationId("op-host-loss")
        runtime.prepare(operationId) { it.getOrThrow() }
        var answer: Result<String>? = null

        runtime.generate(operationId, chunk()) { answer = it }

        val failure = answer!!.exceptionOrNull() as AnalysisRuntimeException
        assertEquals(AnalysisRuntimeFailureCode.HOST_PROCESS_LOST, failure.code)
        assertEquals("HostProcessLost", failure.diagnostic?.type)
    }

    @Test
    fun `stale logical job revision fails closed`() {
        val client = FakeConsumerClient()
        client.submitHandler = { request -> client.running(request.clientRequestId, revision = 4) }
        client.resultHandler = { jobId -> client.succeeded(client.lastClientRequestId, jobId = jobId, revision = 3) }
        val runtime = runtime(client)
        val operationId = AnalysisOperationId("op-stale-revision")
        runtime.prepare(operationId) { it.getOrThrow() }
        var answer: Result<String>? = null

        runtime.generate(operationId, chunk()) { answer = it }

        val failure = answer!!.exceptionOrNull() as AnalysisRuntimeException
        assertEquals(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE, failure.code)
    }

    private fun runtime(
        client: FakeConsumerClient,
        transportConnected: () -> Boolean = { true },
    ): ConsumerAnalysisRuntime =
        ConsumerAnalysisRuntime(
            client = client,
            lifecycleExecutor = Executor(Runnable::run),
            transportConnected = transportConnected,
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
}

private class FakeConsumerClient :
    ConsumerLocalLlmClient,
    ConsumerLogicalJobClient {
    private val useCaseId = UseCaseId("document-pii-detection")
    val defaultPreset = InferencePresetRef(InferencePresetId("qwen35-json"), 1)
    val jobId = ConsumerInferenceJobId("job-1")
    private val revision = "fixture-revision"
    val execution =
        ConsumerExecutionIdentity(
            useCaseId = useCaseId,
            capabilityRevision = revision,
            preset = defaultPreset,
            reasoningMode = EffectiveConsumerReasoningMode.DISABLED,
            outputConstraint = ConsumerOutputConstraintKind.JSON_SCHEMA,
            sessionKind = SessionKind.STATELESS,
        )
    var prepareCalls = 0
    var sessionCalls = 0
    var legacyGenerateCalls = 0
    var lastPrepareRequest: ConsumerPrepareRequest? = null
    val closedSessions = mutableListOf<SessionId>()
    val submitRequests = mutableListOf<ConsumerLogicalJobSubmitRequest>()
    val resultJobIds = mutableListOf<ConsumerInferenceJobId>()
    var lastClientRequestId = ConsumerLogicalJobRequestId("fixture")
    var submitHandler: (ConsumerLogicalJobSubmitRequest) -> ConsumerInferenceJobResponse = { request ->
        running(request.clientRequestId, revision = 1)
    }
    var resultHandler: (ConsumerInferenceJobId) -> ConsumerInferenceJobResponse = { currentJobId ->
        succeeded(lastClientRequestId, currentJobId, revision = 2)
    }

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
        lastPrepareRequest = request
        return ConsumerPrepareResult.Prepared(
            ConsumerPreparedSelection(
                preparedId = ConsumerPreparedId("prepared-1"),
                useCaseId = useCaseId,
                capabilityRevision = revision,
                preset = defaultPreset,
                reasoningMode = EffectiveConsumerReasoningMode.DISABLED,
                outputConstraint = ConsumerOutputConstraintKind.JSON_SCHEMA,
                sessionKind = SessionKind.STATELESS,
            ),
        )
    }

    override fun createSession(preparedId: ConsumerPreparedId): ConsumerSessionResult {
        sessionCalls += 1
        return ConsumerSessionResult.Created(SessionId("legacy-session"))
    }

    override fun generate(
        request: ConsumerGenerationRequest,
        listener: ConsumerGenerationListener,
    ): ConsumerGenerationStartResult {
        legacyGenerateCalls += 1
        return ConsumerGenerationStartResult.Accepted(
            object : ConsumerGenerationHandle {
                override val requestId: RequestId = request.requestId

                override fun cancel() = Unit
            },
        )
    }

    override fun closeSession(sessionId: SessionId) {
        closedSessions += sessionId
    }

    override fun submitLogicalGeneration(request: ConsumerLogicalJobSubmitRequest): ConsumerInferenceJobResponse {
        submitRequests += request
        lastClientRequestId = request.clientRequestId
        return submitHandler(request)
    }

    override fun logicalJob(
        jobId: ConsumerInferenceJobId,
        useCaseId: UseCaseId,
    ): ConsumerInferenceJobResponse = resultHandler(jobId)

    override fun logicalJobResult(
        jobId: ConsumerInferenceJobId,
        useCaseId: UseCaseId,
    ): ConsumerInferenceJobResponse {
        resultJobIds += jobId
        return resultHandler(jobId)
    }

    override fun cancelLogicalJob(
        jobId: ConsumerInferenceJobId,
        useCaseId: UseCaseId,
    ) = Unit

    fun running(
        clientRequestId: ConsumerLogicalJobRequestId,
        revision: Long,
        jobId: ConsumerInferenceJobId = this.jobId,
    ): ConsumerInferenceJobResponse.Available =
        ConsumerInferenceJobResponse.Available(
            snapshot(clientRequestId, jobId, ConsumerInferenceJobState.RUNNING, revision),
        )

    fun succeeded(
        clientRequestId: ConsumerLogicalJobRequestId,
        jobId: ConsumerInferenceJobId = this.jobId,
        revision: Long,
    ): ConsumerInferenceJobResponse.Available =
        ConsumerInferenceJobResponse.Available(
            snapshot(
                clientRequestId,
                jobId,
                ConsumerInferenceJobState.SUCCEEDED,
                revision,
                resultAvailable = true,
            ),
            ConsumerInferenceJobOutput(
                answer = "{\"schemaVersion\":1,\"findings\":[]}",
                surfacedReasoning = null,
                metrics =
                    ConsumerInferenceMetrics(
                        outputTokens = 1,
                        timeToFirstTokenMs = 1,
                        totalMs = 2,
                        decodeTokensPerSecond = 1.0,
                        inputTokens = 1,
                        reasoningTokens = 0,
                        answerTokens = 1,
                        queueMs = 0,
                        stopReason = ConsumerStopReason.GRAMMAR_COMPLETE,
                    ),
            ),
        )

    private fun snapshot(
        clientRequestId: ConsumerLogicalJobRequestId,
        jobId: ConsumerInferenceJobId,
        state: ConsumerInferenceJobState,
        revision: Long,
        resultAvailable: Boolean = false,
    ) = ConsumerInferenceJobSnapshot(
        jobId = jobId,
        clientRequestId = clientRequestId,
        useCaseId = useCaseId,
        execution = execution,
        state = state,
        revision = revision,
        attempt = 1,
        runtimeSessionId = ConsumerRuntimeSessionId("runtime-1"),
        resultAvailable = resultAvailable,
    )
}

private fun rejectedRuntime(): ConsumerInferenceJobResponse =
    ConsumerInferenceJobResponse.Rejected(
        ConsumerFailure(ConsumerErrorCode.RUNTIME_FAILURE, "Synthetic transport failure"),
    )
