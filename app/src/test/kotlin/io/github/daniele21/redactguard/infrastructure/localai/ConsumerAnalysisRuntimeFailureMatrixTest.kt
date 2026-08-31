package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.contracts.ConsumerCapabilityResult
import io.github.daniele21.localllm.contracts.ConsumerErrorCode
import io.github.daniele21.localllm.contracts.ConsumerExecutionIdentity
import io.github.daniele21.localllm.contracts.ConsumerFailure
import io.github.daniele21.localllm.contracts.ConsumerGenerationEvent
import io.github.daniele21.localllm.contracts.ConsumerGenerationHandle
import io.github.daniele21.localllm.contracts.ConsumerGenerationInput
import io.github.daniele21.localllm.contracts.ConsumerGenerationListener
import io.github.daniele21.localllm.contracts.ConsumerGenerationRequest
import io.github.daniele21.localllm.contracts.ConsumerGenerationStartResult
import io.github.daniele21.localllm.contracts.ConsumerInferenceMetrics
import io.github.daniele21.localllm.contracts.ConsumerInferenceResult
import io.github.daniele21.localllm.contracts.ConsumerLimits
import io.github.daniele21.localllm.contracts.ConsumerLocalLlmClient
import io.github.daniele21.localllm.contracts.ConsumerOutputConstraintKind
import io.github.daniele21.localllm.contracts.ConsumerPrepareRequest
import io.github.daniele21.localllm.contracts.ConsumerPrepareResult
import io.github.daniele21.localllm.contracts.ConsumerPreparedId
import io.github.daniele21.localllm.contracts.ConsumerPreparedSelection
import io.github.daniele21.localllm.contracts.ConsumerPresetOption
import io.github.daniele21.localllm.contracts.ConsumerReasoningCapability
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Executor

class ConsumerAnalysisRuntimeFailureMatrixTest {
    @Test
    fun `every consumer error code has a stable generation-event mapping when connected and disconnected`() {
        listOf(true, false).forEach { connected ->
            ConsumerErrorCode.entries.forEach { consumerCode ->
                val client = FailureMatrixConsumerClient(generationEventFailure = consumerCode)
                val runtime = runtime(client, connected)
                val operationId = AnalysisOperationId("event-${consumerCode.name.lowercase()}-$connected")
                runtime.prepare(operationId) { it.getOrThrow() }
                var result: Result<String>? = null

                runtime.generate(operationId, chunk()) { result = it }

                val failure = result!!.exceptionOrNull() as AnalysisRuntimeException
                assertEquals(
                    "Unexpected event mapping for $consumerCode connected=$connected",
                    expectedRuntimeFailure(consumerCode, connected),
                    failure.code,
                )
                runtime.close(operationId)
                assertEquals(listOf(SessionId("session-1")), client.closedSessions)
            }
        }
    }

    @Test
    fun `every consumer error code has the same stable mapping when generation is rejected before start`() {
        listOf(true, false).forEach { connected ->
            ConsumerErrorCode.entries.forEach { consumerCode ->
                val client = FailureMatrixConsumerClient(generationStartFailure = consumerCode)
                val runtime = runtime(client, connected)
                val operationId = AnalysisOperationId("start-${consumerCode.name.lowercase()}-$connected")
                runtime.prepare(operationId) { it.getOrThrow() }
                var result: Result<String>? = null

                runtime.generate(operationId, chunk()) { result = it }

                val failure = result!!.exceptionOrNull() as AnalysisRuntimeException
                assertEquals(
                    "Unexpected start mapping for $consumerCode connected=$connected",
                    expectedRuntimeFailure(consumerCode, connected),
                    failure.code,
                )
                runtime.close(operationId)
                assertEquals(listOf(SessionId("session-1")), client.closedSessions)
            }
        }
    }

    @Test
    fun `prepare failed and session not found use generation family only while transport is connected`() {
        listOf(true, false).forEach { connected ->
            val prepareClient = FailureMatrixConsumerClient(prepareFailure = ConsumerErrorCode.PREPARE_FAILED)
            val prepareRuntime = runtime(prepareClient, connected)
            var prepareResult: Result<io.github.daniele21.redactguard.domain.analysis.AnalysisLimits>? = null

            prepareRuntime.prepare(AnalysisOperationId("prepare-$connected")) { prepareResult = it }

            val prepareFailure = prepareResult!!.exceptionOrNull() as AnalysisRuntimeException
            assertEquals(
                if (connected) AnalysisRuntimeFailureCode.GENERATION_FAILED else AnalysisRuntimeFailureCode.DISCONNECTED,
                prepareFailure.code,
            )
            assertEquals(1, prepareClient.prepareCalls)
            assertEquals(0, prepareClient.sessionCalls)

            val sessionClient = FailureMatrixConsumerClient(sessionFailure = ConsumerErrorCode.SESSION_NOT_FOUND)
            val sessionRuntime = runtime(sessionClient, connected)
            var sessionResult: Result<io.github.daniele21.redactguard.domain.analysis.AnalysisLimits>? = null

            sessionRuntime.prepare(AnalysisOperationId("session-$connected")) { sessionResult = it }

            val sessionFailure = sessionResult!!.exceptionOrNull() as AnalysisRuntimeException
            assertEquals(
                if (connected) AnalysisRuntimeFailureCode.GENERATION_FAILED else AnalysisRuntimeFailureCode.DISCONNECTED,
                sessionFailure.code,
            )
            assertEquals(1, sessionClient.prepareCalls)
            assertEquals(1, sessionClient.sessionCalls)
        }
    }

