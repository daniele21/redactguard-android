package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.contracts.ConsumerCapabilityResult
import io.github.daniele21.localllm.contracts.ConsumerExecutionIdentity
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
import io.github.daniele21.localllm.contracts.ConsumerOutputConstraint
import io.github.daniele21.localllm.contracts.ConsumerOutputConstraintKind
import io.github.daniele21.localllm.contracts.ConsumerPrepareRequest
import io.github.daniele21.localllm.contracts.ConsumerPrepareResult
import io.github.daniele21.localllm.contracts.ConsumerPreparedId
import io.github.daniele21.localllm.contracts.ConsumerPreparedSelection
import io.github.daniele21.localllm.contracts.ConsumerPresetOption
import io.github.daniele21.localllm.contracts.ConsumerReasoningCapability
import io.github.daniele21.localllm.contracts.ConsumerReasoningPreference
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

class ConsumerAnalysisRuntimeTest {
    @Test
    fun `prepare maps strict host capabilities to app limits`() {
        val client = FakeConsumerClient()
        val runtime = ConsumerAnalysisRuntime(client, Executor(Runnable::run))
        var result: Result<io.github.daniele21.redactguard.domain.analysis.AnalysisLimits>? = null

        runtime.prepare(AnalysisOperationId("op-1")) { result = it }

        assertEquals(8_000, result!!.getOrThrow().maxInputCharacters)
        assertEquals(4_000, result!!.getOrThrow().maxJsonSchemaCharacters)
        assertEquals(1, client.capabilityCalls)
        assertEquals(1, client.prepareCalls)
        assertEquals(1, client.sessionCalls)
        assertEquals(client.defaultPreset, client.lastPrepareRequest?.selection?.preset)
        assertEquals(client.revision, client.lastPrepareRequest?.selection?.capabilityRevision)
        assertEquals(ConsumerReasoningPreference.DISABLED, client.lastPrepareRequest?.selection?.reasoning)
        assertEquals(ConsumerOutputConstraintKind.JSON_SCHEMA, client.lastPrepareRequest?.selection?.outputConstraint)
        assertEquals(SessionKind.STATELESS, client.lastPrepareRequest?.selection?.sessionKind)
    }

    @Test
    fun `multiple host published presets use host default when no local selection exists`() {
        val client =
            FakeConsumerClient(
                presets = listOf(PRESET_FAST, PRESET_QUALITY),
                defaultPreset = PRESET_FAST,
            )
        val runtime = ConsumerAnalysisRuntime(client, Executor(Runnable::run))
        var result: Result<io.github.daniele21.redactguard.domain.analysis.AnalysisLimits>? = null

        runtime.prepare(AnalysisOperationId("op-multi-default")) { result = it }

        result!!.getOrThrow()
        assertEquals(PRESET_FAST, client.lastPrepareRequest?.selection?.preset)
        assertEquals(1, client.sessionCalls)
    }

    @Test
    fun `explicit advertised preset is prepared without concrete model selection`() {
        val client =
            FakeConsumerClient(
                presets = listOf(PRESET_FAST, PRESET_QUALITY),
                defaultPreset = PRESET_FAST,
            )
        val runtime =
            ConsumerAnalysisRuntime(
                client = client,
                lifecycleExecutor = Executor(Runnable::run),
                selectedPreset = { PRESET_QUALITY },
            )
        val operationId = AnalysisOperationId("op-quality")
        var prepared: Result<io.github.daniele21.redactguard.domain.analysis.AnalysisLimits>? = null

        runtime.prepare(operationId) { prepared = it }
        prepared!!.getOrThrow()
        runtime.generate(operationId, chunk()) { it.getOrThrow() }

        assertEquals(PRESET_QUALITY, client.lastPrepareRequest?.selection?.preset)
        assertEquals(PRESET_QUALITY, client.lastExecutionPreset)
    }

    @Test
    fun `preset not advertised by host is rejected before prepare`() {
        val client =
            FakeConsumerClient(
                presets = listOf(PRESET_FAST, PRESET_QUALITY),
                defaultPreset = PRESET_FAST,
            )
        val runtime =
            ConsumerAnalysisRuntime(
                client = client,
                lifecycleExecutor = Executor(Runnable::run),
                selectedPreset = { PRESET_WITHDRAWN },
            )
        var result: Result<io.github.daniele21.redactguard.domain.analysis.AnalysisLimits>? = null

        runtime.prepare(AnalysisOperationId("op-stale-preset")) { result = it }

        val failure = result!!.exceptionOrNull() as AnalysisRuntimeException
        assertEquals(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE, failure.code)
        assertEquals(0, client.prepareCalls)
        assertEquals(0, client.sessionCalls)
    }

    @Test
    fun `duplicate advertised preset identity is rejected fail closed`() {
        val client =
            FakeConsumerClient(
                presets = listOf(PRESET_FAST, PRESET_FAST),
                defaultPreset = PRESET_FAST,
            )
        val runtime = ConsumerAnalysisRuntime(client, Executor(Runnable::run))
        var result: Result<io.github.daniele21.redactguard.domain.analysis.AnalysisLimits>? = null

        runtime.prepare(AnalysisOperationId("op-duplicate-preset")) { result = it }

        val failure = result!!.exceptionOrNull() as AnalysisRuntimeException
        assertEquals(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE, failure.code)
        assertEquals(0, client.prepareCalls)
    }

    @Test
    fun `reasoning capability change is rejected fail closed`() {
        val client = FakeConsumerClient(reasoning = ConsumerReasoningCapability.SURFACED_OPTIONAL)
        val runtime = ConsumerAnalysisRuntime(client, Executor(Runnable::run))
        var result: Result<io.github.daniele21.redactguard.domain.analysis.AnalysisLimits>? = null

        runtime.prepare(AnalysisOperationId("op-2")) { result = it }

        val failure = result!!.exceptionOrNull() as AnalysisRuntimeException
        assertEquals(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE, failure.code)
        assertEquals(0, client.prepareCalls)
    }

