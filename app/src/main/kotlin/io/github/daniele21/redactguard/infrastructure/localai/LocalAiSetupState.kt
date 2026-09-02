package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.contracts.ConsumerResolvedSetup
import io.github.daniele21.localllm.contracts.InferencePresetRef
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeDiagnostic
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeException
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeFailureCode
import io.github.daniele21.redactguard.domain.analysis.LocalAiExecutionPhase
import io.github.daniele21.redactguard.domain.analysis.LocalAiExecutionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class LocalAiSetupStage {
    DISCONNECTED,
    CONNECTED,
    CONFIGURED,
    COMPATIBLE,
}

internal enum class LocalAiSetupProblem {
    HOST_UNAVAILABLE,
    CONFIGURATION_REQUIRED,
    MODEL_UNAVAILABLE,
    INCOMPATIBLE,
    TRANSIENT_RUNTIME,
    UNEXPECTED,
}

internal enum class LocalAiSetupRecovery {
    RECONNECT,
    REVIEW_CONFIGURATION,
    MAKE_MODEL_AVAILABLE,
    UPDATE_LOCAL_AI,
    RETRY,
}

/** Consumer-safe setup projection. Resolved configuration is always copied from Harnex evidence. */
internal data class LocalAiSetupState(
    val stage: LocalAiSetupStage = LocalAiSetupStage.DISCONNECTED,
    val selectedPreset: InferencePresetRef? = null,
    val resolvedSetup: ConsumerResolvedSetup? = null,
    val runtimeReady: Boolean = false,
    val problem: LocalAiSetupProblem? = null,
    val recovery: LocalAiSetupRecovery? = null,
    val technicalIdentity: AnalysisRuntimeDiagnostic? = null,
) {
    val connected: Boolean
        get() = stage != LocalAiSetupStage.DISCONNECTED

    val configured: Boolean
        get() = connected && selectedPreset != null

    val compatible: Boolean
        get() = stage == LocalAiSetupStage.COMPATIBLE
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
                recovery = null,
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
        val classification = failure?.toSetupClassification() ?: SetupClassification.unexpected()
        if (!current.connected) {
            mutableState.value =
                LocalAiSetupState(
                    problem = classification.problem,
                    recovery = classification.recovery,
                    technicalIdentity = failure?.diagnostic,
                )
            return
        }
        mutableState.value =
            current.copy(
                stage = if (current.selectedPreset != null) LocalAiSetupStage.CONFIGURED else LocalAiSetupStage.CONNECTED,
                resolvedSetup = null,
                runtimeReady = false,
                problem = classification.problem,
                recovery = classification.recovery,
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
                    problem = LocalAiSetupProblem.INCOMPATIBLE,
                    recovery = LocalAiSetupRecovery.UPDATE_LOCAL_AI,
                    technicalIdentity = null,
                )
            return
        }
        val ready = state.phase == LocalAiExecutionPhase.READY || state.phase == LocalAiExecutionPhase.GENERATING
        mutableState.value =
            current.copy(
                runtimeReady = ready,
                problem = state.failureCode?.toSetupClassification()?.problem,
                recovery = state.failureCode?.toSetupClassification()?.recovery,
                technicalIdentity = null,
            )
    }

    fun onTransportDisconnected() {
        mutableState.value = LocalAiSetupState()
    }
}

private data class SetupClassification(
    val problem: LocalAiSetupProblem,
    val recovery: LocalAiSetupRecovery,
) {
    companion object {
        fun unexpected() = SetupClassification(LocalAiSetupProblem.UNEXPECTED, LocalAiSetupRecovery.RETRY)
    }
}

private fun AnalysisRuntimeException.toSetupClassification(): SetupClassification = code.toSetupClassification()

private fun AnalysisRuntimeFailureCode.toSetupClassification(): SetupClassification =
    when (this) {
        AnalysisRuntimeFailureCode.HOST_UNAVAILABLE,
        AnalysisRuntimeFailureCode.DISCONNECTED,
        AnalysisRuntimeFailureCode.HOST_PROCESS_LOST,
        -> SetupClassification(LocalAiSetupProblem.HOST_UNAVAILABLE, LocalAiSetupRecovery.RECONNECT)

        AnalysisRuntimeFailureCode.CONFIGURATION_REQUIRED ->
            SetupClassification(LocalAiSetupProblem.CONFIGURATION_REQUIRED, LocalAiSetupRecovery.REVIEW_CONFIGURATION)

        AnalysisRuntimeFailureCode.MODEL_UNAVAILABLE ->
            SetupClassification(LocalAiSetupProblem.MODEL_UNAVAILABLE, LocalAiSetupRecovery.MAKE_MODEL_AVAILABLE)

        AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE ->
            SetupClassification(LocalAiSetupProblem.INCOMPATIBLE, LocalAiSetupRecovery.UPDATE_LOCAL_AI)

        AnalysisRuntimeFailureCode.GENERATION_FAILED,
        AnalysisRuntimeFailureCode.CANCELLED,
        -> SetupClassification(LocalAiSetupProblem.TRANSIENT_RUNTIME, LocalAiSetupRecovery.RETRY)

        AnalysisRuntimeFailureCode.INVALID_REQUEST,
        AnalysisRuntimeFailureCode.INTERNAL_FAILURE,
        -> SetupClassification(LocalAiSetupProblem.UNEXPECTED, LocalAiSetupRecovery.RETRY)
    }
