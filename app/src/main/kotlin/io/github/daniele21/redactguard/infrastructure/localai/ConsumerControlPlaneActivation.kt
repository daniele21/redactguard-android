package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.contracts.ConsumerActivation
import io.github.daniele21.localllm.contracts.ConsumerActivationRequest
import io.github.daniele21.localllm.contracts.ConsumerActivationResult
import io.github.daniele21.localllm.contracts.ConsumerAssignedUseCasesResult
import io.github.daniele21.localllm.contracts.ConsumerControlPlaneClient
import io.github.daniele21.localllm.contracts.ConsumerControlPlaneErrorCode
import io.github.daniele21.localllm.contracts.ConsumerControlPlaneFailure
import io.github.daniele21.localllm.contracts.ConsumerDeactivationResult
import io.github.daniele21.localllm.contracts.ConsumerLocalLlmClient
import io.github.daniele21.localllm.contracts.ConsumerPublishedPresetsResult
import io.github.daniele21.localllm.contracts.InferencePresetRef
import io.github.daniele21.localllm.contracts.UseCaseId
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeException
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeFailureCode

internal val REDACTGUARD_DOCUMENT_PII_USE_CASE = UseCaseId("document-pii-detection")

/**
 * Connection-scoped RedactGuard activation over the Harness Consumer Control Plane.
 *
 * Only consumer-safe identities are retained. Concrete model/runtime configuration remains owned by
 * Harness and never crosses this boundary.
 */
internal class ConsumerControlPlaneActivation(
    private val controlPlane: ConsumerControlPlaneClient,
    val targetUseCaseId: UseCaseId = REDACTGUARD_DOCUMENT_PII_USE_CASE,
    private val selectedPreset: () -> InferencePresetRef? = { null },
) {
    private val lock = Any()
    private var active: ConsumerActivation? = null

    val activePreset: InferencePresetRef?
        get() = synchronized(lock) { active?.preset }

    fun ensureActivated(): ConsumerActivation = synchronized(lock) {
        active?.let { return it }

        val assignments =
            when (val result = controlPlane.assignedUseCases()) {
                is ConsumerAssignedUseCasesResult.Available -> result.assignments
                is ConsumerAssignedUseCasesResult.Rejected -> throw controlPlaneFailure(result.failure)
            }
        val assignment = assignments.singleOrNull { it.useCaseId == targetUseCaseId }
            ?: throw AnalysisRuntimeException(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)

        val published =
            when (val result = controlPlane.publishedPresets(targetUseCaseId)) {
                is ConsumerPublishedPresetsResult.Available -> result
                is ConsumerPublishedPresetsResult.Rejected -> throw controlPlaneFailure(result.failure)
            }
        if (published.useCaseId != targetUseCaseId || published.bindingRevision != assignment.bindingRevision) {
            throw AnalysisRuntimeException(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)
        }
        val presetRefs = published.presets.map { it.preset }
        if (presetRefs.isEmpty() || presetRefs.distinct().size != presetRefs.size) {
            throw AnalysisRuntimeException(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)
        }
        val requestedPreset = selectedPreset()
        val preset =
            if (requestedPreset != null) {
                requestedPreset.takeIf { it in presetRefs }
                    ?: throw AnalysisRuntimeException(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)
            } else {
                val defaults = published.presets.filter { it.isDefault }
                if (defaults.size != 1) {
                    throw AnalysisRuntimeException(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)
                }
                defaults.single().preset
            }
        val request =
            ConsumerActivationRequest(
                useCaseId = assignment.useCaseId,
                useCaseRevision = assignment.useCaseRevision,
                bindingRevision = assignment.bindingRevision,
                preset = preset,
            )
        val activation =
            when (val result = controlPlane.activate(request)) {
                is ConsumerActivationResult.Activated -> result.activation
                is ConsumerActivationResult.Rejected -> throw controlPlaneFailure(result.failure)
            }
        val valid =
            activation.useCaseId == request.useCaseId &&
                activation.useCaseRevision == request.useCaseRevision &&
                activation.bindingRevision == request.bindingRevision &&
                activation.preset == request.preset
        if (!valid) throw AnalysisRuntimeException(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)
        active = activation
        activation
    }

    /** Host/Binder death already releases Host residency; only local stale identity is discarded. */
    fun invalidate() {
        synchronized(lock) { active = null }
    }

    /** Explicit normal-lifecycle release. Connection close remains the fail-safe cleanup path. */
    fun deactivate() {
        val activation = synchronized(lock) { active.also { active = null } } ?: return
        when (val result = controlPlane.deactivate(activation.activationId)) {
            ConsumerDeactivationResult.Released -> Unit
            is ConsumerDeactivationResult.Rejected -> throw controlPlaneFailure(result.failure)
        }
    }

    private fun controlPlaneFailure(failure: ConsumerControlPlaneFailure): AnalysisRuntimeException =
        AnalysisRuntimeException(
            when (failure.code) {
                ConsumerControlPlaneErrorCode.MODEL_UNAVAILABLE,
                ConsumerControlPlaneErrorCode.MODEL_CONFLICT,
                ConsumerControlPlaneErrorCode.ACTIVATION_ALREADY_ACTIVE,
                ConsumerControlPlaneErrorCode.RUNTIME_FAILURE,
                -> AnalysisRuntimeFailureCode.HOST_UNAVAILABLE

                ConsumerControlPlaneErrorCode.TRANSPORT_FAILURE -> AnalysisRuntimeFailureCode.DISCONNECTED

                ConsumerControlPlaneErrorCode.FEATURE_UNAVAILABLE,
                ConsumerControlPlaneErrorCode.UNKNOWN_APPLICATION,
                ConsumerControlPlaneErrorCode.APPLICATION_NOT_AUTHORIZED,
                ConsumerControlPlaneErrorCode.USE_CASE_NOT_ASSIGNED,
                ConsumerControlPlaneErrorCode.PRESET_NOT_EXPOSED,
                ConsumerControlPlaneErrorCode.STALE_REVISION,
                ConsumerControlPlaneErrorCode.CONFIGURATION_REQUIRED,
                ConsumerControlPlaneErrorCode.INVALID_REQUEST,
                -> AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE
            },
        )
}

/** Adds explicit Harness activation without creating another Binder connection. */
internal class ActivationAwareConsumerLocalLlmClient(
    private val delegate: ConsumerLocalLlmClient,
    private val activation: ConsumerControlPlaneActivation,
) : ConsumerLocalLlmClient by delegate {
    override fun capabilities(useCaseId: UseCaseId) =
        if (useCaseId == activation.targetUseCaseId) {
            activation.ensureActivated()
            delegate.capabilities(useCaseId)
        } else {
            throw AnalysisRuntimeException(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)
        }
}
