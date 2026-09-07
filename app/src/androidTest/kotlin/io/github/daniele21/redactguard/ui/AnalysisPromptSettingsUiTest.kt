package io.github.daniele21.redactguard.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import io.github.daniele21.redactguard.domain.analysis.AnalysisPromptPolicy
import io.github.daniele21.redactguard.domain.analysis.LocalAiRuntimeState
import io.github.daniele21.redactguard.ui.theme.RedactGuardTheme
import org.junit.Rule
import org.junit.Test

class AnalysisPromptSettingsUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun defaultPromptCanBeCustomizedAndRestoredWithoutWeakeningProtectedRules() {
        composeRule.setContent {
            var prompt by remember { mutableStateOf(AnalysisPromptPolicy.defaultEditablePrompt) }
            var custom by remember { mutableStateOf(false) }
            RedactGuardTheme {
                RedactGuardSettingsScreen(
                    harnex =
                        HarnexConnectionSettingsProjector.project(
                            connectionEnabled = true,
                            state = LocalAiRuntimeState.CONNECTED,
                            analysisActive = false,
                        ),
                    analysisPrompt =
                        AnalysisPromptSettingsUiModel(
                            currentPrompt = prompt,
                            isCustom = custom,
                            defaultPrompt = AnalysisPromptPolicy.defaultEditablePrompt,
                            protectedRules = AnalysisPromptPolicy.protectedRules,
                            maxCharacters = AnalysisPromptPolicy.MAX_EDITABLE_CHARACTERS,
                        ),
                    onValidateAnalysisPrompt = AnalysisPromptPolicy::validate,
                    onSaveAnalysisPrompt = { value ->
                        val validation = AnalysisPromptPolicy.validate(value)
                        if (validation.isValid) {
                            prompt = validation.normalized
                            custom = prompt != AnalysisPromptPolicy.defaultEditablePrompt
                        }
                        validation.isValid
                    },
                    onConnectHarnex = {},
                    onDisconnectHarnex = {},
                    onRetryHarnex = {},
                    onOpenHarnex = {},
                )
            }
        }

        composeRule.onNodeWithText("Predefinito").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-analysis-prompt-edit").performScrollTo().performClick()
        composeRule.onNodeWithText("Prompt di analisi").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-analysis-prompt-editor").performTextReplacement("Extract only explicit occurrences.")
        composeRule.onNodeWithTag("settings-analysis-prompt-save").performClick()

        composeRule.onNodeWithText("Personalizzato").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-analysis-prompt-edit").performScrollTo().performClick()
        composeRule.onNodeWithTag("settings-analysis-prompt-reset").performScrollTo().performClick()
        composeRule.onNodeWithTag("settings-analysis-prompt-save").performClick()

        composeRule.onNodeWithText("Predefinito").assertIsDisplayed()
    }
}
