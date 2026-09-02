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
import io.github.daniele21.localllm.contracts.ConsumerResolvedSetup
import io.github.daniele21.localllm.contracts.ConsumerSetupResolutionRequest
import io.github.daniele21.localllm.contracts.ConsumerSetupResolutionResult
import io.github.daniele21.localllm.contracts.InferencePresetRef
import io.github.daniele21.localllm.contracts.UseCaseId
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeDiagnostic
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
    private val technicalDiagnostics: LocalAiTechnicalDiagnostics = NoopLocalAiTechnicalDiagnostics,
) {
    /**
     * Reads the consumer-safe assignment, preset and Host-resolved execution setup without activation
     * or runtime preparation. The selected preset is previewed rather than committed.
     */
    fun inspectSetup(requestedPreset: InferencePresetRef? = null): ConsumerControlPlaneSetupInspection {
        val assignment = discoverAssignment()
        val published = discoverPresets(assignment)
        val projectedSelection =
            presetSelection.preview(published, requestedPreset)
                ?: throw incompatible(STEP_PRESET_SELECTION, "PRESET_SELECTION_UNAVAILABLE")
        val selectedPreset =
            published.singleOrNull { it.preset == projectedSelection.selectedPreset }
                ?: throw incompatible(STEP_PRESET_SELECTION, "PRESET_SELECTION_IDENTITY_MISMATCH")
        val resolutionRequest =
            ConsumerSetupResolutionRequest(
                useCaseId = assignment.useCaseId,
                useCaseRevision = assignment.useCaseRevision,
                bindingRevision = assignment.bindingRevision,
                preset = selectedPreset.preset,
            )
        val resolvedSetup =
            when (val result = observedBoundary(STEP_SETUP_RESOLUTION) { client.resolveSetup(resolutionRequest) }) {
                is ConsumerSetupResolutionResult.Resolved -> {
                    record(STEP_SETUP_RESOLUTION, "RESOLVED")
                    result.setup
                }

                is ConsumerSetupResolutionResult.Rejected -> {
                    record(STEP_SETUP_RESOLUTION, "REJECTED", result.failure.code.name)
                    throw result.failure.toAnalysisRuntimeException(STEP_SETUP_RESOLUTION, transportConnected)
                }
            }
        if (!resolvedSetup.matches(resolutionRequest)) {
            throw incompatible(STEP_SETUP_RESOLUTION, "SETUP_IDENTITY_MISMATCH")
        }
        record(STEP_SETUP_INSPECTION, "READY")
        return ConsumerControlPlaneSetupInspection(
            assignment = assignment,
            selectedPreset = selectedPreset,
            availablePresets = published,
            resolvedSetup = resolvedSetup,
            staleSelectionWouldBeReplaced = projectedSelection.staleSelectionReplaced,
        )
    }

    fun refreshPresetSelection(requestedPreset: InferencePresetRef? = null): InferencePresetRef {
        val assignment = discoverAssignment()
        val published = discoverPresets(assignment)
        return presetSelection.resolve(published, requestedPreset)
            ?: throw incompatible(STEP_PRESET_SELECTION, "PRESET_SELECTION_UNAVAILABLE")
    }

    fun activate(requestedPreset: InferencePresetRef? = null): AnalysisActivation {
        val assignment = discoverAssignment()
        val published = discoverPresets(assignment)
        val preset =
            presetSelection.resolve(published, requestedPreset)
                ?: throw incompatible(STEP_PRESET_SELECTION, "PRESET_SELECTION_UNAVAILABLE")
        return activateRequest(
            ConsumerActivationRequest(
                useCaseId = useCaseId,
                useCaseRevision = assignment.useCaseRevision,
                bindingRevision = assignment.bindingRevision,
                preset = preset,
            ),
        )
    }

    /** Activates exactly the immutable setup identity that passed the immediately preceding preflight. */
    fun activate(inspection: ConsumerControlPlaneSetupInspection): AnalysisActivation =
        activateRequest(
            ConsumerActivationRequest(
                useCaseId = inspection.resolvedSetup.useCaseId,
                useCaseRevision = inspection.resolvedSetup.useCaseRevision,
                bindingRevision = inspection.resolvedSetup.bindingRevision,
                preset = inspection.resolvedSetup.preset,
            ),
        )

    fun deactivate(activationId: ConsumerActivationId) {
        when (val result = observedBoundary(STEP_DEACTIVATE) { client.deactivate(activationId) }) {
            ConsumerDeactivationResult.Released -> {
                record(STEP_DEACTIVATE, "RELEASED")
            }

            is ConsumerDeactivationResult.Rejected -> {
                record(STEP_DEACTIVATE, "REJECTED", result.failure.code.name)
                if (result.failure.code == ConsumerControlPlaneErrorCode.TRANSPORT_FAILURE && !transportConnected()) return
                throw result.failure.toAnalysisRuntimeException(STEP_DEACTIVATE, transportConnected)
            }
        }
    }

    fun deactivateBestEffort(activationId: ConsumerActivationId) {
        runCatching { deactivate(activationId) }
    }

    private fun activateRequest(request: ConsumerActivationRequest): AnalysisActivation {
        record(STEP_ACTIVATION_REQUEST, "READY")
        val activation =
            when (val result = observedBoundary(STEP_ACTIVATE) { client.activate(request) }) {
                is ConsumerActivationResult.Activated -> {
                    record(STEP_ACTIVATE, "ACTIVATED")
                    result.activation
                }

                is ConsumerActivationResult.Rejected -> {
                    record(STEP_ACTIVATE, "REJECTED", result.failure.code.name)
                    throw result.failure.toAnalysisRuntimeException(STEP_ACTIVATE, transportConnected)
                }
            }
        if (!activationMatches(activation, request)) {
            record(STEP_ACTIVATE, "INCOMPATIBLE", "ACTIVATION_IDENTITY_MISMATCH")
            runCatching { client.deactivate(activation.activationId) }
            throw runtimeFailure(
                code = AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE,
                step = STEP_ACTIVATE,
                type = "ACTIVATION_IDENTITY_MISMATCH",
            )
        }
        return AnalysisActivation(activation.activationId, request.preset)
    }

    private fun discoverAssignment(): ConsumerAssignedUseCase {
        val assignments =
            when (val result = observedBoundary(STEP_ASSIGNED_USE_CASES) { client.assignedUseCases() }) {
                is ConsumerAssignedUseCasesResult.Available -> {
                    record(STEP_ASSIGNED_USE_CASES, "AVAILABLE", count = result.assignments.size)
                    result.assignments
                }

                is ConsumerAssignedUseCasesResult.Rejected -> {
                    record(STEP_ASSIGNED_USE_CASES, "REJECTED", result.failure.code.name)
                    throw result.failure.toAnalysisRuntimeException(STEP_ASSIGNED_USE_CASES, transportConnected)
                }
            }
        val matches = assignments.filter { it.useCaseId == useCaseId }
        if (matches.size != 1) {
            record(STEP_ASSIGNED_USE_CASES, "INCOMPATIBLE", "ASSIGNMENT_MATCH_COUNT", matches.size)
            throw runtimeFailure(
                code = AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE,
                step = STEP_ASSIGNED_USE_CASES,
                type = "ASSIGNMENT_MATCH_COUNT",
            )
        }
        record(STEP_ASSIGNED_USE_CASES, "MATCHED", count = 1)
        return matches.single()
    }

    private fun discoverPresets(assignment: ConsumerAssignedUseCase): List<ConsumerPublishedPreset> {
        val result =
            when (val published = observedBoundary(STEP_PUBLISHED_PRESETS) { client.publishedPresets(useCaseId) }) {
                is ConsumerPublishedPresetsResult.Available -> {
                    record(STEP_PUBLISHED_PRESETS, "AVAILABLE", count = published.presets.size)
                    published
                }

                is ConsumerPublishedPresetsResult.Rejected -> {
                    record(STEP_PUBLISHED_PRESETS, "REJECTED", published.failure.code.name)
                    throw published.failure.toAnalysisRuntimeException(STEP_PUBLISHED_PRESETS, transportConnected)
                }
            }
        if (result.useCaseId != useCaseId || result.bindingRevision != assignment.bindingRevision) {
            record(STEP_PUBLISHED_PRESETS, "INCOMPATIBLE", "BINDING_IDENTITY_MISMATCH")
            throw runtimeFailure(
                code = AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE,
                step = STEP_PUBLISHED_PRESETS,
                type = "BINDING_IDENTITY_MISMATCH",
            )
        }
        val identities = result.presets.map(ConsumerPublishedPreset::preset)
        if (identities.isEmpty()) {
            record(STEP_PUBLISHED_PRESETS, "INCOMPATIBLE", "NO_PUBLISHED_PRESETS")
            throw runtimeFailure(
                code = AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE,
                step = STEP_PUBLISHED_PRESETS,
                type = "NO_PUBLISHED_PRESETS",
            )
        }
        if (identities.distinct().size != identities.size) {
            record(STEP_PUBLISHED_PRESETS, "INCOMPATIBLE", "DUPLICATE_PRESET_IDENTITY")
            throw runtimeFailure(
                code = AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE,
                step = STEP_PUBLISHED_PRESETS,
                type = "DUPLICATE_PRESET_IDENTITY",
            )
        }
        record(STEP_PUBLISHED_PRESETS, "VALIDATED", count = identities.size)
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

    private inline fun <T> observedBoundary(
        step: String,
        block: () -> T,
    ): T =
        try {
            localAiBoundary(step, block)
        } catch (failure: AnalysisRuntimeException) {
            record(step, "FAILED", failure.diagnostic?.type ?: failure.code.name)
            throw failure
        }

    private fun incompatible(
        step: String,
        type: String,
    ): AnalysisRuntimeException {
        record(step, "INCOMPATIBLE", type)
        return runtimeFailure(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE, step, type)
    }

    private fun record(
        step: String,
        result: String,
        reason: String? = null,
        count: Int? = null,
    ) {
        runCatching {
            technicalDiagnostics.record(LocalAiTechnicalEvent(step = step, result = result, reason = reason, count = count))
        }
    }

    private companion object {
        val DOCUMENT_PII_USE_CASE = UseCaseId("document-pii-detection")
        const val STEP_ASSIGNED_USE_CASES = "control-plane.assigned-use-cases"
        const val STEP_PUBLISHED_PRESETS = "control-plane.published-presets"
        const val STEP_PRESET_SELECTION = "control-plane.preset-selection"
        const val STEP_SETUP_RESOLUTION = "control-plane.setup-resolution"
        const val STEP_SETUP_INSPECTION = "control-plane.setup-inspection"
        const val STEP_ACTIVATION_REQUEST = "control-plane.activation-request"
        const val STEP_ACTIVATE = "control-plane.activate"
        const val STEP_DEACTIVATE = "control-plane.deactivate"
    }
}

internal data class ConsumerControlPlaneSetupInspection(
    val assignment: ConsumerAssignedUseCase,
    val selectedPreset: ConsumerPublishedPreset,
    val availablePresets: List<ConsumerPublishedPreset>,
    val resolvedSetup: ConsumerResolvedSetup,
    val staleSelectionWouldBeReplaced: Boolean,
)

internal data class AnalysisActivation(
    val activationId: ConsumerActivationId,
    val preset: InferencePresetRef,
)

private fun ConsumerResolvedSetup.matches(request: ConsumerSetupResolutionRequest): Boolean =
    useCaseId == request.useCaseId &&
        useCaseRevision == request.useCaseRevision &&
        bindingRevision == request.bindingRevision &&
        preset == request.preset

private fun runtimeFailure(
    code: AnalysisRuntimeFailureCode,
    step: String? = null,
    type: String? = null,
): AnalysisRuntimeException =
    AnalysisRuntimeException(
        code = code,
        diagnostic =
            if (step != null && type != null) {
                AnalysisRuntimeDiagnostic(step = step, type = type)
            } else {
                null
            },
    )
