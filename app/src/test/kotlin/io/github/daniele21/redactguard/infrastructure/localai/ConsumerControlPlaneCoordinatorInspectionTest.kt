package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.contracts.ConsumerActivationId
import io.github.daniele21.localllm.contracts.ConsumerActivationRequest
import io.github.daniele21.localllm.contracts.ConsumerActivationResult
import io.github.daniele21.localllm.contracts.ConsumerAssignedUseCase
import io.github.daniele21.localllm.contracts.ConsumerAssignedUseCasesResult
import io.github.daniele21.localllm.contracts.ConsumerControlPlaneClient
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
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeException
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeFailureCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsumerControlPlaneCoordinatorInspectionTest {
    @Test
    fun `inspection returns Host resolved setup without activation or selection mutation`() {
        val selection = ProcessLocalPresetSelection()
        val client = InspectionControlPlaneClient()
        val coordinator = ConsumerControlPlaneCoordinator(client = client, presetSelection = selection)

        val inspected = coordinator.inspectSetup()

        assertEquals(USE_CASE_ID, inspected.assignment.useCaseId)
        assertEquals(7, inspected.assignment.useCaseRevision)
        assertEquals(11, inspected.assignment.bindingRevision)
        assertEquals(DEFAULT_PRESET, inspected.selectedPreset.preset)
        assertEquals(2, inspected.availablePresets.size)
        assertEquals("qwen35-0.8b-q4", inspected.resolvedSetup.modelProfileId)
        assertEquals(4096, inspected.resolvedSetup.contextTokens)
        assertEquals(1, client.resolveSetupCalls)
        assertFalse(inspected.staleSelectionWouldBeReplaced)
        assertEquals(0, client.activateCalls)
        assertEquals(0, client.deactivateCalls)
        assertNull(selection.state.value.selectedPreset)
        assertTrue(selection.state.value.options.isEmpty())
    }

    @Test
    fun `inspection previews stale selection replacement without committing it`() {
        val selection = ProcessLocalPresetSelection()
        selection.resolve(
            listOf(
                published(DEFAULT_PRESET, isDefault = true),
                published(QUALITY_PRESET, isDefault = false),
            ),
            null,
        )
        assertTrue(selection.select(QUALITY_PRESET))
        val client = InspectionControlPlaneClient(presets = listOf(published(DEFAULT_PRESET, isDefault = true)))
        val coordinator = ConsumerControlPlaneCoordinator(client = client, presetSelection = selection)

        val inspected = coordinator.inspectSetup()

        assertEquals(DEFAULT_PRESET, inspected.selectedPreset.preset)
        assertTrue(inspected.staleSelectionWouldBeReplaced)
        assertEquals(QUALITY_PRESET, selection.state.value.selectedPreset)
        assertEquals(0, client.activateCalls)
        assertEquals(0, client.deactivateCalls)
    }

    @Test
    fun `inspection remains fail closed for requested preset that host does not publish`() {
        val client = InspectionControlPlaneClient(presets = listOf(published(DEFAULT_PRESET, isDefault = true)))
        val coordinator = ConsumerControlPlaneCoordinator(client)

        val failure = runCatching { coordinator.inspectSetup(QUALITY_PRESET) }.exceptionOrNull()

        assertTrue(failure is AnalysisRuntimeException)
        assertEquals(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE, (failure as AnalysisRuntimeException).code)
        assertEquals("control-plane.preset-selection", failure.diagnostic?.step)
        assertEquals("PRESET_SELECTION_UNAVAILABLE", failure.diagnostic?.type)
        assertEquals(0, client.resolveSetupCalls)
        assertEquals(0, client.activateCalls)
        assertEquals(0, client.deactivateCalls)
    }

    @Test
    fun `inspection rejects Host setup whose immutable identity does not match discovery`() {
        val client = InspectionControlPlaneClient(resolvedUseCaseRevision = 8)
        val coordinator = ConsumerControlPlaneCoordinator(client)

        val failure = runCatching { coordinator.inspectSetup() }.exceptionOrNull()

        assertTrue(failure is AnalysisRuntimeException)
        assertEquals(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE, (failure as AnalysisRuntimeException).code)
        assertEquals("control-plane.setup-resolution", failure.diagnostic?.step)
        assertEquals("SETUP_IDENTITY_MISMATCH", failure.diagnostic?.type)
        assertEquals(0, client.activateCalls)
    }

    private companion object {
        val USE_CASE_ID = UseCaseId("document-pii-detection")
        val DEFAULT_PRESET = InferencePresetRef(InferencePresetId("balanced"), 3)
        val QUALITY_PRESET = InferencePresetRef(InferencePresetId("quality"), 4)
    }
}

private class InspectionControlPlaneClient(
    private val assignments: List<ConsumerAssignedUseCase> =
        listOf(
            ConsumerAssignedUseCase(
                useCaseId = UseCaseId("document-pii-detection"),
                useCaseRevision = 7,
                bindingRevision = 11,
                displayName = "Document PII detection",
                description = "Synthetic assignment",
                isDefault = true,
            ),
        ),
    private val presets: List<ConsumerPublishedPreset> =
        listOf(
            published(InferencePresetRef(InferencePresetId("balanced"), 3), isDefault = true),
            published(InferencePresetRef(InferencePresetId("quality"), 4), isDefault = false),
        ),
    private val resolvedUseCaseRevision: Int = 7,
) : ConsumerControlPlaneClient {
    var resolveSetupCalls = 0
    var activateCalls = 0
    var deactivateCalls = 0

    override fun assignedUseCases(): ConsumerAssignedUseCasesResult = ConsumerAssignedUseCasesResult.Available(assignments)

    override fun publishedPresets(useCaseId: UseCaseId): ConsumerPublishedPresetsResult =
        ConsumerPublishedPresetsResult.Available(
            useCaseId = useCaseId,
            bindingRevision = assignments.single().bindingRevision,
            presets = presets,
        )

    override fun resolveSetup(request: ConsumerSetupResolutionRequest): ConsumerSetupResolutionResult {
        resolveSetupCalls += 1
        return ConsumerSetupResolutionResult.Resolved(
            ConsumerResolvedSetup(
                useCaseId = request.useCaseId,
                useCaseRevision = resolvedUseCaseRevision,
                bindingRevision = request.bindingRevision,
                preset = request.preset,
                modelProfileId = "qwen35-0.8b-q4",
                contextTokens = 4096,
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
        activateCalls += 1
        error("Inspection must not activate")
    }

    override fun deactivate(activationId: ConsumerActivationId): ConsumerDeactivationResult {
        deactivateCalls += 1
        return ConsumerDeactivationResult.Released
    }
}

private fun published(
    preset: InferencePresetRef,
    isDefault: Boolean,
): ConsumerPublishedPreset =
    ConsumerPublishedPreset(
        preset = preset,
        displayName = if (isDefault) "Balanced" else "Quality",
        description = "Synthetic consumer-safe preset",
        isDefault = isDefault,
    )
