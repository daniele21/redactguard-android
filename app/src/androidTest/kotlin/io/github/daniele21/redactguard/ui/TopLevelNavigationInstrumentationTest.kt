package io.github.daniele21.redactguard.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.daniele21.redactguard.ui.theme.RedactGuardTheme
import org.junit.Rule
import org.junit.Test

class TopLevelNavigationInstrumentationTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun compactNavigationSwitchesDestinationWithoutOwningAnalyzeState() {
        composeRule.setContent {
            RedactGuardTheme {
                var destination by remember {
                    mutableStateOf(RedactGuardTopLevelDestination.ANALYZE)
                }
                val analyzeMarker by remember { mutableIntStateOf(7) }
                RedactGuardAppShell(
                    windowClass = ProductWindowClass.COMPACT,
                    currentDestination = destination,
                    onDestinationSelected = { destination = it },
                ) {
                    when (destination) {
                        RedactGuardTopLevelDestination.ANALYZE -> Text("Analisi stato $analyzeMarker")
                        RedactGuardTopLevelDestination.LOCAL_AI -> Text("Destinazione AI locale")
                        RedactGuardTopLevelDestination.SETTINGS -> Text("Destinazione impostazioni")
                    }
                }
            }
        }

        composeRule.onNodeWithText("Analisi stato 7").assertIsDisplayed()
        composeRule.onNodeWithText("AI locale").performClick()
        composeRule.onNodeWithText("Destinazione AI locale").assertIsDisplayed()
        composeRule.onNodeWithText("Analizza").performClick()
        composeRule.onNodeWithText("Analisi stato 7").assertIsDisplayed()
    }

    @Test
    fun navigationRailExposesTheSameThreeTopLevelDestinations() {
        composeRule.setContent {
            RedactGuardTheme {
                var destination by remember {
                    mutableStateOf(RedactGuardTopLevelDestination.ANALYZE)
                }
                RedactGuardAppShell(
                    windowClass = ProductWindowClass.EXPANDED,
                    currentDestination = destination,
                    onDestinationSelected = { destination = it },
                ) {
                    Text("Corrente ${destination.label}")
                }
            }
        }

        composeRule.onNodeWithText("Analizza").assertIsDisplayed()
        composeRule.onNodeWithText("AI locale").assertIsDisplayed()
        composeRule.onNodeWithText("Impostazioni").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Corrente Impostazioni").assertIsDisplayed()
    }
}
