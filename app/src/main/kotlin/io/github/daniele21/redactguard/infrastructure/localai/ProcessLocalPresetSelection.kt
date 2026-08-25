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

    @Synchronized
    fun resolve(
        published: List<ConsumerPublishedPreset>,
        requestedPreset: InferencePresetRef?,
    ): InferencePresetRef? {
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
        val previous = requestedPreset ?: mutableState.value.selectedPreset
        val staleSelectionReplaced = previous != null && previous !in advertised
        val selected =
            when {
                options.size == 1 -> options.single().preset
                previous != null && previous in advertised -> previous
                else -> options.singleOrNull(LocalAiPresetOption::isDefault)?.preset
            } ?: return null

        mutableState.value =
            LocalAiPresetSelectionState(
                options = options,
                selectedPreset = selected,
                staleSelectionReplaced = staleSelectionReplaced,
            )
        return selected
    }
}

internal data class LocalAiPresetSelectionState(
    val options: List<LocalAiPresetOption> = emptyList(),
    val selectedPreset: InferencePresetRef? = null,
    val staleSelectionReplaced: Boolean = false,
)

internal data class LocalAiPresetOption(
    val preset: InferencePresetRef,
    val displayName: String,
    val description: String,
    val isDefault: Boolean,
)
