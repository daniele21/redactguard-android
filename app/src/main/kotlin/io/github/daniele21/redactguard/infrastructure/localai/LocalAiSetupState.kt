package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.contracts.ConsumerResolvedSetup
import io.github.daniele21.localllm.contracts.InferencePresetRef
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
    RUNTIME_READY,
}

/** Consumer-safe setup projection. Resolved configuration is always copied from Harnex evidence. */
internal data class LocalAiSetupState(
    val stage: LocalAiSetupStage = LocalAiSetupStage.DISCONNECTED,
    val selectedPreset: InferencePresetRef? = null,
    val resolvedSetup: ConsumerResolvedSetup? = null,
    val failureCode: AnalysisRuntimeFailureCode? = null,
) {
    val connected: Boolean
        get() = stage != LocalAiSetupStage.DISCONNECTED

    val configured: Boolean
        get() = connected && selectedPreset != null

    val compatible: Boolean
        get() = stage == LocalAiSetupStage.COMPATIBLE || stage == LocalAiSetupStage.RUNTIME_READY

    val runtimeReady: Boolean
        get() = stage == LocalAiSetupStage.RUNTIME_READY
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
                failureCode = null,
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

    fun onSetupFailure(code: AnalysisRuntimeFailureCode?) {
        val current = mutableState.value
        if (!current.connected) {
            mutableState.value = LocalAiSetupState(failureCode = code)
            return
        }
        mutableState.value =
            current.copy(
                stage = if (current.selectedPreset != null) LocalAiSetupStage.CONFIGURED else LocalAiSetupStage.CONNECTED,
                resolvedSetup = null,
                failureCode = code,
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
                    failureCode = state.failureCode,
                )
            return
        }
        val ready = state.phase == LocalAiExecutionPhase.READY || state.phase == LocalAiExecutionPhase.GENERATING
        mutableState.value =
            current.copy(
                stage = if (ready) LocalAiSetupStage.RUNTIME_READY else LocalAiSetupStage.COMPATIBLE,
                failureCode = state.failureCode,
            )
    }

    fun onTransportDisconnected() {
        mutableState.value = LocalAiSetupState()
    }
}
