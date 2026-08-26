package io.github.daniele21.redactguard.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithText
import io.github.daniele21.redactguard.ui.theme.RedactGuardTheme
import org.junit.Rule
import org.junit.Test

class AdaptiveReviewInstrumentationTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun expandedReviewKeepsContextAndDecisionInSeparateSemanticPanes() {
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
                    windowClass = ProductWindowClass.EXPANDED,
                )
            }
        }

        composeRule
            .onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Contesto del documento"))
            .assertIsDisplayed()
        composeRule
            .onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Decisione sulla rilevazione"))
            .assertIsDisplayed()
        composeRule.onNodeWithText("Contatta [EMAIL_1] per assistenza.").assertIsDisplayed()
        composeRule.onNodeWithText("Oscura").assertIsDisplayed()
        composeRule.onNodeWithText("Mantieni").assertIsDisplayed()
    }
}