    @Test
    fun `typed Harness failure detail never crosses the privacy-safe runtime exception`() {
        val secretDetail = "Synthetic raw detail containing alice@example.test and document text"
        val client =
            FailureMatrixConsumerClient(
                generationEventFailure = ConsumerErrorCode.RUNTIME_FAILURE,
                failureMessage = secretDetail,
            )
        val runtime = runtime(client, connected = true)
        val operationId = AnalysisOperationId("privacy-safe-consumer-failure")
        runtime.prepare(operationId) { it.getOrThrow() }
        var result: Result<String>? = null

        runtime.generate(operationId, chunk()) { result = it }

        val failure = result!!.exceptionOrNull() as AnalysisRuntimeException
        assertEquals(AnalysisRuntimeFailureCode.GENERATION_FAILED, failure.code)
        assertNull(failure.diagnostic)
        assertFalse(failure.message.orEmpty().contains(secretDetail))
        assertFalse(failure.toString().contains("alice@example.test"))
        runtime.close(operationId)
    }

    @Test
    fun `unchecked sdk exception becomes local AI internal with safe generate step and type`() {
        val client =
            FailureMatrixConsumerClient(
                generateThrowable = IllegalStateException("sensitive fixture content alice@example.test"),
            )
        val runtime = runtime(client, connected = true)
        val operationId = AnalysisOperationId("unchecked-generate")
        runtime.prepare(operationId) { it.getOrThrow() }
        var result: Result<String>? = null

        runtime.generate(operationId, chunk()) { result = it }

        val failure = result!!.exceptionOrNull() as AnalysisRuntimeException
        assertEquals(AnalysisRuntimeFailureCode.INTERNAL_FAILURE, failure.code)
        assertEquals(
            AnalysisRuntimeDiagnostic(step = "consumer.generate", type = "IllegalStateException"),
            failure.diagnostic,
        )
        assertFalse(failure.message.orEmpty().contains("alice@example.test"))
        runtime.close(operationId)
        assertEquals(listOf(SessionId("session-1")), client.closedSessions)
    }

    @Test
    fun `first terminal generation failure wins exactly once and late completion is ignored`() {
        val client =
            FailureMatrixConsumerClient(
                generationEventFailure = ConsumerErrorCode.RUNTIME_FAILURE,
                emitCompletionAfterFailure = true,
            )
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
        assertTrue(client.lastHandle?.cancelled == true)
        runtime.close(operationId)
        assertEquals(listOf(SessionId("session-1")), client.closedSessions)
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
                AnalysisRuntimeFailureCode.HOST_UNAVAILABLE
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
    private val sessionFailure: ConsumerErrorCode? = null,
    private val generationStartFailure: ConsumerErrorCode? = null,
    private val generationEventFailure: ConsumerErrorCode? = null,
    private val failureMessage: String = "Synthetic consumer failure",
    private val generateThrowable: RuntimeException? = null,
    private val emitCompletionAfterFailure: Boolean = false,
) : ConsumerLocalLlmClient {
    private val useCaseId = UseCaseId("document-pii-detection")
    val defaultPreset = InferencePresetRef(InferencePresetId("qwen35-json"), 1)
    private val revision = "failure-matrix-revision"
    var prepareCalls = 0
    var sessionCalls = 0
    val closedSessions = mutableListOf<SessionId>()
    var lastHandle: RecordingGenerationHandle? = null

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

    override fun createSession(preparedId: ConsumerPreparedId): ConsumerSessionResult {
        sessionCalls += 1
        sessionFailure?.let { return ConsumerSessionResult.Rejected(failure(it)) }
        return ConsumerSessionResult.Created(SessionId("session-1"))
    }

    override fun generate(
        request: ConsumerGenerationRequest,
        listener: ConsumerGenerationListener,
    ): ConsumerGenerationStartResult {
        generateThrowable?.let { throw it }
        generationStartFailure?.let { return ConsumerGenerationStartResult.Rejected(failure(it)) }

        val execution = execution()
        listener.onEvent(ConsumerGenerationEvent.Prepared(request.requestId, execution))
        generationEventFailure?.let { code ->
            listener.onEvent(ConsumerGenerationEvent.Failed(request.requestId, failure(code)))
            if (emitCompletionAfterFailure) {
                listener.onEvent(ConsumerGenerationEvent.Completed(request.requestId, successfulResult(execution)))
            }
        } ?: listener.onEvent(ConsumerGenerationEvent.Completed(request.requestId, successfulResult(execution)))

        return ConsumerGenerationStartResult.Accepted(
            RecordingGenerationHandle(request.requestId).also { lastHandle = it },
        )
    }

    override fun closeSession(sessionId: SessionId) {
        closedSessions += sessionId
    }

    private fun failure(code: ConsumerErrorCode): ConsumerFailure = ConsumerFailure(code, failureMessage)

    private fun execution(): ConsumerExecutionIdentity =
        ConsumerExecutionIdentity(
            useCaseId = useCaseId,
            capabilityRevision = revision,
            preset = defaultPreset,
            reasoningMode = EffectiveConsumerReasoningMode.DISABLED,
            outputConstraint = ConsumerOutputConstraintKind.JSON_SCHEMA,
            sessionKind = SessionKind.STATELESS,
        )

    private fun successfulResult(execution: ConsumerExecutionIdentity): ConsumerInferenceResult =
        ConsumerInferenceResult(
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
            execution = execution,
        )
}

private class RecordingGenerationHandle(
    override val requestId: RequestId,
) : ConsumerGenerationHandle {
    var cancelled = false

    override fun cancel() {
        cancelled = true
    }
}
