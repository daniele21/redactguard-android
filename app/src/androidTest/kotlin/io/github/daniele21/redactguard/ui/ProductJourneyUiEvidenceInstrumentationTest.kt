package io.github.daniele21.redactguard.ui

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.daniele21.redactguard.BuildConfig
import io.github.daniele21.redactguard.domain.failure.ProductFailure
import io.github.daniele21.redactguard.domain.failure.ProductFailureKind
import io.github.daniele21.redactguard.ui.theme.RedactGuardTheme
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Captures privacy-safe production Compose checkpoints aligned with the deterministic emulator
 * product journeys. Document parsing, redaction and export assertions remain owned by the domain
 * journey instrumentation; these screenshots add visible UI evidence without widening that claim.
 */
@RunWith(AndroidJUnit4::class)
class ProductJourneyUiEvidenceInstrumentationTest {
    @get:Rule val composeRule = createComposeRule()

    private lateinit var surface: MutableState<JourneySurface>

    @Test
    fun capturePastedTextJourneyCheckpoints() {
        startJourney(JourneySurface.TEXT_IMPORT)
        captureCheckpoint("01-text-import", "protect-text", "import")

        composeRule.onNodeWithText("Incolla testo").performClick()
        captureCheckpoint("02-text-protection", "protect-text", "protection")

        composeRule.onNodeWithText("Analizza in locale").performClick()
        captureCheckpoint("03-text-analysis", "protect-text", "analysis")

        advanceTo(JourneySurface.REVIEW_PENDING)
        captureCheckpoint("04-text-review-pending", "protect-text", "review-pending")

        composeRule.onNodeWithText("Oscura").performClick()
        captureCheckpoint("05-text-review-redacted", "protect-text", "review-redacted")

        composeRule.onNodeWithText("Esporta PDF protetto").performClick()
        captureCheckpoint("06-text-outcome", "protect-text", "outcome")
    }

    @Test
    fun captureTextPdfJourneyCheckpoints() {
        startJourney(JourneySurface.PDF_IMPORT)
        composeRule.onNodeWithText("Importa un PDF").performClick()
        captureCheckpoint("07-pdf-importing", "protect-text-pdf", "importing")

        advanceTo(JourneySurface.PROTECTION)
        captureCheckpoint("08-pdf-protection", "protect-text-pdf", "protection")

        composeRule.onNodeWithText("Analizza in locale").performClick()
        captureCheckpoint("09-pdf-analysis", "protect-text-pdf", "analysis")

        advanceTo(JourneySurface.REVIEW_PENDING)
        captureCheckpoint("10-pdf-review-pending", "protect-text-pdf", "review-pending")

        composeRule.onNodeWithText("Oscura").performClick()
        composeRule.onNodeWithText("Esporta PDF protetto").performClick()
        captureCheckpoint("11-pdf-outcome", "protect-text-pdf", "outcome")
    }

    @Test
    fun captureLocalAiRecoveryJourneyCheckpoints() {
        startJourney(JourneySurface.RECOVERY)
        captureCheckpoint("12-recovery-unavailable", "recover-local-ai", "host-unavailable")

        composeRule.onNodeWithText("Riprova").performClick()
        captureCheckpoint("13-recovery-retry-analysis", "recover-local-ai", "retry-analysis")

        advanceTo(JourneySurface.REVIEW_PENDING)
        captureCheckpoint("14-recovery-review-after-retry", "recover-local-ai", "review-after-retry")
    }

    private fun startJourney(initial: JourneySurface) {
        surface = mutableStateOf(initial)
        composeRule.setContent {
            RedactGuardTheme {
                renderJourneySurface(surface.value)
            }
        }
        composeRule.waitForIdle()
    }

    private fun advanceTo(next: JourneySurface) {
        composeRule.runOnUiThread { surface.value = next }
        composeRule.waitForIdle()
    }

