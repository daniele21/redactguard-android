package io.github.daniele21.redactguard.ui

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.daniele21.redactguard.BuildConfig
import io.github.daniele21.redactguard.ui.theme.RedactGuardTheme
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

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
        composeRule.onNodeWithText("Proteggi i tuoi documenti.").assertIsDisplayed()
        composeRule.onNodeWithText("Importa un PDF").assertIsDisplayed()
        composeRule.onNodeWithText("Incolla testo").assertIsDisplayed()
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
        composeRule.onNodeWithText("Cosa vuoi proteggere?").assertIsDisplayed()
        composeRule.onNodeWithText("Generale").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Profilo Generale, selezionato").assertIsDisplayed()
        composeRule.onNodeWithText("Analizza in locale").assertIsDisplayed()
    }

    @Test
    fun captureAnalysisReference() {
        captureEvidence("03-analysis-compact") {
            AnalysisScreen(
                connection = readyConnection(),
                progress = referenceAnalysisProgress(),
                onCancel = {},
            )
        }
        composeRule.onNodeWithText("Analisi in corso").assertIsDisplayed()
        composeRule.onNodeWithText("Ricerca dati sensibili").assertIsDisplayed()
        composeRule.onNodeWithText("Annulla analisi").assertIsDisplayed()
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
        composeRule.onNodeWithContentDescription("Valore sensibile nascosto").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Mostra valore sensibile").assertIsDisplayed()
        composeRule.onNodeWithText("Oscura (consigliato)").assertIsDisplayed()
        composeRule.onNodeWithText("Mantieni").assertIsDisplayed()
    }

    @Test
    fun captureOutcomeReference() {
        captureEvidence("05-outcome-compact") {
            ExportSuccessScreen(
                connection = readyConnection(),
                onNewDocument = {},
                summary = referenceOutcomeSummary(),
            )
        }
        composeRule.onNodeWithText("Documento protetto").assertIsDisplayed()
        composeRule.onNodeWithText("Totale occorrenze").assertIsDisplayed()
        composeRule.onNodeWithText("PDF protetto salvato").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("Condividi").fetchSemanticsNodes().size)
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
        composeRule.onNodeWithText("Impossibile elaborare il PDF").assertIsDisplayed()
        composeRule.onNodeWithText("Riprova").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("Codice: RG-PDF-004").fetchSemanticsNodes().size)
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
                summary = referenceReviewSummary(),
            )
        }
        composeRule.onNodeWithText("documento-demo.pdf").assertIsDisplayed()
        composeRule.onNodeWithText("Pagina 2 di 3").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Valore sensibile nascosto").assertIsDisplayed()
        composeRule.onNodeWithText("Oscura (consigliato)").assertIsDisplayed()
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
    val additionalOutputPath =
        InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.takeIf { it.isNotBlank() }
            ?: error("Visual evidence requires the Gradle additionalTestOutputDir instrumentation argument")
    val evidenceDir =
        File(additionalOutputPath).apply {
            check(isDirectory || mkdirs()) { "Unable to create visual evidence output directory" }
        }
    val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
    FileOutputStream(File(evidenceDir, "$name.png")).use { output ->
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
            "Unable to encode visual evidence $name"
        }
    }

    val metrics = targetContext.resources.displayMetrics
    val metadata =
        JSONObject()
            .put("schema_version", 2)
            .put("evidence_kind", "android_emulator_visual_reference_v2")
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
            .put("target_comparison_required", true)
            .put(
                "claim_boundary",
                "Emulator screenshots plus structural semantics support target-fidelity/adaptive review only; they do not prove physical-device accessibility, runtime integration, performance, or usability.",
            )
    File(evidenceDir, "metadata.json").writeText(metadata.toString(2))
}

private fun readyConnection(): ConnectionBadgeModel = ConnectionBadgeProjector.project(LocalAiConnectionStatus.CONNECTED)

private fun referenceAnalysisProgress(): AnalysisProgressModel =
    AnalysisProgressModel(
        title = "Ricerca dei dati sensibili",
        message = "L’AI locale sta cercando le categorie selezionate nel documento.",
        contentDescription = "Analisi locale dei dati sensibili in corso",
        visualStage = AnalysisVisualStage.SEARCHING,
    )

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
        DefinitionChoice(id = "full-name", label = "Nomi e identità", selected = true),
        DefinitionChoice(id = "email", label = "Email", selected = true),
        DefinitionChoice(id = "telephone", label = "Telefono", selected = true),
        DefinitionChoice(id = "postal-address", label = "Indirizzi e luoghi", selected = true),
        DefinitionChoice(id = "italian-tax-code", label = "Codice fiscale", selected = true),
        DefinitionChoice(id = "private-date", label = "Date private", selected = true),
        DefinitionChoice(id = "private-url", label = "URL privati", selected = true),
        DefinitionChoice(id = "iban", label = "Dati finanziari", selected = false),
        DefinitionChoice(id = "health-condition", label = "Dati sanitari", selected = false),
    )

private fun referenceFinding(): ReviewFindingModel =
    ReviewFindingModel(
        id = "finding-reference",
        categoryLabel = "Email",
        placeholder = "m•••••@example.test",
        context =
            ReviewContextModel(
                maskedText =
                    "Per confermare l’appuntamento, scrivi a m•••••@example.test. Il riferimento precedente resta mascherato.",
                focusPlaceholder = "m•••••@example.test",
                pageNumber = 2,
            ),
        revealedValue = null,
        decision = ReviewDecision.PENDING,
    )

private fun referenceReviewSummary(): ProductDocumentSummary =
    ProductDocumentSummary(
        displayName = "documento-demo.pdf",
        pageCount = 3,
        totalFindings = 4,
        redactedCount = 1,
        keptCount = 1,
        pendingCount = 2,
        categoryCounts = referenceCategoryCounts(),
    )

private fun referenceOutcomeSummary(): ProductDocumentSummary =
    ProductDocumentSummary(
        displayName = "documento-demo.pdf",
        pageCount = 3,
        totalFindings = 4,
        redactedCount = 3,
        keptCount = 1,
        pendingCount = 0,
        categoryCounts = referenceCategoryCounts(),
    )

private fun referenceCategoryCounts(): List<ProductCategorySummary> =
    listOf(
        ProductCategorySummary(PiiVisualFamily.IDENTITY, 1),
        ProductCategorySummary(PiiVisualFamily.CONTACT, 2),
        ProductCategorySummary(PiiVisualFamily.FINANCIAL, 1),
    )
