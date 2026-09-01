package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.contracts.ConsumerResolvedSetup
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeFailureCode
import io.github.daniele21.redactguard.domain.analysis.LocalAiExecutionPhase
import io.github.daniele21.redactguard.domain.analysis.LocalAiExecutionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class LocalAiSetupStage {
    DISCONNECTED,
    CONNECTED,
    COMPATIBLE,
    RUNTIME_READY,
}

/** Consumer-safe setup projection. Resolved configuration is always copied from Harnex evidence. */
internal data class LocalAiSetupState(
    val stage: LocalAiSetupStage = LocalAiSetupStage.DISCONNECTED,
    val resolvedSetup: ConsumerResolvedSetup? = null,
    val failureCode: AnalysisRuntimeFailureCode? = null,
) {
    val connected: Boolean
        get() = stage != LocalAiSetupStage.DISCONNECTED

    val configured: Boolean
        get() = resolvedSetup != null

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

    fun onSetupResolved(inspection: ConsumerControlPlaneSetupInspection) {
        mutableState.value =
            LocalAiSetupState(
                stage = LocalAiSetupStage.COMPATIBLE,
                resolvedSetup = inspection.resolvedSetup,
            )
    }

    fun onSetupFailure(code: AnalysisRuntimeFailureCode?) {
        val current = mutableState.value
        mutableState.value =
            current.copy(
                stage = if (current.connected) LocalAiSetupStage.CONNECTED else LocalAiSetupStage.DISCONNECTED,
                resolvedSetup = null,
                failureCode = code,
            )
    }

    fun onExecutionState(state: LocalAiExecutionState) {
        val current = mutableState.value
        if (!current.compatible && current.stage != LocalAiSetupStage.RUNTIME_READY) return
        val ready = state.phase == LocalAiExecutionPhase.READY || state.phase == LocalAiExecutionPhase.GENERATING
        mutableState.value = current.copy(stage = if (ready) LocalAiSetupStage.RUNTIME_READY else LocalAiSetupStage.COMPATIBLE)
    }

    fun onTransportDisconnected() {
        mutableState.value = LocalAiSetupState()
    }
}
