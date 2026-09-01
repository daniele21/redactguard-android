package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.contracts.ConsumerAssignedUseCase
import io.github.daniele21.localllm.contracts.ConsumerGenerationConfiguration
import io.github.daniele21.localllm.contracts.ConsumerPublishedPreset
import io.github.daniele21.localllm.contracts.ConsumerResolvedSetup
import io.github.daniele21.localllm.contracts.InferencePresetId
import io.github.daniele21.localllm.contracts.InferencePresetRef
import io.github.daniele21.localllm.contracts.SeedPolicyType
import io.github.daniele21.localllm.contracts.ThinkingMode
import io.github.daniele21.localllm.contracts.UseCaseId
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeFailureCode
import io.github.daniele21.redactguard.domain.analysis.LocalAiExecutionPhase
import io.github.daniele21.redactguard.domain.analysis.LocalAiExecutionState
import io.github.daniele21.redactguard.domain.analysis.LocalAiPreparationAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAiSetupStateTest {
    @Test
    fun `preset change invalidates previous compatibility until Host resolves new setup`() {
        val projection = LocalAiSetupStateProjection()
        projection.onTransportConnected()
        val firstInspection = inspection(BALANCED_PRESET)
        projection.onSetupResolved(firstInspection)
        assertTrue(projection.state.value.compatible)

        projection.onPresetSelected(QUALITY_PRESET)

        val configured = projection.state.value
        assertEquals(LocalAiSetupStage.CONFIGURED, configured.stage)
        assertEquals(QUALITY_PRESET, configured.selectedPreset)
        assertTrue(configured.configured)
        assertFalse(configured.compatible)
        assertNull(configured.resolvedSetup)

        val qualityInspection = inspection(QUALITY_PRESET)
        projection.onSetupResolved(qualityInspection)

        val compatible = projection.state.value
        assertEquals(LocalAiSetupStage.COMPATIBLE, compatible.stage)
        assertSame(qualityInspection.resolvedSetup, compatible.resolvedSetup)
        assertEquals(QUALITY_PRESET, compatible.selectedPreset)
    }

    @Test
    fun `runtime readiness is derived only from explicit execution evidence`() {
        val projection = LocalAiSetupStateProjection()
        projection.onTransportConnected()
        projection.onPresetSelected(BALANCED_PRESET)

        projection.onExecutionState(LocalAiExecutionState(LocalAiExecutionPhase.READY))
        assertEquals(LocalAiSetupStage.CONFIGURED, projection.state.value.stage)

        projection.onSetupResolved(inspection(BALANCED_PRESET))
        projection.onExecutionState(
            LocalAiExecutionState(
                phase = LocalAiExecutionPhase.PREPARING,
                preparationAction = LocalAiPreparationAction.LOADING,
            ),
        )
        assertEquals(LocalAiSetupStage.COMPATIBLE, projection.state.value.stage)

        projection.onExecutionState(LocalAiExecutionState(LocalAiExecutionPhase.READY))
        assertEquals(LocalAiSetupStage.RUNTIME_READY, projection.state.value.stage)
        assertTrue(projection.state.value.runtimeReady)

        projection.onExecutionState(LocalAiExecutionState(LocalAiExecutionPhase.GENERATING))
        assertEquals(LocalAiSetupStage.RUNTIME_READY, projection.state.value.stage)
    }

    @Test
    fun `configuration stale evidence fails closed to configured and disconnect clears identity`() {
        val projection = LocalAiSetupStateProjection()
        projection.onTransportConnected()
        projection.onSetupResolved(inspection(BALANCED_PRESET))
        projection.onExecutionState(LocalAiExecutionState(LocalAiExecutionPhase.READY))

        projection.onExecutionState(
            LocalAiExecutionState(
                phase = LocalAiExecutionPhase.FAILED,
                failureCode = AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE,
            ),
        )

        val stale = projection.state.value
        assertEquals(LocalAiSetupStage.CONFIGURED, stale.stage)
        assertEquals(BALANCED_PRESET, stale.selectedPreset)
        assertNull(stale.resolvedSetup)
        assertFalse(stale.compatible)

        projection.onTransportDisconnected()

        val disconnected = projection.state.value
        assertEquals(LocalAiSetupStage.DISCONNECTED, disconnected.stage)
        assertFalse(disconnected.connected)
        assertFalse(disconnected.configured)
        assertNull(disconnected.selectedPreset)
        assertNull(disconnected.resolvedSetup)
    }

    private fun inspection(preset: InferencePresetRef): ConsumerControlPlaneSetupInspection {
        val assignment =
            ConsumerAssignedUseCase(
                useCaseId = USE_CASE_ID,
                useCaseRevision = 7,
                bindingRevision = 11,
                displayName = "Document PII detection",
                description = "Synthetic assignment",
                isDefault = true,
            )
        val published =
            ConsumerPublishedPreset(
                preset = preset,
                displayName = "Preset",
                description = "Synthetic consumer-safe preset",
                isDefault = true,
            )
        val resolved =
            ConsumerResolvedSetup(
                useCaseId = USE_CASE_ID,
                useCaseRevision = 7,
                bindingRevision = 11,
                preset = preset,
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
            )
        return ConsumerControlPlaneSetupInspection(
            assignment = assignment,
            selectedPreset = published,
            availablePresets = listOf(published),
            resolvedSetup = resolved,
            staleSelectionWouldBeReplaced = false,
        )
    }

    private companion object {
        val USE_CASE_ID = UseCaseId("document-pii-detection")
        val BALANCED_PRESET = InferencePresetRef(InferencePresetId("balanced"), 3)
        val QUALITY_PRESET = InferencePresetRef(InferencePresetId("quality"), 4)
    }
}