    @Test
    fun `generation sends structured PII definitions without duplicating them in document payload`() {
        val client = FakeConsumerClient()
        val runtime = ConsumerAnalysisRuntime(client, Executor(Runnable::run))
        val operationId = AnalysisOperationId("op-3")
        runtime.prepare(operationId) { it.getOrThrow() }
        var answer: Result<String>? = null

        runtime.generate(operationId, chunk()) { answer = it }

        val request = requireNotNull(client.lastGenerationRequest)
        assertTrue(request.outputConstraint is ConsumerOutputConstraint.JsonSchema)
        assertTrue((request.outputConstraint as ConsumerOutputConstraint.JsonSchema).schema.contains("findings"))
        val definition = request.taskDefinitions.single()
        assertEquals("email", definition.id)
        assertEquals("Personal email address", definition.description)
        assertEquals("alice@example.test", definition.example)
        val input = (request.input as ConsumerGenerationInput.Text).value
        assertTrue(input.contains("\"selectedTypeIds\":[\"email\"]"))
        assertFalse(input.contains("Personal email address"))
        assertFalse(input.contains("alice@example.test"))
        assertEquals("{\"schemaVersion\":1,\"findings\":[]}", answer!!.getOrThrow())
        runtime.close(operationId)
        assertEquals(listOf(SessionId("session-1")), client.closedSessions)
    }

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

    private companion object {
        val PRESET_FAST = InferencePresetRef(InferencePresetId("fast"), 1)
        val PRESET_QUALITY = InferencePresetRef(InferencePresetId("quality"), 2)
        val PRESET_WITHDRAWN = InferencePresetRef(InferencePresetId("withdrawn"), 1)
    }
}

private class FakeConsumerClient(
    private val reasoning: ConsumerReasoningCapability = ConsumerReasoningCapability.NOT_SUPPORTED,
    val presets: List<InferencePresetRef> = listOf(DEFAULT_PRESET),
    val defaultPreset: InferencePresetRef = presets.first(),
) : ConsumerLocalLlmClient {
    private val useCaseId = UseCaseId("document-pii-detection")
    val revision = "fixture-revision"
    var capabilityCalls = 0
    var prepareCalls = 0
    var sessionCalls = 0
    var lastPrepareRequest: ConsumerPrepareRequest? = null
    var lastGenerationRequest: ConsumerGenerationRequest? = null
    var lastExecutionPreset: InferencePresetRef? = null
    val closedSessions = mutableListOf<SessionId>()

    override fun capabilities(useCaseId: UseCaseId): ConsumerCapabilityResult {
        capabilityCalls += 1
        val defaultIndex = presets.indexOf(defaultPreset)
        return ConsumerCapabilityResult.Available(
            UseCaseCapabilities(
                useCaseId = this.useCaseId,
                readiness = UseCaseReadiness.READY,
                presets = presets.mapIndexed { index, ref -> ConsumerPresetOption(ref, index == defaultIndex) },
                defaultPreset = defaultPreset,
                reasoning = reasoning,
                outputConstraints = setOf(ConsumerOutputConstraintKind.JSON_SCHEMA),
                defaultOutputConstraint = ConsumerOutputConstraintKind.JSON_SCHEMA,
                sessionKinds = setOf(SessionKind.STATELESS),
                defaultSessionKind = SessionKind.STATELESS,
                limits = ConsumerLimits(8_000, 1, 4_000),
                capabilityRevision = revision,
            ),
        )
    }

    override fun prepare(request: ConsumerPrepareRequest): ConsumerPrepareResult {
        prepareCalls += 1
        lastPrepareRequest = request
        val requestedPreset = request.selection.preset ?: defaultPreset
        return ConsumerPrepareResult.Prepared(
            ConsumerPreparedSelection(
                preparedId = ConsumerPreparedId("prepared-1"),
                useCaseId = useCaseId,
                capabilityRevision = revision,
                preset = requestedPreset,
                reasoningMode = EffectiveConsumerReasoningMode.DISABLED,
                outputConstraint = ConsumerOutputConstraintKind.JSON_SCHEMA,
                sessionKind = SessionKind.STATELESS,
            ),
        )
    }

    override fun createSession(preparedId: ConsumerPreparedId): ConsumerSessionResult {
        sessionCalls += 1
        return ConsumerSessionResult.Created(SessionId("session-1"))
    }

    override fun generate(
        request: ConsumerGenerationRequest,
        listener: ConsumerGenerationListener,
    ): ConsumerGenerationStartResult {
        lastGenerationRequest = request
        val executionPreset = requireNotNull(lastPrepareRequest?.selection?.preset ?: defaultPreset)
        lastExecutionPreset = executionPreset
        val execution =
            ConsumerExecutionIdentity(
                useCaseId = useCaseId,
                capabilityRevision = revision,
                preset = executionPreset,
                reasoningMode = EffectiveConsumerReasoningMode.DISABLED,
                outputConstraint = ConsumerOutputConstraintKind.JSON_SCHEMA,
                sessionKind = SessionKind.STATELESS,
            )
        listener.onEvent(ConsumerGenerationEvent.Prepared(request.requestId, execution))
        listener.onEvent(
            ConsumerGenerationEvent.Completed(
                request.requestId,
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
                ),
            ),
        )
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

    private companion object {
        val DEFAULT_PRESET = InferencePresetRef(InferencePresetId("qwen35-json"), 1)
    }
}
