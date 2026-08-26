package io.github.daniele21.redactguard.ui

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.daniele21.redactguard.BuildConfig
import io.github.daniele21.redactguard.ui.theme.RedactGuardTheme
import java.io.File
import java.io.FileOutputStream
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VisualReferenceCompactEvidenceInstrumentationTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun captureImportReference() {
        captureEvidence("01-import-compact") {
            ImportScreen(
                connection = readyConnection(),
                onImportPdf = {},
                onPasteText = {},
            )
        }
    }

    @Test
    fun captureProtectionReference() {
        captureEvidence("02-protection-compact") {
            DefinitionSelectionScreen(
                connection = readyConnection(),
                choices = referenceDefinitionChoices(),
                profiles = referenceProfiles(),
                onToggle = {},
                onProfileSelect = {},
                onAddCustom = {},
                onAnalyze = {},
            )
        }
    }

    @Test
    fun captureAnalysisReference() {
        captureEvidence("03-analysis-compact") {
            AnalysisScreen(
                connection = readyConnection(),
                onCancel = {},
            )
        }
    }

    @Test
    fun captureReviewReference() {
        captureEvidence("04-review-compact") {
            ReviewScreen(
                connection = readyConnection(),
                finding = referenceFinding(),
                position = 1,
                total = 4,
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

    @Test
    fun captureOutcomeReference() {
        captureEvidence("05-outcome-compact") {
            ExportSuccessScreen(
                connection = readyConnection(),
                onNewDocument = {},
            )
        }
    }

    @Test
    fun captureRecoveryReference() {
        captureEvidence("06-recovery-compact") {
            ProductErrorScreen(
                connection = readyConnection(),
                title = "Impossibile elaborare il PDF",
                message = "Il documento non contiene testo estraibile. Importa un PDF con testo oppure incolla il contenuto.",
                technicalDetails =
                    ProductErrorTechnicalDetails(
                        code = "RG-PDF-004",
                        cause = "IMAGE_ONLY_PDF",
                        stage = "DOCUMENT_IMPORT",
                        operationId = "visual-evidence",
                        lowLevelStep = "EXTRACT_TEXT",
                        lowLevelType = "UnsupportedDocument",
                    ),
                onRetry = {},
                onNewDocument = {},
            )
        }
    }

    private fun captureEvidence(
        name: String,
        content: @Composable () -> Unit,
    ) {
        captureVisualReference(composeRule = composeRule, name = name, content = content)
    }
}

@RunWith(AndroidJUnit4::class)
class VisualReferenceExpandedEvidenceInstrumentationTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun captureExpandedReviewReference() {
        captureVisualReference(composeRule = composeRule, name = "07-review-expanded") {
            ReviewScreen(
                connection = readyConnection(),
                finding = referenceFinding(),
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
            )
        }
    }
}

private fun captureVisualReference(
    composeRule: androidx.compose.ui.test.junit4.ComposeContentTestRule,
    name: String,
    content: @Composable () -> Unit,
) {
    composeRule.setContent {
        RedactGuardTheme {
            content()
        }
    }
    composeRule.waitForIdle()

    val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    val evidenceDir = File(targetContext.filesDir, "visual-evidence").apply { mkdirs() }
    val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
    FileOutputStream(File(evidenceDir, "$name.png")).use { output ->
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
            "Unable to encode visual evidence $name"
        }
    }

    val metrics = targetContext.resources.displayMetrics
    val metadata =
        JSONObject()
            .put("schema_version", 1)
            .put("evidence_kind", "android_emulator_visual_reference")
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
            .put("synthetic_environment", true)
            .put(
                "claim_boundary",
                "Emulator screenshots support visual/adaptive review only; they do not prove physical-device accessibility, runtime integration, performance, or usability.",
            )
    File(evidenceDir, "metadata.json").writeText(metadata.toString(2))
}

private fun readyConnection(): ConnectionBadgeModel =
    ConnectionBadgeProjector.project(LocalAiConnectionStatus.CONNECTED)

private fun referenceProfiles(): List<ProtectionProfileChoice> =
    listOf(
        ProtectionProfileChoice(
            id = "GENERAL",
            label = "Generale",
            description = "Identità, contatti e informazioni personali comuni.",
            selected = true,
        ),
        ProtectionProfileChoice(
            id = "HEALTHCARE",
            label = "Sanitario",
            description = "Dati personali, sanitari, trattamenti e risultati.",
            selected = false,
        ),
        ProtectionProfileChoice(
            id = "FINANCIAL",
            label = "Finanziario",
            description = "Conti, IBAN e informazioni finanziarie sensibili.",
            selected = false,
        ),
        ProtectionProfileChoice(
            id = "LEGAL",
            label = "Legale",
            description = "Identità, riferimenti e informazioni sensibili in documenti legali.",
            selected = false,
        ),
    )

private fun referenceDefinitionChoices(): List<DefinitionChoice> =
    listOf(
        DefinitionChoice(id = "private_person", label = "Nomi e identità", selected = true),
        DefinitionChoice(id = "private_email", label = "Email e contatti", selected = true),
        DefinitionChoice(id = "private_address", label = "Indirizzi e luoghi", selected = true),
        DefinitionChoice(id = "account_number", label = "Dati finanziari", selected = false),
        DefinitionChoice(id = "health_condition", label = "Dati sanitari", selected = false),
    )

private fun referenceFinding(): ReviewFindingModel =
    ReviewFindingModel(
        id = "finding-reference",
        categoryLabel = "Email",
        placeholder = "[EMAIL_2]",
        context =
            ReviewContextModel(
                maskedText =
                    "Per confermare l'appuntamento, scrivi a [EMAIL_2]. Il riferimento precedente [PERSON_1] resta mascherato.",
                focusPlaceholder = "[EMAIL_2]",
                pageNumber = 2,
            ),
        revealedValue = null,
        decision = ReviewDecision.PENDING,
    )
