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
    fun importSurfaceExposesReferenceHierarchyAndAccessibleLocalAiState() {
        composeRule.setContent {
            RedactGuardTheme {
                ImportScreen(
                    connection = connectedBadge(),
                    onImportPdf = {},
                    onPasteText = {},
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("RedactGuard")
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("PROTEZIONE LOCALE")
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("Proteggi i tuoi documenti.")
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("Solo sul dispositivo")
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("Importa un PDF")
            .assertHasClickAction()
            .assertIsEnabled()
        composeRule
            .onNodeWithText("Incolla testo")
            .assertHasClickAction()
            .assertIsEnabled()
        composeRule
            .onNodeWithContentDescription(
                "Stato AI locale: AI locale collegata",
            ).assertIsDisplayed()
        composeRule
            .onNodeWithText("AI locale pronta")
            .assertIsDisplayed()
    }

    @Test
    fun definitionSurfaceBlocksAnalysisUntilAProtectionCategoryIsSelected() {
        composeRule.setContent {
            RedactGuardTheme {
                DefinitionSelectionScreen(
                    connection = connectedBadge(),
                    choices =
                        listOf(
                            DefinitionChoice(
                                id = "email",
                                label = "Email",
                                selected = false,
                            ),
                        ),
                    onToggle = {},
                    onAddCustom = {},
                    onAnalyze = {},
                )
            }
        }

        composeRule
            .onNodeWithText("Seleziona almeno una categoria per continuare.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Esclusa").assertIsDisplayed()
        composeRule
            .onNodeWithText("Analizza in locale")
            .assertIsNotEnabled()
    }

    @Test
    fun definitionSurfaceShowsProfilesAsReferenceDecisionCards() {
        composeRule.setContent {
            RedactGuardTheme {
                DefinitionSelectionScreen(
                    connection = connectedBadge(),
                    choices =
                        listOf(
                            DefinitionChoice(
                                id = "email",
                                label = "Email",
                                selected = true,
                            ),
                        ),
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

        composeRule.onNodeWithText("Preset consigliati").assertIsDisplayed()
        composeRule.onNodeWithText("Categorie selezionate").assertIsDisplayed()
        composeRule.onNodeWithText("Profilo attivo").assertIsDisplayed()
        composeRule.onNodeWithText("Inclusa ✓").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Profilo Generale, selezionato")
            .assertHasClickAction()
            .assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Profilo Sanitario")
            .assertHasClickAction()
            .assertIsDisplayed()
    }

    @Test
    fun definitionSurfaceHidesPresetSelectorForSingleOption() {
        composeRule.setContent {
            RedactGuardTheme {
                DefinitionSelectionScreen(
                    connection = connectedBadge(),
                    choices =
                        listOf(
                            DefinitionChoice(
                                id = "email",
                                label = "Email",
                                selected = true,
                            ),
                        ),
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

        val presetHeaders =
            composeRule
                .onAllNodesWithText("Modalità di analisi")
                .fetchSemanticsNodes()
        assertEquals(0, presetHeaders.size)
        composeRule
            .onNodeWithText("Analizza in locale")
            .assertIsEnabled()
    }

    @Test
    fun definitionSurfaceShowsOnlyConsumerSafePresetMetadataForMultipleOptions() {
        composeRule.setContent {
            RedactGuardTheme {
                DefinitionSelectionScreen(
                    connection = connectedBadge(),
                    choices =
                        listOf(
                            DefinitionChoice(
                                id = "email",
                                label = "Email",
                                selected = true,
                            ),
                        ),
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

        composeRule
            .onNodeWithText("Modalità di analisi")
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("Bilanciata")
            .assertHasClickAction()
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("Accurata")
            .assertHasClickAction()
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("Analizza in locale")
            .assertIsEnabled()
    }

    @Test
    fun analysisUsesTruthfulReferencePhasesWithoutInventedPercentage() {
        composeRule.setContent {
            RedactGuardTheme {
                AnalysisScreen(
                    connection = connectedBadge(),
                    onCancel = {},
                )
            }
        }

        composeRule.onNodeWithText("Analisi in corso").assertIsDisplayed()
        composeRule.onNodeWithText("Documento preparato").assertIsDisplayed()
        composeRule
            .onNodeWithText("Ricerca dati sensibili")
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("Validazione risultati")
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("Annulla analisi")
            .assertHasClickAction()
            .assertIsEnabled()
    }

    @Test
    fun reviewShowsMaskedContextAndKeepsExportBlockedUntilDecisionsAreComplete() {
        composeRule.setContent {
            RedactGuardTheme {
                ReviewScreen(
                    connection = connectedBadge(),
                    finding = pendingEmailFinding(),
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
        composeRule
            .onNodeWithText("Possibile dato sensibile")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Contesto").assertIsDisplayed()
        composeRule.onNodeWithText("Pagina 2").assertIsDisplayed()
        composeRule
            .onNodeWithText("Contatta [EMAIL_1] per assistenza.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("[EMAIL_1]").assertIsDisplayed()
        composeRule
            .onNodeWithText("Mostra valore")
            .assertHasClickAction()
        composeRule
            .onNodeWithText("Oscura")
            .assertHasClickAction()
            .assertIsEnabled()
        composeRule
            .onNodeWithText("Mantieni")
            .assertHasClickAction()
            .assertIsEnabled()
        composeRule
            .onNodeWithText("Decisione da prendere")
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("Esporta PDF protetto")
            .assertIsNotEnabled()
    }

    @Test
    fun exportSuccessUsesApprovedOutcomeHierarchy() {
        composeRule.setContent {
            RedactGuardTheme {
                ExportSuccessScreen(
                    connection = connectedBadge(),
                    onNewDocument = {},
                )
            }
        }

        composeRule
            .onNodeWithText("PROTEZIONE COMPLETATA")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Documento protetto").assertIsDisplayed()
        composeRule.onNodeWithText("Prossimo passo").assertIsDisplayed()
        composeRule
            .onNodeWithText("Proteggi un altro documento")
            .assertHasClickAction()
            .assertIsEnabled()
    }

    @Test
    fun technicalFailureDetailsStayCollapsedUntilExplicitlyRequested() {
        composeRule.setContent {
            RedactGuardTheme {
                ProductErrorScreen(
                    connection = connectedBadge(),
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

        composeRule.onNodeWithText("AZIONE RICHIESTA").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription(
                "Mostra dettagli tecnici dell’errore",
            ).assertIsDisplayed()
            .performClick()
        composeRule
            .onNodeWithContentDescription(
                "Nascondi dettagli tecnici dell’errore",
            ).assertIsDisplayed()
        composeRule
            .onNodeWithText("Codice: RG-PDF-005")
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("Step: LOAD_DOCUMENT")
            .assertIsDisplayed()
    }

    private fun connectedBadge(): ConnectionBadgeModel = ConnectionBadgeProjector.project(LocalAiConnectionStatus.CONNECTED)

    private fun pendingEmailFinding(): ReviewFindingModel =
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
        )
}
