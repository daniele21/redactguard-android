package io.github.daniele21.redactguard.ui

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.daniele21.redactguard.ui.theme.RedactGuardTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProductExperienceInstrumentationTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun importSurfaceExposesTaskActionsAndAccessibleLocalAiState() {
        composeRule.setContent {
            RedactGuardTheme {
                ImportScreen(
                    connection = ConnectionBadgeProjector.project(LocalAiConnectionStatus.CONNECTED),
                    onImportPdf = {},
                    onPasteText = {},
                )
            }
        }

        composeRule.onNodeWithText("Proteggi un documento").assertIsDisplayed()
        composeRule.onNodeWithText("Importa PDF").assertHasClickAction().assertIsEnabled()
        composeRule.onNodeWithText("Incolla testo").assertHasClickAction().assertIsEnabled()
        composeRule.onNodeWithContentDescription("Stato AI locale: AI locale collegata").assertIsDisplayed()
    }

    @Test
    fun definitionSurfaceBlocksAnalysisUntilAProtectionCategoryIsSelected() {
        composeRule.setContent {
            RedactGuardTheme {
                DefinitionSelectionScreen(
                    connection = ConnectionBadgeProjector.project(LocalAiConnectionStatus.CONNECTED),
                    choices = listOf(DefinitionChoice(id = "email", label = "Email", selected = false)),
                    onToggle = {},
                    onAddCustom = {},
                    onAnalyze = {},
                )
            }
        }

        composeRule.onNodeWithText("Seleziona almeno una categoria per continuare.").assertIsDisplayed()
        composeRule.onNodeWithText("Analizza in locale").assertIsNotEnabled()
    }

    @Test
    fun definitionSurfaceShowsProductProfilesAsDecisionCards() {
        composeRule.setContent {
            RedactGuardTheme {
                DefinitionSelectionScreen(
                    connection = ConnectionBadgeProjector.project(LocalAiConnectionStatus.CONNECTED),
                    choices = listOf(DefinitionChoice(id = "email", label = "Email", selected = true)),
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

        composeRule.onNodeWithText("Profili rapidi").assertIsDisplayed()
        composeRule.onNodeWithText("Personalizza categorie").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Profilo Generale, selezionato").assertHasClickAction().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Profilo Sanitario").assertHasClickAction().assertIsDisplayed()
    }

    @Test
    fun definitionSurfaceHidesPresetSelectorForSingleOption() {
        composeRule.setContent {
            RedactGuardTheme {
                DefinitionSelectionScreen(
                    connection = ConnectionBadgeProjector.project(LocalAiConnectionStatus.CONNECTED),
                    choices = listOf(DefinitionChoice(id = "email", label = "Email", selected = true)),
                    presets =
                        listOf(
                            LocalAiPresetChoice(
                                id = "preset-0",
                                label = "Bilanciata",
                                description = "Opzione consigliata",
                                selected = true,
                            ),
                        ),
                    onToggle = {},
                    onAddCustom = {},
                    onAnalyze = {},
                )
            }
        }

        assertEquals(0, composeRule.onAllNodesWithText("Modalità di analisi").fetchSemanticsNodes().size)
        composeRule.onNodeWithText("Analizza in locale").assertIsEnabled()
    }

    @Test
    fun definitionSurfaceShowsOnlyConsumerSafePresetMetadataForMultipleOptions() {
        composeRule.setContent {
            RedactGuardTheme {
                DefinitionSelectionScreen(
                    connection = ConnectionBadgeProjector.project(LocalAiConnectionStatus.CONNECTED),
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
                    onPresetSelect = {},
                    onAddCustom = {},
                    onAnalyze = {},
                )
            }
        }

        composeRule.onNodeWithText("Modalità di analisi").assertIsDisplayed()
        composeRule.onNodeWithText("Bilanciata").assertHasClickAction().assertIsDisplayed()
        composeRule.onNodeWithText("Accurata").assertHasClickAction().assertIsDisplayed()
        composeRule.onNodeWithText("Analizza in locale").assertIsEnabled()
    }

    @Test
    fun reviewShowsMaskedContextAndKeepsExportBlockedUntilDecisionsAreComplete() {
        composeRule.setContent {
            RedactGuardTheme {
                ReviewScreen(
                    connection = ConnectionBadgeProjector.project(LocalAiConnectionStatus.CONNECTED),
                    finding =
                        ReviewFindingModel(
                            id = "finding-1",
                            categoryLabel = "Email",
                            placeholder = "[EMAIL_1]",
                            context =
                                ReviewContextModel(
                                    maskedText = "Contatta [EMAIL_1] per assistenza.",
                                    focusPlaceholder = "[EMAIL_1]",
                                    pageNumber = 2,
                                ),
                            revealedValue = null,
                            decision = ReviewDecision.PENDING,
                        ),
                    position = 0,
                    total = 1,
                    onRevealToggle = {},
                    onRedact = {},
                    onIgnore = {},
                    onPrevious = {},
                    onNext = {},
                    onExport = {},
                    exportEnabled = false,
                )
            }
        }

        composeRule.onNodeWithText("Revisione 1/1").assertIsDisplayed()
        composeRule.onNodeWithText("Contesto").assertIsDisplayed()
        composeRule.onNodeWithText("Pagina 2").assertIsDisplayed()
        composeRule.onNodeWithText("Contatta [EMAIL_1] per assistenza.").assertIsDisplayed()
        composeRule.onNodeWithText("[EMAIL_1]").assertIsDisplayed()
        composeRule.onNodeWithText("Mostra valore").assertHasClickAction()
        composeRule.onNodeWithText("Oscura").assertHasClickAction().assertIsEnabled()
        composeRule.onNodeWithText("Mantieni").assertHasClickAction().assertIsEnabled()
        composeRule.onNodeWithText("Decisione da prendere").assertIsDisplayed()
        composeRule.onNodeWithText("Esporta PDF protetto").assertIsNotEnabled()
    }

    @Test
    fun technicalFailureDetailsStayCollapsedUntilExplicitlyRequested() {
        composeRule.setContent {
            RedactGuardTheme {
                ProductErrorScreen(
                    connection = ConnectionBadgeProjector.project(LocalAiConnectionStatus.CONNECTED),
                    title = "Impossibile elaborare il PDF",
                    message = "Prova a importare di nuovo il documento.",
                    technicalDetails =
                        ProductErrorTechnicalDetails(
                            code = "RG-PDF-005",
                            cause = "PARSER_FAILED",
                            stage = "DOCUMENT_IMPORT",
                            operationId = "operation-1",
                            lowLevelStep = "LOAD_DOCUMENT",
                            lowLevelType = "IOException",
                        ),
                    onRetry = null,
                    onNewDocument = {},
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Mostra dettagli tecnici dell’errore")
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithContentDescription("Nascondi dettagli tecnici dell’errore").assertIsDisplayed()
        composeRule.onNodeWithText("Codice: RG-PDF-005").assertIsDisplayed()
        composeRule.onNodeWithText("Step: LOAD_DOCUMENT").assertIsDisplayed()
    }
}
