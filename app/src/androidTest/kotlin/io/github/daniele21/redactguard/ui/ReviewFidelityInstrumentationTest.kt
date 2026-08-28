package io.github.daniele21.redactguard.ui

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import io.github.daniele21.redactguard.ui.theme.RedactGuardTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReviewFidelityInstrumentationTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun compactReviewKeepsMaskedContextHiddenValueAndDecisionHierarchy() {
        composeRule.setContent {
            RedactGuardTheme {
                ReviewScreen(
                    connection = connectedBadge(),
                    finding = pendingFinding(),
                    position = 0,
                    total = 3,
                    onRevealToggle = {},
                    onRedact = {},
                    onIgnore = {},
                    onPrevious = {},
                    onNext = {},
                    onExport = {},
                    exportEnabled = false,
                    windowClass = ProductWindowClass.COMPACT,
                )
            }
        }

        composeRule.onNodeWithText("1 di 3").assertIsDisplayed()
        composeRule.onNodeWithText("Email").assertIsDisplayed()
        composeRule.onNodeWithText("Contatta [EMAIL_1] per confermare.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Valore sensibile nascosto").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("mario.rossi@example.test").fetchSemanticsNodes().size)
        composeRule.onNodeWithText("Oscura (consigliato)").assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithText("Mantieni").assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun expandedReviewShowsTruthfulSummaryContextAndDecisionZones() {
        composeRule.setContent {
            RedactGuardTheme {
                ReviewScreen(
                    connection = connectedBadge(),
                    finding = pendingFinding(),
                    position = 1,
                    total = 4,
                    onRevealToggle = {},
                    onRedact = {},
                    onIgnore = {},
                    onPrevious = {},
                    onNext = {},
                    onExport = {},
                    exportEnabled = false,
                    windowClass = ProductWindowClass.EXPANDED,
                    summary =
                        ProductDocumentSummary(
                            displayName = "documento-riservato.pdf",
                            pageCount = 6,
                            totalFindings = 4,
                            redactedCount = 1,
                            keptCount = 0,
                            pendingCount = 3,
                            categoryCounts =
                                listOf(
                                    ProductCategorySummary(PiiVisualFamily.CONTACT, 2),
                                    ProductCategorySummary(PiiVisualFamily.IDENTITY, 2),
                                ),
                        ),
                )
            }
        }

        composeRule.onNodeWithText("documento-riservato.pdf").assertIsDisplayed()
        composeRule.onNodeWithText("Pagina 2 di 6").assertIsDisplayed()
        composeRule.onNodeWithText("Contatti").assertIsDisplayed()
        composeRule.onNodeWithText("Identità").assertIsDisplayed()
        composeRule.onNodeWithText("Contatta [EMAIL_1] per confermare.").assertIsDisplayed()
        composeRule.onNodeWithText("Oscura (consigliato)").assertIsDisplayed()
    }

    private fun connectedBadge(): ConnectionBadgeModel =
        ConnectionBadgeProjector.project(LocalAiConnectionStatus.CONNECTED)

    private fun pendingFinding(): ReviewFindingModel =
        ReviewFindingModel(
            id = "finding-review-target",
            categoryLabel = "Email",
            placeholder = "[EMAIL_1]",
            context =
                ReviewContextModel(
                    maskedText = "Contatta [EMAIL_1] per confermare.",
                    focusPlaceholder = "[EMAIL_1]",
                    pageNumber = 2,
                ),
            revealedValue = null,
            decision = ReviewDecision.PENDING,
        )
}
