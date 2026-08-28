package io.github.daniele21.redactguard.ui

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.daniele21.redactguard.ui.theme.RedactGuardTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProtectionSelectionInstrumentationTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun partialFamilySelectionTogglesOnlyMissingCanonicalDefinitions() {
        val toggled = mutableListOf<String>()
        composeRule.setContent {
            RedactGuardTheme {
                DefinitionSelectionScreen(
                    connection = connectedBadge(),
                    choices =
                        listOf(
                            DefinitionChoice(id = "email", label = "Email", selected = true),
                            DefinitionChoice(id = "telephone", label = "Telefono", selected = false),
                        ),
                    onToggle = toggled::add,
                    onAddCustom = {},
                    onAnalyze = {},
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Contatti, parzialmente inclusa")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(listOf("telephone"), toggled)
        }
    }

    @Test
    fun advancedCustomizationStaysHiddenUntilExplicitDisclosure() {
        composeRule.setContent {
            RedactGuardTheme {
                DefinitionSelectionScreen(
                    connection = connectedBadge(),
                    choices = listOf(DefinitionChoice(id = "email", label = "Email", selected = true)),
                    presets =
                        listOf(
                            LocalAiPresetChoice(
                                id = "preset-0",
                                label = "Bilanciata",
                                description = "Opzione consigliata",
                                selected = true,
                            ),
                            LocalAiPresetChoice(
                                id = "preset-1",
                                label = "Accurata",
                                description = "Più attenzione alla qualità",
                                selected = false,
                            ),
                        ),
                    onToggle = {},
                    onAddCustom = {},
                    onAnalyze = {},
                )
            }
        }

        assertEquals(0, composeRule.onAllNodesWithText("Aggiungi categoria personalizzata").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Modalità di analisi").fetchSemanticsNodes().size)

        composeRule
            .onNodeWithText("Personalizza categorie e analisi")
            .assertHasClickAction()
            .performClick()

        composeRule.onNodeWithText("Aggiungi categoria personalizzata").assertIsDisplayed()
        composeRule.onNodeWithText("Modalità di analisi").assertIsDisplayed()
    }

    @Test
    fun recommendedProfilesExposeNonColorSelectionSemantics() {
        composeRule.setContent {
            RedactGuardTheme {
                DefinitionSelectionScreen(
                    connection = connectedBadge(),
                    choices = listOf(DefinitionChoice(id = "full-name", label = "Nome completo", selected = true)),
                    profiles =
                        listOf(
                            ProtectionProfileChoice(
                                id = "GENERAL",
                                label = "Generale",
                                description = "Identità e contatti comuni.",
                                selected = true,
                            ),
                            ProtectionProfileChoice(
                                id = "HEALTHCARE",
                                label = "Sanitario",
                                description = "Dati personali e sanitari.",
                                selected = false,
                            ),
                        ),
                    onToggle = {},
                    onProfileSelect = {},
                    onAddCustom = {},
                    onAnalyze = {},
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Profilo Generale, selezionato")
            .assertIsDisplayed()
            .assertHasClickAction()
        composeRule
            .onNodeWithContentDescription("Profilo Sanitario")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    private fun connectedBadge(): ConnectionBadgeModel =
        ConnectionBadgeProjector.project(LocalAiConnectionStatus.CONNECTED)
}
