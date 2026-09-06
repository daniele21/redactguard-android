package io.github.daniele21.redactguard.ui

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.daniele21.redactguard.BuildConfig
import io.github.daniele21.redactguard.domain.analysis.AnalysisPromptPolicy
import io.github.daniele21.redactguard.domain.analysis.LocalAiRuntimeState
import io.github.daniele21.redactguard.ui.theme.RedactGuardTheme
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/** Full-media evidence journey for the RedactGuard-owned analysis prompt Settings experience. */
@RunWith(AndroidJUnit4::class)
class AnalysisPromptSettingsUiEvidenceInstrumentationTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun captureAnalysisPromptSettingsJourneyCheckpoints() {
        val prompt = mutableStateOf(AnalysisPromptPolicy.defaultEditablePrompt)
        val isCustom = mutableStateOf(false)

        composeRule.setContent {
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
                            currentPrompt = prompt.value,
                            isCustom = isCustom.value,
                            defaultPrompt = AnalysisPromptPolicy.defaultEditablePrompt,
                            protectedRules = AnalysisPromptPolicy.protectedRules,
                            maxCharacters = AnalysisPromptPolicy.MAX_EDITABLE_CHARACTERS,
                        ),
                    onValidateAnalysisPrompt = AnalysisPromptPolicy::validate,
                    onSaveAnalysisPrompt = { value ->
                        val validation = AnalysisPromptPolicy.validate(value)
                        if (validation.isValid) {
                            prompt.value = validation.normalized
                            isCustom.value = validation.normalized != AnalysisPromptPolicy.defaultEditablePrompt
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

        captureCheckpoint("15-prompt-default", "default", "Predefinito")

        composeRule.onNodeWithTag("settings-analysis-prompt-edit").performScrollTo().performClick()
        composeRule.onNodeWithText("Prompt di analisi").assertIsDisplayed()
        composeRule
            .onNodeWithTag("settings-analysis-prompt-editor")
            .performTextReplacement("Rileva soltanto occorrenze esplicite presenti nel documento.")
        composeRule.onNodeWithTag("settings-analysis-prompt-save").performClick()

        captureCheckpoint("16-prompt-custom", "custom", "Personalizzato")

        composeRule.onNodeWithTag("settings-analysis-prompt-edit").performScrollTo().performClick()
        composeRule.onNodeWithTag("settings-analysis-prompt-reset").performScrollTo().performClick()
        composeRule.onNodeWithText("Prompt di analisi").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-analysis-prompt-save").performClick()

        captureCheckpoint("17-prompt-restored", "restored-default", "Predefinito")
    }

    private fun captureCheckpoint(
        name: String,
        checkpoint: String,
        expectedText: String,
    ) {
        composeRule.waitForIdle()
        composeRule.onNodeWithText(expectedText).assertIsDisplayed()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val evidenceDir = additionalOutputDir()
        FileOutputStream(File(evidenceDir, "$name.png")).use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                "Unable to encode prompt Settings UI evidence $name"
            }
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val metrics = context.resources.displayMetrics
        val metadata =
            JSONObject()
                .put("schema_version", 2)
                .put("evidence_kind", "android_emulator_e2e_ui_checkpoint_v2")
                .put("journey", "customize-analysis-prompt")
                .put("checkpoint", checkpoint)
                .put("screenshot", "$name.png")
                .put("expected_text", expectedText)
                .put("source_revision", BuildConfig.SOURCE_REVISION)
                .put("build_id", BuildConfig.REDACTGUARD_BUILD_ID)
                .put("version_name", BuildConfig.VERSION_NAME)
                .put("version_code", BuildConfig.VERSION_CODE)
                .put("sdk_int", Build.VERSION.SDK_INT)
                .put("manufacturer", Build.MANUFACTURER)
                .put("model", Build.MODEL)
                .put("width_px", metrics.widthPixels)
                .put("height_px", metrics.heightPixels)
                .put("density_dpi", metrics.densityDpi)
                .put("synthetic_content", true)
                .put("prompt_contents_logged", false)
                .put(
                    "claim_boundary",
                    "This checkpoint plus the complete journey video proves the production Compose prompt Settings interaction on the emulator. It does not prove physical-device accessibility or real ARM64/GGUF model behavior.",
                )
        File(evidenceDir, "$name.json").writeText(metadata.toString(2))
    }

    private fun additionalOutputDir(): File {
        val outputPath =
            InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.takeIf { it.isNotBlank() }
                ?: error("Prompt Settings evidence requires the Gradle additionalTestOutputDir instrumentation argument")
        return File(outputPath).apply {
            check(isDirectory || mkdirs()) { "Unable to create prompt Settings evidence output directory" }
        }
    }
}
