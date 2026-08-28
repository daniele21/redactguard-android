package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.contracts.ConsumerActivation
import io.github.daniele21.localllm.contracts.ConsumerActivationId
import io.github.daniele21.localllm.contracts.ConsumerActivationRequest
import io.github.daniele21.localllm.contracts.ConsumerActivationResult
import io.github.daniele21.localllm.contracts.ConsumerAssignedUseCase
import io.github.daniele21.localllm.contracts.ConsumerAssignedUseCasesResult
import io.github.daniele21.localllm.contracts.ConsumerControlPlaneClient
import io.github.daniele21.localllm.contracts.ConsumerControlPlaneErrorCode
import io.github.daniele21.localllm.contracts.ConsumerDeactivationResult
import io.github.daniele21.localllm.contracts.ConsumerPublishedPreset
import io.github.daniele21.localllm.contracts.ConsumerPublishedPresetsResult
import io.github.daniele21.localllm.contracts.InferencePresetRef
import io.github.daniele21.localllm.contracts.UseCaseId
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeException
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeFailureCode

/**
 * RedactGuard-side owner of the Harness control-plane handshake needed before Consumer API
 * capabilities can be resolved. Exact model/runtime identity remains Host-owned.
 */
internal class ConsumerControlPlaneCoordinator(
    private val client: ConsumerControlPlaneClient,
    private val transportConnected: () -> Boolean = { true },
    private val useCaseId: UseCaseId = DOCUMENT_PII_USE_CASE,
    private val presetSelection: ProcessLocalPresetSelection = ProcessLocalPresetSelection(),
) {
    fun refreshPresetSelection(requestedPreset: InferencePresetRef? = null): InferencePresetRef {
        val assignment = discoverAssignment()
        val published = discoverPresets(assignment)
        return presetSelection.resolve(published, requestedPreset)
            ?: throw runtimeFailure(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)
    }

    fun activate(requestedPreset: InferencePresetRef? = null): AnalysisActivation {
        val assignment = discoverAssignment()
        val published = discoverPresets(assignment)
        val preset =
            presetSelection.resolve(published, requestedPreset)
                ?: throw runtimeFailure(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)
        val request =
            localAiBoundary(STEP_ACTIVATION_REQUEST) {
                ConsumerActivationRequest(
                    useCaseId = useCaseId,
                    useCaseRevision = assignment.useCaseRevision,
                    bindingRevision = assignment.bindingRevision,
                    preset = preset,
                )
            }
        val activation =
            when (val result = localAiBoundary(STEP_ACTIVATE) { client.activate(request) }) {
                is ConsumerActivationResult.Activated -> result.activation
                is ConsumerActivationResult.Rejected -> throw runtimeFailure(result.failure.toAnalysisFailureCode(transportConnected))
            }
        if (!activationMatches(activation, request)) {
            runCatching { client.deactivate(activation.activationId) }
            throw runtimeFailure(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)
        }
        return AnalysisActivation(activation.activationId, preset)
    }

    fun deactivate(activationId: ConsumerActivationId) {
        when (val result = localAiBoundary(STEP_DEACTIVATE) { client.deactivate(activationId) }) {
            ConsumerDeactivationResult.Released -> {
                Unit
            }

            is ConsumerDeactivationResult.Rejected -> {
                if (result.failure.code == ConsumerControlPlaneErrorCode.TRANSPORT_FAILURE && !transportConnected()) {
                    return
                }
                throw runtimeFailure(result.failure.toAnalysisFailureCode(transportConnected))
            }
        }
    }

    fun deactivateBestEffort(activationId: ConsumerActivationId) {
        runCatching { deactivate(activationId) }
    }

    private fun discoverAssignment(): ConsumerAssignedUseCase {
        val assignments =
            when (val result = localAiBoundary(STEP_ASSIGNED_USE_CASES) { client.assignedUseCases() }) {
                is ConsumerAssignedUseCasesResult.Available -> result.assignments
                is ConsumerAssignedUseCasesResult.Rejected -> throw runtimeFailure(result.failure.toAnalysisFailureCode(transportConnected))
            }
        val matches = assignments.filter { it.useCaseId == useCaseId }
        if (matches.size != 1) throw runtimeFailure(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)
        return matches.single()
    }

    private fun discoverPresets(assignment: ConsumerAssignedUseCase): List<ConsumerPublishedPreset> {
        val result =
            when (val published = localAiBoundary(STEP_PUBLISHED_PRESETS) { client.publishedPresets(useCaseId) }) {
                is ConsumerPublishedPresetsResult.Available -> {
                    published
                }

                is ConsumerPublishedPresetsResult.Rejected -> {
                    throw runtimeFailure(published.failure.toAnalysisFailureCode(transportConnected))
                }
            }
        if (result.useCaseId != useCaseId || result.bindingRevision != assignment.bindingRevision) {
            throw runtimeFailure(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)
        }
        val identities = result.presets.map(ConsumerPublishedPreset::preset)
        if (identities.isEmpty() || identities.distinct().size != identities.size) {
            throw runtimeFailure(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)
        }
        return result.presets
    }

    private fun activationMatches(
        activation: ConsumerActivation,
        request: ConsumerActivationRequest,
    ): Boolean =
        activation.useCaseId == request.useCaseId &&
            activation.useCaseRevision == request.useCaseRevision &&
            activation.bindingRevision == request.bindingRevision &&
            activation.preset == request.preset

    private companion object {
        val DOCUMENT_PII_USE_CASE = UseCaseId("document-pii-detection")
        const val STEP_ASSIGNED_USE_CASES = "control-plane.assigned-use-cases"
        const val STEP_PUBLISHED_PRESETS = "control-plane.published-presets"
        const val STEP_ACTIVATION_REQUEST = "control-plane.activation-request"
        const val STEP_ACTIVATE = "control-plane.activate"
        const val STEP_DEACTIVATE = "control-plane.deactivate"
    }
}

internal data class AnalysisActivation(
    val activationId: ConsumerActivationId,
    val preset: InferencePresetRef,
)

private fun runtimeFailure(code: AnalysisRuntimeFailureCode): AnalysisRuntimeException = AnalysisRuntimeException(code)
