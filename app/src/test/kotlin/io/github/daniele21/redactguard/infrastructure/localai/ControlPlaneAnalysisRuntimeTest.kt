package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.contracts.ConsumerActivation
import io.github.daniele21.localllm.contracts.ConsumerActivationId
import io.github.daniele21.localllm.contracts.ConsumerActivationRequest
import io.github.daniele21.localllm.contracts.ConsumerActivationResult
import io.github.daniele21.localllm.contracts.ConsumerAssignedUseCase
import io.github.daniele21.localllm.contracts.ConsumerAssignedUseCasesResult
import io.github.daniele21.localllm.contracts.ConsumerControlPlaneClient
import io.github.daniele21.localllm.contracts.ConsumerControlPlaneErrorCode
import io.github.daniele21.localllm.contracts.ConsumerControlPlaneFailure
import io.github.daniele21.localllm.contracts.ConsumerDeactivationResult
import io.github.daniele21.localllm.contracts.ConsumerGenerationConfiguration
import io.github.daniele21.localllm.contracts.ConsumerPublishedPreset
import io.github.daniele21.localllm.contracts.ConsumerPublishedPresetsResult
import io.github.daniele21.localllm.contracts.ConsumerResolvedSetup
import io.github.daniele21.localllm.contracts.ConsumerSetupResolutionRequest
import io.github.daniele21.localllm.contracts.ConsumerSetupResolutionResult
import io.github.daniele21.localllm.contracts.InferencePresetId
import io.github.daniele21.localllm.contracts.InferencePresetRef
import io.github.daniele21.localllm.contracts.SeedPolicyType
import io.github.daniele21.localllm.contracts.ThinkingMode
import io.github.daniele21.localllm.contracts.UseCaseId
import io.github.daniele21.redactguard.domain.analysis.AnalysisChunk
import io.github.daniele21.redactguard.domain.analysis.AnalysisLimits
import io.github.daniele21.redactguard.domain.analysis.AnalysisOperationId
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeException
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeFailureCode
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimePort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Executor

class ControlPlaneAnalysisRuntimeTest {
    @Test
    fun `prepare resolves fresh setup and observes assigned use case before delegate and close releases both`() {
        val events = mutableListOf<String>()
        val controlPlaneClient = FakeControlPlaneClient(events = events)
        val delegate = FakeAnalysisRuntime(events)
        val readiness = FakeReadinessObserver(events)
        val runtime =
            ControlPlaneAnalysisRuntime(
                delegate = delegate,
                controlPlane = ConsumerControlPlaneCoordinator(controlPlaneClient),
                readinessObserver = readiness,
                lifecycleExecutor = Executor(Runnable::run),
            )
        val operationId = AnalysisOperationId("op-control-plane")
        var prepared: Result<AnalysisLimits>? = null

        runtime.prepare(operationId) { prepared = it }

        assertEquals(8_000, prepared!!.getOrThrow().maxInputCharacters)
        assertEquals(
            listOf("assigned", "presets", "resolve-setup", "activate", "readiness", "delegate-prepare"),
            events,
        )
        assertEquals(DEFAULT_PRESET, controlPlaneClient.lastActivationRequest?.preset)

        runtime.close(operationId)

        assertEquals(1, delegate.closeCalls)
        assertEquals(1, readiness.closeCalls)
        assertEquals(listOf(ACTIVATION_ID), controlPlaneClient.deactivated)
        assertEquals("deactivate", events.last())
    }

    @Test
    fun `explicit advertised preset is resolved and activated before delegate preparation`() {
        val controlPlaneClient =
            FakeControlPlaneClient(
                presets =
                    listOf(
                        ConsumerPublishedPreset(DEFAULT_PRESET, "Balanced", "Default", true),
                        ConsumerPublishedPreset(QUALITY_PRESET, "Quality", "Higher quality", false),
                    ),
            )
        val delegate = FakeAnalysisRuntime()
        val runtime =
            ControlPlaneAnalysisRuntime(
                delegate = delegate,
                controlPlane = ConsumerControlPlaneCoordinator(controlPlaneClient),
                readinessObserver = FakeReadinessObserver(),
                lifecycleExecutor = Executor(Runnable::run),
                selectedPreset = { QUALITY_PRESET },
            )
        val operationId = AnalysisOperationId("op-quality")

        runtime.prepare(operationId) { it.getOrThrow() }

        assertEquals(QUALITY_PRESET, controlPlaneClient.lastResolvedSetupRequest?.preset)
        assertEquals(QUALITY_PRESET, controlPlaneClient.lastActivationRequest?.preset)
        runtime.close(operationId)
    }

