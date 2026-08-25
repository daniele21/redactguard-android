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
) {
    fun activate(requestedPreset: InferencePresetRef? = null): AnalysisActivation {
        val assignment = discoverAssignment()
        val published = discoverPresets(assignment)
        val preset = selectPreset(published, requestedPreset)
        val request =
            ConsumerActivationRequest(
                useCaseId = useCaseId,
                useCaseRevision = assignment.useCaseRevision,
                bindingRevision = assignment.bindingRevision,
                preset = preset,
            )
        val activation =
            when (val result = client.activate(request)) {
                is ConsumerActivationResult.Activated -> result.activation
                is ConsumerActivationResult.Rejected -> throw runtimeFailure(mapControlPlaneFailure(result.failure))
            }
        if (!activationMatches(activation, request)) {
            runCatching { client.deactivate(activation.activationId) }
            throw runtimeFailure(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)
        }
        return AnalysisActivation(activation.activationId, preset)
    }

    fun deactivate(activationId: ConsumerActivationId) {
        when (val result = client.deactivate(activationId)) {
            ConsumerDeactivationResult.Released -> {
                Unit
            }

            is ConsumerDeactivationResult.Rejected -> {
                if (result.failure.code == ConsumerControlPlaneErrorCode.TRANSPORT_FAILURE && !transportConnected()) {
                    return
                }
                throw runtimeFailure(mapControlPlaneFailure(result.failure))
            }
        }
    }

    fun deactivateBestEffort(activationId: ConsumerActivationId) {
        runCatching { deactivate(activationId) }
    }

    private fun discoverAssignment(): ConsumerAssignedUseCase {
        val assignments =
            when (val result = client.assignedUseCases()) {
                is ConsumerAssignedUseCasesResult.Available -> result.assignments
                is ConsumerAssignedUseCasesResult.Rejected -> throw runtimeFailure(mapControlPlaneFailure(result.failure))
            }
        val matches = assignments.filter { it.useCaseId == useCaseId }
        if (matches.size != 1) throw runtimeFailure(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)
        return matches.single()
    }

    private fun discoverPresets(assignment: ConsumerAssignedUseCase): List<ConsumerPublishedPreset> {
        val result =
            when (val published = client.publishedPresets(useCaseId)) {
                is ConsumerPublishedPresetsResult.Available -> published
                is ConsumerPublishedPresetsResult.Rejected -> throw runtimeFailure(mapControlPlaneFailure(published.failure))
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

    private fun selectPreset(
        presets: List<ConsumerPublishedPreset>,
        requestedPreset: InferencePresetRef?,
    ): InferencePresetRef {
        if (requestedPreset != null) {
            if (presets.none { it.preset == requestedPreset }) {
                throw runtimeFailure(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)
            }
            return requestedPreset
        }
        val defaults = presets.filter(ConsumerPublishedPreset::isDefault)
        if (defaults.size != 1) throw runtimeFailure(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)
        return defaults.single().preset
    }

    private fun activationMatches(
        activation: ConsumerActivation,
        request: ConsumerActivationRequest,
    ): Boolean =
        activation.useCaseId == request.useCaseId &&
            activation.useCaseRevision == request.useCaseRevision &&
            activation.bindingRevision == request.bindingRevision &&
            activation.preset == request.preset

    private fun mapControlPlaneFailure(failure: ConsumerControlPlaneFailure): AnalysisRuntimeFailureCode =
        when (failure.code) {
            ConsumerControlPlaneErrorCode.TRANSPORT_FAILURE -> {
                AnalysisRuntimeFailureCode.DISCONNECTED
            }

            ConsumerControlPlaneErrorCode.MODEL_UNAVAILABLE,
            ConsumerControlPlaneErrorCode.CONFIGURATION_REQUIRED,
            ConsumerControlPlaneErrorCode.MODEL_CONFLICT,
            ConsumerControlPlaneErrorCode.ACTIVATION_ALREADY_ACTIVE,
            -> {
                AnalysisRuntimeFailureCode.HOST_UNAVAILABLE
            }

            ConsumerControlPlaneErrorCode.RUNTIME_FAILURE -> {
                if (transportConnected()) {
                    AnalysisRuntimeFailureCode.GENERATION_FAILED
                } else {
                    AnalysisRuntimeFailureCode.DISCONNECTED
                }
            }

            else -> {
                AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE
            }
        }

    private companion object {
        val DOCUMENT_PII_USE_CASE = UseCaseId("document-pii-detection")
    }
}

internal data class AnalysisActivation(
    val activationId: ConsumerActivationId,
    val preset: InferencePresetRef,
)

private fun runtimeFailure(code: AnalysisRuntimeFailureCode): AnalysisRuntimeException = AnalysisRuntimeException(code)
