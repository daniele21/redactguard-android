package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.redactguard.domain.analysis.AnalysisOperationId
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimePort
import io.github.daniele21.redactguard.domain.analysis.LocalAiExecutionState
import io.github.daniele21.redactguard.domain.analysis.LocalAiRuntimeState
import kotlinx.coroutines.flow.StateFlow

/**
 * App-internal runtime surface consumed by the product coordinator.
 *
 * Production is owned by [BinderAnalysisRuntimeComposition]. This seam lets Android instrumentation
 * exercise product journeys deterministically without a shipped test mode or fake Host.
 */
internal interface ProductAnalysisRuntime :
    AnalysisRuntimePort,
    AutoCloseable {
    val connectionState: LocalAiRuntimeState
    val presetSelectionState: StateFlow<LocalAiPresetSelectionState>

    fun selectPresetAt(index: Int): Boolean

    fun refreshPresetSelection()

    fun connect()
}

internal fun interface ProductAnalysisRuntimeFactory {
    fun create(
        onStateChanged: (LocalAiRuntimeState) -> Unit,
        onExecutionStateChanged: (AnalysisOperationId, LocalAiExecutionState) -> Unit,
    ): ProductAnalysisRuntime
}