    @Test
    fun `fresh setup identity mismatch fails before activation readiness and consumer prepare`() {
        val events = mutableListOf<String>()
        val controlPlaneClient = FakeControlPlaneClient(events = events, resolvedUseCaseRevision = 2)
        val delegate = FakeAnalysisRuntime()
        val readiness = FakeReadinessObserver()
        val runtime =
            ControlPlaneAnalysisRuntime(
                delegate = delegate,
                controlPlane = ConsumerControlPlaneCoordinator(controlPlaneClient),
                readinessObserver = readiness,
                lifecycleExecutor = Executor(Runnable::run),
            )
        var prepared: Result<AnalysisLimits>? = null

        runtime.prepare(AnalysisOperationId("op-stale-setup")) { prepared = it }

        val failure = prepared!!.exceptionOrNull() as AnalysisRuntimeException
        assertEquals(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE, failure.code)
        assertEquals(listOf("assigned", "presets", "resolve-setup"), events)
        assertEquals(null, controlPlaneClient.lastActivationRequest)
        assertFalse(delegate.prepared)
        assertEquals(0, readiness.observeCalls)
    }

    @Test
    fun `activation rejection fails before readiness and consumer prepare`() {
        val controlPlaneClient =
            FakeControlPlaneClient(
                activationFailure =
                    ConsumerControlPlaneFailure(
                        ConsumerControlPlaneErrorCode.USE_CASE_NOT_ASSIGNED,
                        "Synthetic missing assignment",
                    ),
            )
        val delegate = FakeAnalysisRuntime()
        val readiness = FakeReadinessObserver()
        val runtime =
            ControlPlaneAnalysisRuntime(
                delegate = delegate,
                controlPlane = ConsumerControlPlaneCoordinator(controlPlaneClient),
                readinessObserver = readiness,
                lifecycleExecutor = Executor(Runnable::run),
            )
        var prepared: Result<AnalysisLimits>? = null

        runtime.prepare(AnalysisOperationId("op-rejected")) { prepared = it }

        val failure = prepared!!.exceptionOrNull() as AnalysisRuntimeException
        assertEquals(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE, failure.code)
        assertFalse(delegate.prepared)
        assertEquals(0, readiness.observeCalls)
        assertTrue(controlPlaneClient.deactivated.isEmpty())
    }

    @Test
    fun `readiness rejection releases activation before consumer prepare`() {
        val controlPlaneClient = FakeControlPlaneClient()
        val delegate = FakeAnalysisRuntime()
        val readiness =
            FakeReadinessObserver(
                failure = AnalysisRuntimeException(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE),
            )
        val runtime =
            ControlPlaneAnalysisRuntime(
                delegate = delegate,
                controlPlane = ConsumerControlPlaneCoordinator(controlPlaneClient),
                readinessObserver = readiness,
                lifecycleExecutor = Executor(Runnable::run),
            )
        var prepared: Result<AnalysisLimits>? = null

        runtime.prepare(AnalysisOperationId("op-readiness-rejected")) { prepared = it }

        val failure = prepared!!.exceptionOrNull() as AnalysisRuntimeException
        assertEquals(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE, failure.code)
        assertFalse(delegate.prepared)
        assertEquals(1, readiness.observeCalls)
        assertEquals(listOf(ACTIVATION_ID), controlPlaneClient.deactivated)
    }

    @Test
    fun `cancel closes delegate observation and activation`() {
        val controlPlaneClient = FakeControlPlaneClient()
        val delegate = FakeAnalysisRuntime()
        val readiness = FakeReadinessObserver()
        val runtime =
            ControlPlaneAnalysisRuntime(
                delegate = delegate,
                controlPlane = ConsumerControlPlaneCoordinator(controlPlaneClient),
                readinessObserver = readiness,
                lifecycleExecutor = Executor(Runnable::run),
            )
        val operationId = AnalysisOperationId("op-cancel")
        var cancelled = false
        runtime.prepare(operationId) { it.getOrThrow() }

        runtime.cancel(operationId) { cancelled = true }

        assertTrue(cancelled)
        assertEquals(1, delegate.cancelCalls)
        assertEquals(1, delegate.closeCalls)
        assertEquals(1, readiness.closeCalls)
        assertEquals(listOf(ACTIVATION_ID), controlPlaneClient.deactivated)
    }

    @Test
    fun `withdrawn requested preset fails closed before setup resolution and activation`() {
        val controlPlaneClient = FakeControlPlaneClient()
        val delegate = FakeAnalysisRuntime()
        val readiness = FakeReadinessObserver()
        val runtime =
            ControlPlaneAnalysisRuntime(
                delegate = delegate,
                controlPlane = ConsumerControlPlaneCoordinator(controlPlaneClient),
                readinessObserver = readiness,
                lifecycleExecutor = Executor(Runnable::run),
                selectedPreset = { QUALITY_PRESET },
            )
        var prepared: Result<AnalysisLimits>? = null

        runtime.prepare(AnalysisOperationId("op-withdrawn")) { prepared = it }

        val failure = prepared!!.exceptionOrNull() as AnalysisRuntimeException
        assertEquals(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE, failure.code)
        assertEquals(null, controlPlaneClient.lastResolvedSetupRequest)
        assertEquals(null, controlPlaneClient.lastActivationRequest)
        assertFalse(delegate.prepared)
        assertEquals(0, readiness.observeCalls)
    }

