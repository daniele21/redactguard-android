package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.contracts.ConsumerResolvedSetup
import io.github.daniele21.localllm.contracts.InferencePresetRef
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeDiagnostic
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeException
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeFailureCode
import io.github.daniele21.redactguard.domain.analysis.LocalAiExecutionPhase
import io.github.daniele21.redactguard.domain.analysis.LocalAiExecutionState
import io.github.daniele21.redactguard.domain.failure.FailureRecoveryAction
import io.github.daniele21.redactguard.domain.failure.ProductFailureKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class LocalAiSetupStage {
    DISCONNECTED,
    CONNECTED,
    CONFIGURED,
    COMPATIBLE,
}

/** Consumer-safe setup projection. Resolved configuration is always copied from Harnex evidence. */
internal data class LocalAiSetupState(
    val stage: LocalAiSetupStage = LocalAiSetupStage.DISCONNECTED,
    val selectedPreset: InferencePresetRef? = null,
    val resolvedSetup: ConsumerResolvedSetup? = null,
    val runtimeReady: Boolean = false,
    val problem: ProductFailureKind? = null,
    val technicalIdentity: AnalysisRuntimeDiagnostic? = null,
) {
    val connected: Boolean
        get() = stage != LocalAiSetupStage.DISCONNECTED

    val configured: Boolean
        get() = connected && selectedPreset != null

    val compatible: Boolean
        get() = stage == LocalAiSetupStage.COMPATIBLE

    val recoveryAction: FailureRecoveryAction?
        get() = problem?.recoveryAction
}

/** Single owner for passive setup and explicit-operation readiness projection. */
internal class LocalAiSetupStateProjection {
    private val mutableState = MutableStateFlow(LocalAiSetupState())

    val state: StateFlow<LocalAiSetupState> = mutableState.asStateFlow()

    fun onTransportConnected() {
        mutableState.value = LocalAiSetupState(stage = LocalAiSetupStage.CONNECTED)
    }

    fun onPresetSelected(preset: InferencePresetRef) {
        val current = mutableState.value
        if (!current.connected) return
        mutableState.value =
            current.copy(
                stage = LocalAiSetupStage.CONFIGURED,
                selectedPreset = preset,
                resolvedSetup = null,
                runtimeReady = false,
                problem = null,
                technicalIdentity = null,
            )
    }

    fun onSetupResolved(inspection: ConsumerControlPlaneSetupInspection) {
        if (!mutableState.value.connected) return
        mutableState.value =
            LocalAiSetupState(
                stage = LocalAiSetupStage.COMPATIBLE,
                selectedPreset = inspection.selectedPreset.preset,
                resolvedSetup = inspection.resolvedSetup,
            )
    }

    fun onSetupFailure(failure: AnalysisRuntimeException?) {
        val current = mutableState.value
        val problem = failure?.code?.toSetupProblem() ?: ProductFailureKind.LOCAL_AI_SETUP_UNEXPECTED
        if (!current.connected) {
            mutableState.value =
                LocalAiSetupState(
                    problem = problem,
                    technicalIdentity = failure?.diagnostic,
                )
            return
        }
        mutableState.value =
            current.copy(
                stage = if (current.selectedPreset != null) LocalAiSetupStage.CONFIGURED else LocalAiSetupStage.CONNECTED,
                resolvedSetup = null,
                runtimeReady = false,
                problem = problem,
                technicalIdentity = failure?.diagnostic,
            )
    }

    fun onExecutionState(state: LocalAiExecutionState) {
        val current = mutableState.value
        if (!current.compatible) return
        if (state.phase == LocalAiExecutionPhase.FAILED && state.failureCode == AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE) {
            mutableState.value =
                current.copy(
                    stage = LocalAiSetupStage.CONFIGURED,
                    resolvedSetup = null,
                    runtimeReady = false,
                    problem = ProductFailureKind.CAPABILITY_INCOMPATIBLE,
                    technicalIdentity = null,
                )
            return
        }
        val ready = state.phase == LocalAiExecutionPhase.READY || state.phase == LocalAiExecutionPhase.GENERATING
        mutableState.value =
            current.copy(
                runtimeReady = ready,
                problem = state.failureCode?.toSetupProblem(),
                technicalIdentity = null,
            )
    }

    fun onTransportDisconnected() {
        mutableState.value = LocalAiSetupState()
    }
}

private fun AnalysisRuntimeFailureCode.toSetupProblem(): ProductFailureKind =
    when (this) {
        AnalysisRuntimeFailureCode.HOST_UNAVAILABLE -> ProductFailureKind.HOST_UNAVAILABLE
        AnalysisRuntimeFailureCode.CONFIGURATION_REQUIRED -> ProductFailureKind.LOCAL_AI_CONFIGURATION_REQUIRED
        AnalysisRuntimeFailureCode.MODEL_UNAVAILABLE -> ProductFailureKind.LOCAL_AI_MODEL_UNAVAILABLE
        AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE -> ProductFailureKind.CAPABILITY_INCOMPATIBLE
        AnalysisRuntimeFailureCode.INVALID_REQUEST -> ProductFailureKind.LOCAL_AI_INVALID_REQUEST
        AnalysisRuntimeFailureCode.GENERATION_FAILED -> ProductFailureKind.LOCAL_AI_RUNTIME_UNAVAILABLE
        AnalysisRuntimeFailureCode.DISCONNECTED -> ProductFailureKind.DISCONNECTED
        AnalysisRuntimeFailureCode.HOST_PROCESS_LOST -> ProductFailureKind.HOST_PROCESS_LOST
        AnalysisRuntimeFailureCode.CANCELLED -> ProductFailureKind.CANCELLED
        AnalysisRuntimeFailureCode.INTERNAL_FAILURE -> ProductFailureKind.LOCAL_AI_SETUP_UNEXPECTED
    }
