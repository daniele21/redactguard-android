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
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeDiagnostic
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeException
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeFailureCode
import io.github.daniele21.redactguard.domain.analysis.LocalAiExecutionPhase
import io.github.daniele21.redactguard.domain.analysis.LocalAiExecutionState
import io.github.daniele21.redactguard.domain.analysis.LocalAiPreparationAction
import io.github.daniele21.redactguard.domain.failure.FailureRecoveryAction
import io.github.daniele21.redactguard.domain.failure.ProductFailureKind
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
        assertFalse(configured.runtimeReady)
        assertNull(configured.resolvedSetup)

        val qualityInspection = inspection(QUALITY_PRESET)
        projection.onSetupResolved(qualityInspection)

        val compatible = projection.state.value
        assertEquals(LocalAiSetupStage.COMPATIBLE, compatible.stage)
        assertSame(qualityInspection.resolvedSetup, compatible.resolvedSetup)
        assertEquals(QUALITY_PRESET, compatible.selectedPreset)
    }

    @Test
    fun `runtime readiness is independent from passive compatible setup stage`() {
        val projection = LocalAiSetupStateProjection()
        projection.onTransportConnected()
        projection.onPresetSelected(BALANCED_PRESET)

        projection.onExecutionState(LocalAiExecutionState(LocalAiExecutionPhase.READY))
        assertEquals(LocalAiSetupStage.CONFIGURED, projection.state.value.stage)
        assertFalse(projection.state.value.runtimeReady)

        projection.onSetupResolved(inspection(BALANCED_PRESET))
        projection.onExecutionState(
            LocalAiExecutionState(
                phase = LocalAiExecutionPhase.PREPARING,
                preparationAction = LocalAiPreparationAction.LOADING,
            ),
        )
        assertEquals(LocalAiSetupStage.COMPATIBLE, projection.state.value.stage)
        assertFalse(projection.state.value.runtimeReady)

        projection.onExecutionState(LocalAiExecutionState(LocalAiExecutionPhase.READY))
        assertEquals(LocalAiSetupStage.COMPATIBLE, projection.state.value.stage)
        assertTrue(projection.state.value.runtimeReady)

        projection.onExecutionState(LocalAiExecutionState(LocalAiExecutionPhase.GENERATING))
        assertEquals(LocalAiSetupStage.COMPATIBLE, projection.state.value.stage)
        assertTrue(projection.state.value.runtimeReady)
    }

    @Test
    fun `setup failure preserves product problem recovery and bounded technical identity`() {
        val projection = LocalAiSetupStateProjection()
        projection.onTransportConnected()
        projection.onPresetSelected(BALANCED_PRESET)
        val diagnostic =
            AnalysisRuntimeDiagnostic(
                step = "control-plane.setup-resolution",
                type = "ControlPlane:CONFIGURATION_REQUIRED",
            )

        projection.onSetupFailure(
            AnalysisRuntimeException(
                code = AnalysisRuntimeFailureCode.CONFIGURATION_REQUIRED,
                diagnostic = diagnostic,
            ),
        )

        val state = projection.state.value
        assertEquals(LocalAiSetupStage.CONFIGURED, state.stage)
        assertEquals(ProductFailureKind.LOCAL_AI_CONFIGURATION_REQUIRED, state.problem)
        assertEquals(FailureRecoveryAction.OPEN_HARNESS, state.recoveryAction)
        assertEquals(diagnostic, state.technicalIdentity)
        assertFalse(state.compatible)
        assertFalse(state.runtimeReady)
    }

    @Test
    fun `model unavailable is not represented as incompatibility`() {
        val projection = LocalAiSetupStateProjection()
        projection.onTransportConnected()
        projection.onPresetSelected(BALANCED_PRESET)

        projection.onSetupFailure(AnalysisRuntimeException(AnalysisRuntimeFailureCode.MODEL_UNAVAILABLE))

        assertEquals(ProductFailureKind.LOCAL_AI_MODEL_UNAVAILABLE, projection.state.value.problem)
        assertFalse(projection.state.value.problem == ProductFailureKind.CAPABILITY_INCOMPATIBLE)
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
        assertFalse(stale.runtimeReady)
        assertEquals(ProductFailureKind.CAPABILITY_INCOMPATIBLE, stale.problem)
        assertEquals(FailureRecoveryAction.UPDATE_HARNESS, stale.recoveryAction)

        projection.onTransportDisconnected()

        val disconnected = projection.state.value
        assertEquals(LocalAiSetupStage.DISCONNECTED, disconnected.stage)
        assertFalse(disconnected.connected)
        assertFalse(disconnected.configured)
        assertNull(disconnected.selectedPreset)
        assertNull(disconnected.resolvedSetup)
        assertNull(disconnected.problem)
        assertNull(disconnected.technicalIdentity)
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
