package io.github.daniele21.redactguard.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.daniele21.redactguard.MainActivity
import org.junit.Rule
import org.junit.Test

/** UI half of the real two-APK Harnex connection journey. The workflow supplies the authorization state. */
class HarnexConnectionSettingsE2eUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun pendingAuthorizationIsActionableInSettings() {
        openSettings()

        waitForText("Autorizzazione Harnex richiesta")
        composeRule.onNodeWithText("Autorizzazione Harnex richiesta").assertIsDisplayed()
        composeRule.onNodeWithText("Apri Harnex").assertIsDisplayed()
    }

    @Test
    fun authorizedConnectionCanBeDisconnectedAndReconnectedFromSettings() {
        openSettings()

        waitForText("Connesso a Harnex")
        composeRule.onNodeWithText("Disconnetti Harnex").assertIsDisplayed().performClick()

        waitForText("Disconnesso")
        composeRule.onNodeWithText("Connetti a Harnex").assertIsDisplayed().performClick()

        waitForText("Connesso a Harnex")
        composeRule.onNodeWithText("Connesso a Harnex").assertIsDisplayed()
    }

    private fun openSettings() {
        composeRule.onNodeWithText("Impostazioni").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = CONNECTION_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val CONNECTION_TIMEOUT_MILLIS = 10_000L
    }
}
