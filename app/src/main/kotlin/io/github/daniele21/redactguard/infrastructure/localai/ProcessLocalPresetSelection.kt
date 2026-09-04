package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.contracts.ConsumerPublishedPreset
import io.github.daniele21.localllm.contracts.InferencePresetRef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-local owner of consumer-safe Harness preset metadata and current selection.
 *
 * This state is deliberately not persisted. It stores only Host-published display metadata and
 * opaque preset references, never concrete model/runtime configuration.
 */
internal class ProcessLocalPresetSelection {
    private val mutableState = MutableStateFlow(LocalAiPresetSelectionState())
    val state: StateFlow<LocalAiPresetSelectionState> = mutableState.asStateFlow()

    val selectedPreset: InferencePresetRef?
        get() = mutableState.value.selectedPreset

    @Synchronized
    fun select(preset: InferencePresetRef): Boolean {
        val current = mutableState.value
        if (current.options.none { it.preset == preset }) return false
        mutableState.value =
            current.copy(
                selectedPreset = preset,
                staleSelectionReplaced = false,
            )
        return true
    }

    /**
     * Resolves the same selection that [resolve] would use without mutating process-local state.
     *
     * This is used by Local AI setup inspection so merely opening/refreshing that surface cannot
     * change the selected preset. A later analysis activation can commit the same projection via
     * [resolve] after its fresh control-plane read.
     */
    @Synchronized
    fun preview(
        published: List<ConsumerPublishedPreset>,
        requestedPreset: InferencePresetRef?,
    ): LocalAiPresetSelectionState? = projectedState(published, requestedPreset)

    @Synchronized
    fun resolve(
        published: List<ConsumerPublishedPreset>,
        requestedPreset: InferencePresetRef?,
    ): InferencePresetRef? {
        val projected = projectedState(published, requestedPreset) ?: return null
        mutableState.value = projected
        return projected.selectedPreset
    }

    private fun projectedState(
        published: List<ConsumerPublishedPreset>,
        requestedPreset: InferencePresetRef?,
    ): LocalAiPresetSelectionState? {
        val options =
            published.map { preset ->
                LocalAiPresetOption(
                    preset = preset.preset,
                    displayName = preset.displayName,
                    description = preset.description,
                    isDefault = preset.isDefault,
                )
            }
        if (options.isEmpty()) return null

        val advertised = options.map(LocalAiPresetOption::preset)
        if (requestedPreset != null && requestedPreset !in advertised) return null

        val previous = mutableState.value.selectedPreset
        val staleSelectionReplaced = requestedPreset == null && previous != null && previous !in advertised
        val selected =
            when {
                options.size == 1 -> options.single().preset
                requestedPreset != null -> requestedPreset
                previous != null && previous in advertised -> previous
                else -> options.singleOrNull(LocalAiPresetOption::isDefault)?.preset
            } ?: return null

        return LocalAiPresetSelectionState(
            options = options,
            selectedPreset = selected,
            staleSelectionReplaced = staleSelectionReplaced,
        )
    }
}

internal data class LocalAiPresetSelectionState(
    val options: List<LocalAiPresetOption> = emptyList(),
    val selectedPreset: InferencePresetRef? = null,
    val staleSelectionReplaced: Boolean = false,
)

internal data class LocalAiPresetOption(
    val preset: InferencePresetRef,
    val displayName: String?,
    val description: String?,
    val isDefault: Boolean,
)