    private companion object {
        val USE_CASE_ID = UseCaseId("document-pii-detection")
        val DEFAULT_PRESET = InferencePresetRef(InferencePresetId("qwen35-json"), 1)
        val QUALITY_PRESET = InferencePresetRef(InferencePresetId("quality"), 2)
        val ACTIVATION_ID = ConsumerActivationId("activation-1")
    }
}

private class FakeReadinessObserver(
    private val events: MutableList<String> = mutableListOf(),
    private val failure: AnalysisRuntimeException? = null,
) : LocalAiRuntimeReadinessObserver {
    var observeCalls = 0
    var closeCalls = 0

    override fun observe(
        operationId: AnalysisOperationId,
        activationId: ConsumerActivationId,
    ): AutoCloseable {
        observeCalls += 1
        events += "readiness"
        failure?.let { throw it }
        return AutoCloseable { closeCalls += 1 }
    }
}

private class FakeControlPlaneClient(
    private val events: MutableList<String> = mutableListOf(),
    private val assignments: List<ConsumerAssignedUseCase> =
        listOf(
            ConsumerAssignedUseCase(
                useCaseId = UseCaseId("document-pii-detection"),
                useCaseRevision = 1,
                bindingRevision = 1,
                displayName = "Document PII detection",
                description = "Synthetic assignment",
                isDefault = true,
            ),
        ),
    private val presets: List<ConsumerPublishedPreset> =
        listOf(
            ConsumerPublishedPreset(
                preset = InferencePresetRef(InferencePresetId("qwen35-json"), 1),
                displayName = "Balanced",
                description = "Synthetic default",
                isDefault = true,
            ),
        ),
    private val activationFailure: ConsumerControlPlaneFailure? = null,
    private val resolvedUseCaseRevision: Int = 1,
) : ConsumerControlPlaneClient {
    var lastResolvedSetupRequest: ConsumerSetupResolutionRequest? = null
    var lastActivationRequest: ConsumerActivationRequest? = null
    val deactivated = mutableListOf<ConsumerActivationId>()

    override fun assignedUseCases(): ConsumerAssignedUseCasesResult {
        events += "assigned"
        return ConsumerAssignedUseCasesResult.Available(assignments)
    }

    override fun publishedPresets(useCaseId: UseCaseId): ConsumerPublishedPresetsResult {
        events += "presets"
        return ConsumerPublishedPresetsResult.Available(
            useCaseId = useCaseId,
            bindingRevision = assignments.single().bindingRevision,
            presets = presets,
        )
    }

    override fun resolveSetup(request: ConsumerSetupResolutionRequest): ConsumerSetupResolutionResult {
        events += "resolve-setup"
        lastResolvedSetupRequest = request
        return ConsumerSetupResolutionResult.Resolved(
            ConsumerResolvedSetup(
                useCaseId = request.useCaseId,
                useCaseRevision = resolvedUseCaseRevision,
                bindingRevision = request.bindingRevision,
                preset = request.preset,
                modelProfileId = "qwen35-0.8b-q4",
                contextTokens = 4_096,
                generation =
                    ConsumerGenerationConfiguration(
                        maxOutputTokens = 512,
                        temperature = 0.2f,
                        topP = 0.9f,
                        topK = 40,
                        minP = 0f,
                        presencePenalty = 0f,
                        repeatPenalty = 1.05f,
                        repeatLastN = 64,
                        thinkingMode = ThinkingMode.DISABLED,
                        seedPolicy = SeedPolicyType.FIXED,
                    ),
            ),
        )
    }

    override fun activate(request: ConsumerActivationRequest): ConsumerActivationResult {
        events += "activate"
        lastActivationRequest = request
        val activationFailure = activationFailure
        if (activationFailure != null) return ConsumerActivationResult.Rejected(activationFailure)
        return ConsumerActivationResult.Activated(
            ConsumerActivation(
                activationId = ConsumerActivationId("activation-1"),
                useCaseId = request.useCaseId,
                useCaseRevision = request.useCaseRevision,
                bindingRevision = request.bindingRevision,
                preset = request.preset,
            ),
        )
    }

    override fun deactivate(activationId: ConsumerActivationId): ConsumerDeactivationResult {
        events += "deactivate"
        deactivated += activationId
        return ConsumerDeactivationResult.Released
    }
}

private class FakeAnalysisRuntime(
    private val events: MutableList<String> = mutableListOf(),
) : AnalysisRuntimePort {
    var prepared = false
    var cancelCalls = 0
    var closeCalls = 0

    override fun prepare(
        operationId: AnalysisOperationId,
        onResult: (Result<AnalysisLimits>) -> Unit,
    ) {
        events += "delegate-prepare"
        prepared = true
        onResult(Result.success(AnalysisLimits(8_000, 4_000)))
    }

    override fun generate(
        operationId: AnalysisOperationId,
        chunk: AnalysisChunk,
        onResult: (Result<String>) -> Unit,
    ) {
        onResult(Result.success("{}"))
    }

    override fun cancel(
        operationId: AnalysisOperationId,
        onCancelled: () -> Unit,
    ) {
        cancelCalls += 1
        onCancelled()
    }

    override fun close(operationId: AnalysisOperationId) {
        closeCalls += 1
    }
}
