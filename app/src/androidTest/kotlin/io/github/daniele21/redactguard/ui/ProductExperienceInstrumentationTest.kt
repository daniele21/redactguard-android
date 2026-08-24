package io.github.daniele21.redactguard.ui

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.daniele21.redactguard.ui.theme.RedactGuardTheme
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
        composeRule.onNodeWithContentDescription("Stato AI locale: AI locale pronta").assertIsDisplayed()
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
    fun reviewKeepsFindingHiddenAndExportBlockedUntilDecisionsAreComplete() {
        composeRule.setContent {
            RedactGuardTheme {
                ReviewScreen(
                    connection = ConnectionBadgeProjector.project(LocalAiConnectionStatus.CONNECTED),
                    finding =
                        ReviewFindingModel(
                            id = "finding-1",
                            categoryLabel = "Email",
                            placeholder = "••••••",
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

        composeRule.onNodeWithText("••••••").assertIsDisplayed()
        composeRule.onNodeWithText("Mostra valore").assertHasClickAction()
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
