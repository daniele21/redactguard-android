package io.github.daniele21.redactguard

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.daniele21.redactguard.domain.analysis.AnalysisOperationId
import io.github.daniele21.redactguard.domain.analysis.DocumentAnalysisException
import io.github.daniele21.redactguard.domain.analysis.DocumentAnalysisFailureCode
import io.github.daniele21.redactguard.domain.analysis.DocumentAnalysisRequest
import io.github.daniele21.redactguard.domain.analysis.LocalAiRuntimeState
import io.github.daniele21.redactguard.domain.analysis.SequentialDocumentAnalyzer
import io.github.daniele21.redactguard.domain.analysis.ValidatedFinding
import io.github.daniele21.redactguard.domain.document.DocumentSegment
import io.github.daniele21.redactguard.domain.pii.DefinitionSelectionController
import io.github.daniele21.redactguard.domain.pii.PiiDefinition
import io.github.daniele21.redactguard.domain.pii.PiiDefinitionCreationResult
import io.github.daniele21.redactguard.domain.pii.PiiDefinitionDraft
import io.github.daniele21.redactguard.domain.pii.PiiTypeId
import io.github.daniele21.redactguard.domain.redaction.OccurrenceId
import io.github.daniele21.redactguard.domain.redaction.RedactionPlanResult
import io.github.daniele21.redactguard.domain.redaction.RedactionPlanner
import io.github.daniele21.redactguard.domain.redaction.ReviewOccurrence
import io.github.daniele21.redactguard.infrastructure.document.AndroidDocumentExtractor
import io.github.daniele21.redactguard.infrastructure.document.AndroidRedactedPdfExporter
import io.github.daniele21.redactguard.infrastructure.document.DocumentExtractionException
import io.github.daniele21.redactguard.infrastructure.document.DocumentExtractionFailureCode
import io.github.daniele21.redactguard.infrastructure.document.DocumentSourceRegistry
import io.github.daniele21.redactguard.infrastructure.document.ExtractedDocument
import io.github.daniele21.redactguard.infrastructure.document.IsolatedPdfTextReader
import io.github.daniele21.redactguard.infrastructure.localai.BinderAnalysisRuntimeComposition
import io.github.daniele21.redactguard.ui.ConnectionBadgeProjector
import io.github.daniele21.redactguard.ui.DefinitionChoice
import io.github.daniele21.redactguard.ui.LocalAiConnectionStatus
import io.github.daniele21.redactguard.ui.ProductFailureKind
import io.github.daniele21.redactguard.ui.ProductFailureProjector
import io.github.daniele21.redactguard.ui.ProductRetryTarget
import io.github.daniele21.redactguard.ui.ProductStep
import io.github.daniele21.redactguard.ui.RedactGuardProductUiState
import io.github.daniele21.redactguard.ui.ReviewFindingProjector
import io.github.daniele21.redactguard.ui.ReviewProjectionResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Process-local product owner. Sensitive document/review state is deliberately not backed by
 * SavedStateHandle, preferences, database or files and therefore disappears after process death.
 */
