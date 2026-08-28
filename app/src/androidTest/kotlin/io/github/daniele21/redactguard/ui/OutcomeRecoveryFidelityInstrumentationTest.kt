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

class OutcomeRecoveryFidelityInstrumentationTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun outcomeShowsOnlyTruthfulSummaryAndOwnedActions() {
        composeRule.setContent {
            RedactGuardTheme {
                ExportSuccessScreen(
                    connection = connectedBadge(),
                    onNewDocument = {},
                    summary =
                        ProductDocumentSummary(
                            displayName = "documento-riservato.pdf",
                            pageCount = 3,
                            totalFindings = 6,
                            redactedCount = 4,
                            keptCount = 2,
                            pendingCount = 0,
                            categoryCounts =
                                listOf(
                                    ProductCategorySummary(PiiVisualFamily.IDENTITY, 2),
                                    ProductCategorySummary(PiiVisualFamily.CONTACT, 4),
                                ),
                        ),
                )
            }
        }

        composeRule.onNodeWithText("Documento protetto").assertIsDisplayed()
        composeRule.onNodeWithText("Totale occorrenze").assertIsDisplayed()
        composeRule.onNodeWithText("Oscurate").assertIsDisplayed()
        composeRule.onNodeWithText("Mantenute").assertIsDisplayed()
        composeRule.onNodeWithText("PDF protetto salvato").assertIsDisplayed()
        composeRule.onNodeWithText("Nuovo documento").assertIsDisplayed().assertHasClickAction()
        assertEquals(0, composeRule.onAllNodesWithText("Condividi").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("documento-riservato.pdf").fetchSemanticsNodes().size)
    }

    @Test
    fun outcomeWithoutSummaryDoesNotFabricateCounters() {
        composeRule.setContent {
            RedactGuardTheme {
                ExportSuccessScreen(
                    connection = connectedBadge(),
                    onNewDocument = {},
                    summary = null,
                )
            }
        }

        composeRule.onNodeWithText("Documento protetto").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("Totale occorrenze").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Oscurate").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Mantenute").fetchSemanticsNodes().size)
    }

    @Test
    fun recoveryKeepsCauseActionAndDiagnosticsProgressivelyDisclosed() {
        composeRule.setContent {
            RedactGuardTheme {
                ProductErrorScreen(
                    connection = unavailableBadge(),
                    title = "AI locale non disponibile",
                    message = "Riapri il servizio AI locale e riprova senza reimportare il documento.",
                    technicalDetails =
                        ProductErrorTechnicalDetails(
                            code = "RG-AI-001",
                            cause = "HOST_UNAVAILABLE",
                            stage = "ANALYSIS",
                            operationId = "evidence-operation",
                            lowLevelStep = null,
                            lowLevelType = null,
                        ),
                    onRetry = {},
                    onNewDocument = {},
                )
            }
        }

        composeRule.onNodeWithText("AI locale non disponibile").assertIsDisplayed()
        composeRule.onNodeWithText("Riprova").assertIsDisplayed().assertHasClickAction()
        assertEquals(0, composeRule.onAllNodesWithText("Codice: RG-AI-001").fetchSemanticsNodes().size)

        composeRule
            .onNodeWithContentDescription("Mostra dettagli tecnici dell’errore")
            .performClick()
        composeRule.onNodeWithText("Codice: RG-AI-001").assertIsDisplayed()
    }

    private fun connectedBadge(): ConnectionBadgeModel = ConnectionBadgeProjector.project(LocalAiConnectionStatus.CONNECTED)

    private fun unavailableBadge(): ConnectionBadgeModel = ConnectionBadgeProjector.project(LocalAiConnectionStatus.UNAVAILABLE)
}
