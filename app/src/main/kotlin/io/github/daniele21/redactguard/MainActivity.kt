package io.github.daniele21.redactguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.daniele21.redactguard.infrastructure.document.DocumentSourceRegistry
import io.github.daniele21.redactguard.ui.AdaptiveProductSurfaceForWindow
import io.github.daniele21.redactguard.ui.AnalysisScreen
import io.github.daniele21.redactguard.ui.CustomPiiDialog
import io.github.daniele21.redactguard.ui.DefinitionSelectionScreen
import io.github.daniele21.redactguard.ui.ExportSuccessScreen
import io.github.daniele21.redactguard.ui.ExportingScreen
import io.github.daniele21.redactguard.ui.ImportScreen
import io.github.daniele21.redactguard.ui.ImportingScreen
import io.github.daniele21.redactguard.ui.NoFindingsScreen
import io.github.daniele21.redactguard.ui.PasteTextDialog
import io.github.daniele21.redactguard.ui.ProductErrorScreen
import io.github.daniele21.redactguard.ui.ProductRetryTarget
import io.github.daniele21.redactguard.ui.ProductStep
import io.github.daniele21.redactguard.ui.ProtectionProfileProjector
import io.github.daniele21.redactguard.ui.ProtectionProfileSelection
import io.github.daniele21.redactguard.ui.ReviewScreen
import io.github.daniele21.redactguard.ui.theme.RedactGuardTheme

class MainActivity : ComponentActivity() {
    private lateinit var productViewModel: RedactGuardProductViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        productViewModel = ViewModelProvider(this)[RedactGuardProductViewModel::class.java]

        val importPdf =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                uri?.let(productViewModel::importPdf)
            }
        val exportPdf =
            registerForActivityResult(ActivityResultContracts.CreateDocument(DocumentSourceRegistry.PDF_MIME_TYPE)) { uri ->
                uri?.let(productViewModel::exportPdf)
            }

        setContent {
            val state by productViewModel.uiState.collectAsStateWithLifecycle()
            val presetState by productViewModel.presetUiState.collectAsStateWithLifecycle()
            var showCustomPiiDialog by remember { mutableStateOf(false) }
            var showPasteTextDialog by remember { mutableStateOf(false) }
            LaunchedEffect(state.step) {
                if (state.step != ProductStep.DEFINITIONS) showCustomPiiDialog = false
                if (state.step != ProductStep.IMPORT) showPasteTextDialog = false
            }

            RedactGuardTheme {
                AdaptiveProductSurfaceForWindow { windowClass ->
                    when (state.step) {
                        ProductStep.IMPORT -> {
                            ImportScreen(
                                connection = state.connection,
                                onImportPdf = { importPdf.launch(arrayOf(DocumentSourceRegistry.PDF_MIME_TYPE)) },
                                onPasteText = { showPasteTextDialog = true },
                            )
                        }

                        ProductStep.IMPORTING -> {
                            ImportingScreen(state.connection)
                        }

                        ProductStep.DEFINITIONS -> {
                            val protectionProfiles = ProtectionProfileProjector.project(state.definitions)
                            DefinitionSelectionScreen(
                                connection = state.connection,
                                choices = state.definitions,
                                profiles = protectionProfiles,
                                presets = presetState.choices,
                                presetSelectionNotice = presetState.replacementNotice,
                                onToggle = productViewModel::toggleDefinition,
                                onProfileSelect = { profileId ->
                                    ProtectionProfileSelection
                                        .togglesFor(profileId, state.definitions)
                                        .forEach(productViewModel::toggleDefinition)
                                },
                                onPresetSelect = productViewModel::selectAnalysisPreset,
                                onAddCustom = { showCustomPiiDialog = true },
                                onAnalyze = productViewModel::startAnalysis,
                            )
                        }

                        ProductStep.ANALYZING -> {
                            AnalysisScreen(
                                connection = state.connection,
                                onCancel = productViewModel::cancelAnalysis,
                            )
                        }

                        ProductStep.REVIEW -> {
                            ReviewScreen(
                                connection = state.connection,
                                finding = requireNotNull(state.reviewFinding),
                                position = state.reviewPosition,
                                total = state.reviewTotal,
                                onRevealToggle = productViewModel::toggleReveal,
                                onRedact = productViewModel::redactCurrent,
                                onIgnore = productViewModel::ignoreCurrent,
                                onPrevious = productViewModel::previousFinding,
                                onNext = productViewModel::nextFinding,
                                onExport = { exportPdf.launch(productViewModel.suggestedExportFileName()) },
                                exportEnabled = state.exportEnabled,
                                windowClass = windowClass,
                            )
                        }

                        ProductStep.NO_FINDINGS -> {
                            NoFindingsScreen(
                                connection = state.connection,
                                onExport = { exportPdf.launch(productViewModel.suggestedExportFileName()) },
                                onNewDocument = productViewModel::newDocument,
                            )
                        }

                        ProductStep.EXPORTING -> {
                            ExportingScreen(state.connection)
                        }

                        ProductStep.EXPORTED -> {
                            ExportSuccessScreen(
                                connection = state.connection,
                                onNewDocument = productViewModel::newDocument,
                            )
                        }

                        ProductStep.ERROR -> {
                            val error = requireNotNull(state.error)
                            val retry =
                                when (error.retryTarget) {
                                    ProductRetryTarget.ANALYSIS -> {
                                        productViewModel::retryFromError
                                    }

                                    ProductRetryTarget.EXPORT -> {
                                        { exportPdf.launch(productViewModel.suggestedExportFileName()) }
                                    }

                                    ProductRetryTarget.NONE -> {
                                        null
                                    }
                                }
                            ProductErrorScreen(
                                connection = state.connection,
                                title = error.title,
                                message = error.message,
                                technicalDetails = error.technicalDetails,
                                onRetry = retry,
                                onNewDocument = productViewModel::newDocument,
                            )
                        }
                    }
                }

                if (state.step == ProductStep.IMPORT && showPasteTextDialog) {
                    PasteTextDialog(
                        onDismiss = { showPasteTextDialog = false },
                        onSubmit = { text ->
                            showPasteTextDialog = false
                            productViewModel.importText(text)
                        },
                    )
                }

                if (state.step == ProductStep.DEFINITIONS && showCustomPiiDialog) {
                    CustomPiiDialog(
                        onDismiss = { showCustomPiiDialog = false },
                        onSubmit = { input ->
                            productViewModel.addCustomPii(
                                label = input.label,
                                definition = input.definition,
                                example = input.example,
                            )
                        },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::productViewModel.isInitialized) productViewModel.connectHarness()
    }
}