internal class RedactGuardProductViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val sourceRegistry = DocumentSourceRegistry(application)
    private val extractor = AndroidDocumentExtractor(sourceRegistry, IsolatedPdfTextReader(application))
    private val exporter = AndroidRedactedPdfExporter(application)
    private val definitionSelection = DefinitionSelectionController()
    private val runtime =
        BinderAnalysisRuntimeComposition.create(application) { state ->
            viewModelScope.launch { updateConnection(state) }
        }
    private val analyzer = SequentialDocumentAnalyzer(runtime)

    private val mutableUiState = MutableStateFlow(RedactGuardProductUiState())
    val uiState: StateFlow<RedactGuardProductUiState> = mutableUiState.asStateFlow()

    private var document: ExtractedDocument? = null
    private var analysisDefinitions: List<PiiDefinition> = emptyList()
    private var reviewOccurrences: List<ReviewOccurrence> = emptyList()
    private var currentReviewIndex = 0
    private var revealedOccurrenceId: OccurrenceId? = null
    private var activeAnalysisId: AnalysisOperationId? = null

    init {
        updateConnection(runtime.connectionState)
        runtime.connect()
    }

    fun connectHarness() {
        runtime.connect()
        updateConnection(runtime.connectionState)
    }

    fun importPdf(uri: Uri) {
        if (mutableUiState.value.step in BUSY_STEPS) return
        clearTaskState(cancelAnalysis = true)
        mutableUiState.update { state ->
            state.copy(
                step = ProductStep.IMPORTING,
                definitions = emptyList(),
                reviewFinding = null,
                reviewPosition = 0,
                reviewTotal = 0,
                exportEnabled = false,
                error = null,
            )
        }
        val sourceRef =
            try {
                sourceRegistry.register(uri)
            } catch (_: IllegalArgumentException) {
                showError(ProductFailureKind.IMPORT_UNREADABLE)
                return
            }
        viewModelScope.launch {
            try {
                val extracted = withContext(Dispatchers.IO) { extractor.extract(sourceRef) }
                document = extracted
                definitionSelection.reset()
                publishDefinitions()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                showError(mapImportFailure(failure))
            } finally {
                sourceRegistry.release(sourceRef)
            }
        }
    }

    fun toggleDefinition(id: String) {
        if (mutableUiState.value.step != ProductStep.DEFINITIONS) return
        val typeId = runCatching { PiiTypeId.parse(id) }.getOrNull() ?: return
        definitionSelection.toggle(typeId)
        publishDefinitions()
    }

    fun addCustomPii(
        label: String,
        definition: String,
        example: String?,
    ): Boolean {
        if (mutableUiState.value.step != ProductStep.DEFINITIONS) return false
        val result =
            definitionSelection.addCustom(
                PiiDefinitionDraft(
                    label = label,
                    definition = definition,
                    example = example,
                ),
            )
        if (result is PiiDefinitionCreationResult.Created) publishDefinitions()
        return result is PiiDefinitionCreationResult.Created
    }

    fun startAnalysis() {
        val extracted = document ?: return
        val selected = definitionSelection.state.selectedDefinitions
        if (selected.isEmpty() || !mutableUiState.value.connection.analysisReady) return
        activeAnalysisId?.let { previous -> analyzer.cancel(previous) {} }
        reviewOccurrences = emptyList()
        revealedOccurrenceId = null
        currentReviewIndex = 0
        analysisDefinitions = selected.toList()
        val operationId = AnalysisOperationId(UUID.randomUUID().toString())
        activeAnalysisId = operationId
        mutableUiState.update { state ->
            state.copy(
                step = ProductStep.ANALYZING,
                reviewFinding = null,
                reviewPosition = 0,
                reviewTotal = 0,
                exportEnabled = false,
                error = null,
            )
        }
        analyzer.analyze(
            operationId = operationId,
            request = DocumentAnalysisRequest(extracted.segments, analysisDefinitions),
        ) { result ->
            viewModelScope.launch {
                if (activeAnalysisId != operationId) return@launch
                activeAnalysisId = null
                result.fold(
                    onSuccess = ::handleAnalysisSuccess,
                    onFailure = { failure -> showError(mapAnalysisFailure(failure)) },
                )
            }
        }
    }

    fun cancelAnalysis() {
        val operationId = activeAnalysisId ?: returnToDefinitions()
        analyzer.cancel(operationId) {
            viewModelScope.launch {
                if (activeAnalysisId == operationId) {
                    activeAnalysisId = null
                    reviewOccurrences = emptyList()
                    revealedOccurrenceId = null
                    returnToDefinitions()
                }
            }
        }
    }

    fun toggleReveal() {
        if (mutableUiState.value.step != ProductStep.REVIEW) return
        val occurrence = reviewOccurrences.getOrNull(currentReviewIndex) ?: return
        revealedOccurrenceId = if (revealedOccurrenceId == occurrence.id) null else occurrence.id
        publishReview()
    }

    fun redactCurrent() = updateCurrentReview { it.accept() }

    fun ignoreCurrent() = updateCurrentReview { it.ignore() }

    fun previousFinding() {
        if (currentReviewIndex <= 0) return
        currentReviewIndex -= 1
        revealedOccurrenceId = null
        publishReview()
    }

    fun nextFinding() {
        if (currentReviewIndex + 1 >= reviewOccurrences.size) return
        currentReviewIndex += 1
        revealedOccurrenceId = null
        publishReview()
    }

    fun exportPdf(destination: Uri) {
        val extracted = document ?: return
        if (!canExportFromCurrentState()) return
        revealedOccurrenceId = null
        val planResult = RedactionPlanner.build(extracted.segments, analysisDefinitions, reviewOccurrences)
        val plan = (planResult as? RedactionPlanResult.Ready)?.plan
        if (plan == null) {
            showError(ProductFailureKind.REVIEW_INVALID)
            return
        }
        mutableUiState.update { state ->
            state.copy(
                step = ProductStep.EXPORTING,
                reviewFinding = null,
                error = null,
            )
        }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    exporter.export(destination, extracted.descriptor, extracted.segments, plan)
                }
                mutableUiState.update { state ->
                    state.copy(
                        step = ProductStep.EXPORTED,
                        reviewFinding = null,
                        reviewPosition = 0,
                        reviewTotal = 0,
                        exportEnabled = false,
                        error = null,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                showError(ProductFailureKind.EXPORT_FAILED)
            }
        }
    }

    fun newDocument() {
        clearTaskState(cancelAnalysis = true)
        definitionSelection.reset()
        mutableUiState.update { state ->
            RedactGuardProductUiState(connection = state.connection)
        }
    }

    fun retryFromError() {
        when (mutableUiState.value.error?.retryTarget) {
            ProductRetryTarget.ANALYSIS -> startAnalysis()
            ProductRetryTarget.EXPORT, ProductRetryTarget.NONE, null -> Unit
        }
    }

    fun suggestedExportFileName(): String = EXPORT_FILE_NAME

    override fun onCleared() {
        activeAnalysisId?.let { operationId -> analyzer.cancel(operationId) {} }
        activeAnalysisId = null
        clearSensitiveFields()
        sourceRegistry.close()
        runtime.close()
        super.onCleared()
    }

    private fun publishDefinitions() {
        val choices =
            definitionSelection.state.definitions.map { definition ->
                DefinitionChoice(
                    id = definition.id.value,
                    label = definition.label,
                    selected = definition.id in definitionSelection.state.selectedIds,
                )
            }
        mutableUiState.update { state ->
            state.copy(
                step = ProductStep.DEFINITIONS,
                definitions = choices,
                reviewFinding = null,
                reviewPosition = 0,
                reviewTotal = 0,
                exportEnabled = false,
                error = null,
            )
        }
    }

    private fun handleAnalysisSuccess(findings: List<ValidatedFinding>) {
        reviewOccurrences = findings.map(::toReviewOccurrence).sortedWith(reviewSourceComparator())
        revealedOccurrenceId = null
        currentReviewIndex = 0
        if (reviewOccurrences.isEmpty()) {
            mutableUiState.update { state ->
                state.copy(
                    step = ProductStep.NO_FINDINGS,
                    reviewFinding = null,
                    reviewPosition = 0,
                    reviewTotal = 0,
                    exportEnabled = true,
                    error = null,
                )
            }
        } else {
            publishReview()
        }
    }

    private fun publishReview() {
        val projection =
            ReviewFindingProjector.project(
                occurrences = reviewOccurrences,
                definitions = analysisDefinitions,
                revealedOccurrenceId = revealedOccurrenceId,
            )
        val ready = projection as? ReviewProjectionResult.Ready
        if (ready == null || ready.findings.isEmpty() || currentReviewIndex !in ready.findings.indices) {
            showError(ProductFailureKind.REVIEW_INVALID)
            return
        }
        mutableUiState.update { state ->
            state.copy(
                step = ProductStep.REVIEW,
                reviewFinding = ready.findings[currentReviewIndex],
                reviewPosition = currentReviewIndex,
                reviewTotal = ready.findings.size,
                exportEnabled = ready.canExport,
                error = null,
            )
        }
    }

    private fun updateCurrentReview(transform: (ReviewOccurrence) -> ReviewOccurrence) {
        if (mutableUiState.value.step != ProductStep.REVIEW) return
        val current = reviewOccurrences.getOrNull(currentReviewIndex) ?: return
        reviewOccurrences = reviewOccurrences.toMutableList().also { it[currentReviewIndex] = transform(current) }
        revealedOccurrenceId = null
        publishReview()
    }

    private fun returnToDefinitions() {
        if (document == null) {
            newDocument()
        } else {
            reviewOccurrences = emptyList()
            revealedOccurrenceId = null
            currentReviewIndex = 0
            publishDefinitions()
        }
    }

    private fun canExportFromCurrentState(): Boolean =
        when (mutableUiState.value.step) {
            ProductStep.REVIEW -> mutableUiState.value.exportEnabled
            ProductStep.NO_FINDINGS -> true
            ProductStep.ERROR -> mutableUiState.value.error?.retryTarget == ProductRetryTarget.EXPORT
            else -> false
        }

    private fun showError(kind: ProductFailureKind) {
        revealedOccurrenceId = null
        mutableUiState.update { state ->
            state.copy(
                step = ProductStep.ERROR,
                reviewFinding = null,
                exportEnabled = false,
                error = ProductFailureProjector.project(kind),
            )
        }
    }

    private fun updateConnection(state: LocalAiRuntimeState) {
        val status =
            when (state) {
                LocalAiRuntimeState.CONNECTED -> LocalAiConnectionStatus.CONNECTED
                LocalAiRuntimeState.CONNECTING -> LocalAiConnectionStatus.CONNECTING
                LocalAiRuntimeState.PERMISSION_DENIED -> LocalAiConnectionStatus.PERMISSION_DENIED
                LocalAiRuntimeState.INCOMPATIBLE -> LocalAiConnectionStatus.INCOMPATIBLE
                LocalAiRuntimeState.HOST_NOT_INSTALLED -> LocalAiConnectionStatus.HOST_NOT_INSTALLED
                LocalAiRuntimeState.DISCONNECTED -> LocalAiConnectionStatus.DISCONNECTED
            }
        mutableUiState.update { current ->
            current.copy(connection = ConnectionBadgeProjector.project(status))
        }
    }

    private fun clearTaskState(cancelAnalysis: Boolean) {
        if (cancelAnalysis) {
            activeAnalysisId?.let { operationId -> analyzer.cancel(operationId) {} }
            activeAnalysisId = null
        }
        clearSensitiveFields()
        sourceRegistry.close()
    }

    private fun clearSensitiveFields() {
        document = null
        analysisDefinitions = emptyList()
        reviewOccurrences = emptyList()
        currentReviewIndex = 0
        revealedOccurrenceId = null
    }

    private fun mapImportFailure(failure: Throwable): ProductFailureKind {
        val extraction = failure as? DocumentExtractionException ?: return ProductFailureKind.IMPORT_UNREADABLE
        return when (extraction.code) {
            DocumentExtractionFailureCode.SOURCE_NOT_FOUND,
            DocumentExtractionFailureCode.SOURCE_UNREADABLE,
            -> ProductFailureKind.IMPORT_UNREADABLE

            DocumentExtractionFailureCode.ENCRYPTED_PDF,
            DocumentExtractionFailureCode.MALFORMED_PDF,
            DocumentExtractionFailureCode.PARSER_FAILED,
            DocumentExtractionFailureCode.LIMIT_EXCEEDED,
            DocumentExtractionFailureCode.EMPTY_PDF,
            DocumentExtractionFailureCode.IMAGE_ONLY_PDF,
            -> ProductFailureKind.IMPORT_UNSUPPORTED
        }
    }

    private fun mapAnalysisFailure(failure: Throwable): ProductFailureKind {
        val analysis = failure as? DocumentAnalysisException ?: return ProductFailureKind.ANALYSIS_FAILED
        return when (analysis.code) {
            DocumentAnalysisFailureCode.HOST_UNAVAILABLE -> ProductFailureKind.HOST_UNAVAILABLE

            DocumentAnalysisFailureCode.CAPABILITY_INCOMPATIBLE -> ProductFailureKind.HARNESS_INCOMPATIBLE

            DocumentAnalysisFailureCode.PLAN_REJECTED,
            DocumentAnalysisFailureCode.INVALID_STRUCTURED_RESULT,
            DocumentAnalysisFailureCode.INVALID_FINDINGS,
            DocumentAnalysisFailureCode.CHUNK_FAILED,
            DocumentAnalysisFailureCode.DISCONNECTED,
            DocumentAnalysisFailureCode.CANCELLED,
            -> ProductFailureKind.ANALYSIS_FAILED
        }
    }

    private fun toReviewOccurrence(finding: ValidatedFinding): ReviewOccurrence =
        ReviewOccurrence(
            id = OccurrenceId(finding.typeId, finding.source),
            surface = finding.surface,
        )

    private fun reviewSourceComparator(): Comparator<ReviewOccurrence> =
        compareBy<ReviewOccurrence>(
            { it.id.source.segmentId.value },
            { it.id.source.range.startInclusive },
            { it.id.source.range.endExclusive },
            { it.id.typeId.value },
        )

    private companion object {
        val BUSY_STEPS = setOf(ProductStep.IMPORTING, ProductStep.ANALYZING, ProductStep.EXPORTING)
        const val EXPORT_FILE_NAME = "redactguard-protected.pdf"
    }
}