    @Composable
    private fun renderJourneySurface(current: JourneySurface) {
        when (current) {
            JourneySurface.TEXT_IMPORT -> {
                ImportScreen(
                    connection = readyConnection(),
                    onImportPdf = {},
                    onPasteText = { surface.value = JourneySurface.PROTECTION },
                )
            }

            JourneySurface.PDF_IMPORT -> {
                ImportScreen(
                    connection = readyConnection(),
                    onImportPdf = { surface.value = JourneySurface.IMPORTING },
                    onPasteText = {},
                )
            }

            JourneySurface.IMPORTING -> {
                ImportingScreen(connection = readyConnection())
            }

            JourneySurface.PROTECTION -> {
                DefinitionSelectionScreen(
                    connection = readyConnection(),
                    choices = selectedEmailDefinition(),
                    onToggle = {},
                    onAddCustom = {},
                    onAnalyze = { surface.value = JourneySurface.ANALYSIS },
                )
            }

            JourneySurface.ANALYSIS -> {
                AnalysisScreen(
                    connection = readyConnection(),
                    onCancel = {},
                )
            }

            JourneySurface.REVIEW_PENDING -> {
                ReviewScreen(
                    connection = readyConnection(),
                    finding = reviewFinding(ReviewDecision.PENDING),
                    position = 0,
                    total = 1,
                    onRevealToggle = {},
                    onRedact = { surface.value = JourneySurface.REVIEW_REDACTED },
                    onIgnore = {},
                    onPrevious = {},
                    onNext = {},
                    onExport = {},
                    exportEnabled = false,
                    windowClass = ProductWindowClass.COMPACT,
                )
            }

            JourneySurface.REVIEW_REDACTED -> {
                ReviewScreen(
                    connection = readyConnection(),
                    finding = reviewFinding(ReviewDecision.REDACT),
                    position = 0,
                    total = 1,
                    onRevealToggle = {},
                    onRedact = {},
                    onIgnore = {},
                    onPrevious = {},
                    onNext = {},
                    onExport = { surface.value = JourneySurface.OUTCOME },
                    exportEnabled = true,
                    windowClass = ProductWindowClass.COMPACT,
                )
            }

            JourneySurface.OUTCOME -> {
                ExportSuccessScreen(
                    connection = readyConnection(),
                    onNewDocument = {},
                )
            }

            JourneySurface.RECOVERY -> {
                val projected =
                    ProductFailureProjector.project(
                        ProductFailure(ProductFailureKind.HOST_UNAVAILABLE, "emulator-e2e-ui"),
                    )
                ProductErrorScreen(
                    connection = unavailableConnection(),
                    title = projected.title,
                    message = projected.message,
                    technicalDetails = projected.technicalDetails,
                    onRetry = { surface.value = JourneySurface.ANALYSIS },
                    onNewDocument = {},
                )
            }
        }
    }

    private fun captureCheckpoint(
        name: String,
        journey: String,
        checkpoint: String,
    ) {
        composeRule.waitForIdle()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val evidenceDir = additionalOutputDir()
        FileOutputStream(File(evidenceDir, "$name.png")).use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                "Unable to encode E2E UI evidence $name"
            }
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val metrics = context.resources.displayMetrics
        val metadata =
            JSONObject()
                .put("schema_version", 1)
                .put("evidence_kind", "android_emulator_e2e_ui_checkpoint")
                .put("journey", journey)
                .put("checkpoint", checkpoint)
                .put("screenshot", "$name.png")
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
                .put(
                    "claim_boundary",
                    "This screenshot renders production Compose surfaces at deterministic checkpoints of the emulator E2E journeys. The document/parser/export assertions are owned by ProductJourneyInstrumentationTest. Real Harness Binder/native/GGUF execution and physical-device accessibility remain separate evidence.",
                )
        File(evidenceDir, "$name.json").writeText(metadata.toString(2))
    }

    private fun additionalOutputDir(): File {
        val outputPath =
            InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.takeIf { it.isNotBlank() }
                ?: error("E2E UI evidence requires the Gradle additionalTestOutputDir instrumentation argument")
        return File(outputPath).apply {
            check(isDirectory || mkdirs()) { "Unable to create E2E UI evidence output directory" }
        }
    }

    private fun selectedEmailDefinition(): List<DefinitionChoice> =
        listOf(
            DefinitionChoice(
                id = "email",
                label = "Email",
                selected = true,
            ),
        )

    private fun reviewFinding(decision: ReviewDecision): ReviewFindingModel =
        ReviewFindingModel(
            id = "emulator-e2e-email",
            categoryLabel = "Email",
            placeholder = "[EMAIL_1]",
            context =
                ReviewContextModel(
                    maskedText = "Contact [EMAIL_1] for follow up.",
                    focusPlaceholder = "[EMAIL_1]",
                    pageNumber = 1,
                ),
            revealedValue = null,
            decision = decision,
        )

    private fun readyConnection(): ConnectionBadgeModel = ConnectionBadgeProjector.project(LocalAiConnectionStatus.CONNECTED)

    private fun unavailableConnection(): ConnectionBadgeModel = ConnectionBadgeProjector.project(LocalAiConnectionStatus.UNAVAILABLE)

    private enum class JourneySurface {
        TEXT_IMPORT,
        PDF_IMPORT,
        IMPORTING,
        PROTECTION,
        ANALYSIS,
        REVIEW_PENDING,
        REVIEW_REDACTED,
        OUTCOME,
        RECOVERY,
    }
}
