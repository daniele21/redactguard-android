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
import io.github.daniele21.localllm.contracts.ConsumerPublishedPreset
import io.github.daniele21.localllm.contracts.ConsumerPublishedPresetsResult
import io.github.daniele21.localllm.contracts.InferencePresetId
import io.github.daniele21.localllm.contracts.InferencePresetRef
import io.github.daniele21.localllm.contracts.UseCaseId
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeException
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeFailureCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ConsumerControlPlaneActivationTest {
    @Test
    fun `activates once with exact host revisions and default preset`() {
        val controlPlane = FakeControlPlane()
        val activation = ConsumerControlPlaneActivation(controlPlane)

        val first = activation.ensureActivated()
        val second = activation.ensureActivated()

        assertEquals(first, second)
        assertEquals(1, controlPlane.assignmentCalls)
        assertEquals(1, controlPlane.presetCalls)
        assertEquals(1, controlPlane.activationCalls)
        assertEquals(USE_CASE, controlPlane.lastActivationRequest?.useCaseId)
        assertEquals(3, controlPlane.lastActivationRequest?.useCaseRevision)
        assertEquals(7, controlPlane.lastActivationRequest?.bindingRevision)
        assertEquals(PRESET_FAST, controlPlane.lastActivationRequest?.preset)
        assertEquals(PRESET_FAST, activation.activePreset)

        activation.deactivate()
        assertEquals(listOf(ConsumerActivationId("activation-1")), controlPlane.deactivated)
    }

    @Test
    fun `explicit advertised preset is used without model identity`() {
        val controlPlane = FakeControlPlane()
        val activation = ConsumerControlPlaneActivation(controlPlane, selectedPreset = { PRESET_QUALITY })

        activation.ensureActivated()

        assertEquals(PRESET_QUALITY, controlPlane.lastActivationRequest?.preset)
    }

    @Test
    fun `disconnect invalidation forces rediscovery and fresh activation`() {
        val controlPlane = FakeControlPlane()
        val activation = ConsumerControlPlaneActivation(controlPlane)
        activation.ensureActivated()

        activation.invalidate()
        activation.ensureActivated()

        assertEquals(2, controlPlane.assignmentCalls)
        assertEquals(2, controlPlane.presetCalls)
        assertEquals(2, controlPlane.activationCalls)
        assertEquals(0, controlPlane.deactivated.size)
    }

    @Test
    fun `missing assigned pii use case fails closed before preset discovery`() {
        val controlPlane =
            FakeControlPlane(
                assignments =
                    listOf(
                        assignment.copy(useCaseId = UseCaseId("other-use-case")),
                    ),
            )
        val activation = ConsumerControlPlaneActivation(controlPlane)

        val failure = assertThrows(AnalysisRuntimeException::class.java) { activation.ensureActivated() }

        assertEquals(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE, failure.code)
        assertEquals(0, controlPlane.presetCalls)
        assertEquals(0, controlPlane.activationCalls)
    }

    @Test
    fun `stale binding revision fails closed before activation`() {
        val controlPlane = FakeControlPlane(publishedBindingRevision = 8)
        val activation = ConsumerControlPlaneActivation(controlPlane)

        val failure = assertThrows(AnalysisRuntimeException::class.java) { activation.ensureActivated() }

        assertEquals(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE, failure.code)
        assertEquals(0, controlPlane.activationCalls)
    }

    @Test
    fun `host model unavailable maps to host unavailable without local fallback`() {
        val controlPlane =
            FakeControlPlane(
                activationFailure =
                    ConsumerControlPlaneFailure(
                        ConsumerControlPlaneErrorCode.MODEL_UNAVAILABLE,
                        "Configured Harness model is unavailable",
                    ),
            )
        val activation = ConsumerControlPlaneActivation(controlPlane)

        val failure = assertThrows(AnalysisRuntimeException::class.java) { activation.ensureActivated() }

        assertEquals(AnalysisRuntimeFailureCode.HOST_UNAVAILABLE, failure.code)
        assertEquals(1, controlPlane.activationCalls)
    }

    private companion object {
        val USE_CASE = REDACTGUARD_DOCUMENT_PII_USE_CASE
        val PRESET_FAST = InferencePresetRef(InferencePresetId("fast"), 1)
        val PRESET_QUALITY = InferencePresetRef(InferencePresetId("quality"), 2)
        val assignment =
            ConsumerAssignedUseCase(
                useCaseId = USE_CASE,
                useCaseRevision = 3,
                bindingRevision = 7,
                displayName = "Document PII detection",
                description = "Detect PII in RedactGuard documents",
                isDefault = true,
            )
    }
}

private class FakeControlPlane(
    private val assignments: List<ConsumerAssignedUseCase> = listOf(ConsumerControlPlaneActivationTest.assignment),
    private val publishedBindingRevision: Int = 7,
    private val activationFailure: ConsumerControlPlaneFailure? = null,
) : ConsumerControlPlaneClient {
    var assignmentCalls = 0
    var presetCalls = 0
    var activationCalls = 0
    var lastActivationRequest: ConsumerActivationRequest? = null
    val deactivated = mutableListOf<ConsumerActivationId>()

    override fun assignedUseCases(): ConsumerAssignedUseCasesResult {
        assignmentCalls += 1
        return ConsumerAssignedUseCasesResult.Available(assignments)
    }

    override fun publishedPresets(useCaseId: UseCaseId): ConsumerPublishedPresetsResult {
        presetCalls += 1
        return ConsumerPublishedPresetsResult.Available(
            useCaseId = useCaseId,
            bindingRevision = publishedBindingRevision,
            presets =
                listOf(
                    ConsumerPublishedPreset(
                        preset = ConsumerControlPlaneActivationTest.PRESET_FAST,
                        displayName = "Fast",
                        description = "Lower latency",
                        isDefault = true,
                    ),
                    ConsumerPublishedPreset(
                        preset = ConsumerControlPlaneActivationTest.PRESET_QUALITY,
                        displayName = "Quality",
                        description = "Higher quality",
                        isDefault = false,
                    ),
                ),
        )
    }

    override fun activate(request: ConsumerActivationRequest): ConsumerActivationResult {
        activationCalls += 1
        lastActivationRequest = request
        activationFailure?.let { return ConsumerActivationResult.Rejected(it) }
        return ConsumerActivationResult.Activated(
            ConsumerActivation(
                activationId = ConsumerActivationId("activation-$activationCalls"),
                useCaseId = request.useCaseId,
                useCaseRevision = request.useCaseRevision,
                bindingRevision = request.bindingRevision,
                preset = request.preset,
            ),
        )
    }

    override fun deactivate(activationId: ConsumerActivationId): ConsumerDeactivationResult {
        deactivated += activationId
        return ConsumerDeactivationResult.Released
    }
}
