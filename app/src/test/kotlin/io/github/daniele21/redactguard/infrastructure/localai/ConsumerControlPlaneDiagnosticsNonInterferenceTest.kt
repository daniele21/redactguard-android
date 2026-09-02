package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.contracts.ConsumerActivationId
import io.github.daniele21.localllm.contracts.ConsumerActivationRequest
import io.github.daniele21.localllm.contracts.ConsumerAssignedUseCase
import io.github.daniele21.localllm.contracts.ConsumerAssignedUseCasesResult
import io.github.daniele21.localllm.contracts.ConsumerControlPlaneClient
import io.github.daniele21.localllm.contracts.ConsumerControlPlaneErrorCode
import io.github.daniele21.localllm.contracts.ConsumerControlPlaneFailure
import io.github.daniele21.localllm.contracts.ConsumerPublishedPreset
import io.github.daniele21.localllm.contracts.ConsumerPublishedPresetsResult
import io.github.daniele21.localllm.contracts.ConsumerSetupResolutionRequest
import io.github.daniele21.localllm.contracts.ConsumerSetupResolutionResult
import io.github.daniele21.localllm.contracts.InferencePresetId
import io.github.daniele21.localllm.contracts.InferencePresetRef
import io.github.daniele21.localllm.contracts.UseCaseId
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeException
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeFailureCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsumerControlPlaneDiagnosticsNonInterferenceTest {
    @Test
    fun `setup rejection preserves typed identity regardless of human message formatting`() {
        val messages =
            listOf(
                "Required local model is unavailable",
                "Modello locale non disponibile — riprovare più tardi",
                "synthetic detail ${"x".repeat(240)}",
                "detail: value/with spaces + punctuation?",
            )

        messages.forEach { message ->
            val events = mutableListOf<LocalAiTechnicalEvent>()
            val coordinator =
                ConsumerControlPlaneCoordinator(
                    client = RejectingSetupControlPlaneClient(message),
                    technicalDiagnostics = LocalAiTechnicalDiagnostics(events::add),
                )

            val failure =
                assertThrows(AnalysisRuntimeException::class.java) {
                    coordinator.inspectSetup()
                }

            assertEquals(AnalysisRuntimeFailureCode.HOST_UNAVAILABLE, failure.code)
            assertEquals("control-plane.setup-resolution", failure.diagnostic?.step)
            assertEquals("ControlPlane:MODEL_UNAVAILABLE", failure.diagnostic?.type)

            val rejection =
                events.single {
                    it.step == "control-plane.setup-resolution" && it.result == "REJECTED"
                }
            assertEquals("MODEL_UNAVAILABLE", rejection.reason)
            assertFalse(rejection.render().contains(message))
            assertTrue(rejection.render().length <= 128)
        }
    }

    @Test
    fun `failing diagnostic sink cannot replace canonical setup failure`() {
        val coordinator =
            ConsumerControlPlaneCoordinator(
                client = RejectingSetupControlPlaneClient("Required local model is unavailable"),
                technicalDiagnostics =
                    LocalAiTechnicalDiagnostics {
                        throw IllegalStateException("synthetic diagnostics sink failure")
                    },
            )

        val failure =
            assertThrows(AnalysisRuntimeException::class.java) {
                coordinator.inspectSetup()
            }

        assertEquals(AnalysisRuntimeFailureCode.HOST_UNAVAILABLE, failure.code)
        assertEquals("control-plane.setup-resolution", failure.diagnostic?.step)
        assertEquals("ControlPlane:MODEL_UNAVAILABLE", failure.diagnostic?.type)
    }
}

private class RejectingSetupControlPlaneClient(
    private val message: String,
) : ConsumerControlPlaneClient {
    override fun assignedUseCases(): ConsumerAssignedUseCasesResult =
        ConsumerAssignedUseCasesResult.Available(
            listOf(
                ConsumerAssignedUseCase(
                    useCaseId = USE_CASE_ID,
                    useCaseRevision = 7,
                    bindingRevision = 11,
                    displayName = "Document PII detection",
                    description = "Synthetic assignment",
                    isDefault = true,
                ),
            ),
        )

    override fun publishedPresets(useCaseId: UseCaseId): ConsumerPublishedPresetsResult =
        ConsumerPublishedPresetsResult.Available(
            useCaseId = useCaseId,
            bindingRevision = 11,
            presets =
                listOf(
                    ConsumerPublishedPreset(
                        preset = PRESET,
                        displayName = "Balanced",
                        description = "Synthetic consumer-safe preset",
                        isDefault = true,
                    ),
                ),
        )

    override fun resolveSetup(request: ConsumerSetupResolutionRequest): ConsumerSetupResolutionResult =
        ConsumerSetupResolutionResult.Rejected(
            ConsumerControlPlaneFailure(
                code = ConsumerControlPlaneErrorCode.MODEL_UNAVAILABLE,
                message = message,
            ),
        )

    override fun activate(request: ConsumerActivationRequest) = error("must not be reached")

    override fun deactivate(activationId: ConsumerActivationId) = error("must not be reached")

    private companion object {
        val USE_CASE_ID = UseCaseId("document-pii-detection")
        val PRESET = InferencePresetRef(InferencePresetId("balanced"), 3)
    }
}