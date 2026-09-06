package io.github.daniele21.redactguard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.github.daniele21.redactguard.domain.analysis.AnalysisPromptPolicy
import io.github.daniele21.redactguard.domain.analysis.AnalysisPromptValidation
import kotlinx.coroutines.flow.StateFlow

/** Thin Settings controller over the process-local product owner; prompt contents are never logged. */
internal class AnalysisPromptSettingsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val owner = ProcessLocalProductAnalysisOwner.get(application)

    val state: StateFlow<AnalysisPromptPreferenceState> = owner.analysisPrompt

    val defaultPrompt: String
        get() = AnalysisPromptPolicy.defaultEditablePrompt

    val protectedRules: String
        get() = AnalysisPromptPolicy.protectedRules

    val maxCharacters: Int
        get() = AnalysisPromptPolicy.MAX_EDITABLE_CHARACTERS

    fun validate(value: String): AnalysisPromptValidation = AnalysisPromptPolicy.validate(value)

    fun save(value: String): Boolean = owner.setAnalysisPrompt(value)

    fun reset() = owner.resetAnalysisPrompt()
}
