package io.github.daniele21.redactguard

import android.content.Context
import io.github.daniele21.redactguard.domain.analysis.AnalysisPromptPolicy

internal data class AnalysisPromptPreferenceState(
    val prompt: String,
    val isCustom: Boolean,
) {
    override fun toString(): String = "AnalysisPromptPreferenceState(isCustom=$isCustom, prompt=<redacted>)"
}

/**
 * App-private durable RedactGuard analysis guidance.
 *
 * A custom prompt may contain user-authored sensitive text, so its contents are never logged. Only a
 * custom value is stored: absence means "use the current built-in default", allowing future default
 * improvements to reach users who never customized the prompt.
 */
internal class AnalysisPromptPreferenceStore(
    context: Context,
) {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(): AnalysisPromptPreferenceState {
        val raw = preferences.getString(KEY_CUSTOM_PROMPT, null) ?: return defaultState()
        val validation = AnalysisPromptPolicy.validate(raw)
        if (!validation.isValid || validation.normalized == AnalysisPromptPolicy.defaultEditablePrompt) {
            preferences.edit().remove(KEY_CUSTOM_PROMPT).apply()
            return defaultState()
        }
        return AnalysisPromptPreferenceState(validation.normalized, isCustom = true)
    }

    fun write(value: String): AnalysisPromptPreferenceState? {
        val validation = AnalysisPromptPolicy.validate(value)
        if (!validation.isValid) return null
        if (validation.normalized == AnalysisPromptPolicy.defaultEditablePrompt) {
            preferences.edit().remove(KEY_CUSTOM_PROMPT).apply()
            return defaultState()
        }
        preferences.edit().putString(KEY_CUSTOM_PROMPT, validation.normalized).apply()
        return AnalysisPromptPreferenceState(validation.normalized, isCustom = true)
    }

    fun reset(): AnalysisPromptPreferenceState {
        preferences.edit().remove(KEY_CUSTOM_PROMPT).apply()
        return defaultState()
    }

    private fun defaultState(): AnalysisPromptPreferenceState =
        AnalysisPromptPreferenceState(AnalysisPromptPolicy.defaultEditablePrompt, isCustom = false)

    private companion object {
        const val PREFERENCES_NAME = "redactguard_analysis_prompt"
        const val KEY_CUSTOM_PROMPT = "custom_prompt"
    }
}
