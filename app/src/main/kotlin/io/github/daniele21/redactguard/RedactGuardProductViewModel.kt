package io.github.daniele21.redactguard

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.daniele21.redactguard.diagnostics.BoundedFailureDiagnosticStore
import io.github.daniele21.redactguard.diagnostics.FailureDiagnosticContext
import io.github.daniele21.redactguard.diagnostics.FailureDiagnosticEvent
import io.github.daniele21.redactguard.domain.analysis.AnalysisOperationId
import io.github.daniele21.redactguard.domain.analysis.DocumentAnalysisRequest
import io.github.daniele21.redactguard.domain.analysis.LocalAiRuntimeState
import io.github.daniele21.redactguard.domain.analysis.SequentialDocumentAnalyzer
import io.github.daniele21.redactguard.domain.analysis.ValidatedFinding
import io.github.daniele21.redactguard.domain.failure.ProductFailure
import io.github.daniele21.redactguard.domain.failure.ProductFailureKind
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
import io.github.daniele21.redactguard.infrastructure.document.DocumentSourceRegistry
import io.github.daniele21.redactguard.infrastructure.document.ExtractedDocument
import io.github.daniele21.redactguard.infrastructure.document.IsolatedPdfTextReader
import io.github.daniele21.redactguard.infrastructure.document.PlainTextDocumentExtractor
import io.github.daniele21.redactguard.infrastructure.localai.BinderAnalysisRuntimeComposition
import io.github.daniele21.redactguard.ui.ConnectionBadgeProjector
import io.github.daniele21.redactguard.ui.DefinitionChoice
import io.github.daniele21.redactguard.ui.LocalAiConnectionStatus
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
    private val failureDiagnostics = BoundedFailureDiagnosticStore()

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
        val operationId = newOperationId()
        val startedAtNanos = System.nanoTime()
        beginImport()
        val sourceRef =
            try {
                sourceRegistry.register(uri)
            } catch (_: IllegalArgumentException) {
                showError(
                    ProductFailure(ProductFailureKind.SOURCE_UNREADABLE, operationId),
                    diagnosticContext(startedAtNanos),
                )
                return
            } catch (_: SecurityException) {
                showError(
                    ProductFailure(ProductFailureKind.SOURCE_UNREADABLE, operationId),
                    diagnosticContext(startedAtNanos),
                )
                return
            }
        viewModelScope.launch {
            try {
                val extracted = withContext(Dispatchers.IO) { extractor.extract(sourceRef) }
                acceptImportedDocument(extracted)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                showError(
                    ImportFailureMapper.fromThrowable(failure, operationId),
                    diagnosticContext(startedAtNanos),
                )
            } finally {
                sourceRegistry.release(sourceRef)
            }
        }
    }

    fun importText(text: String) {
        if (mutableUiState.value.step in BUSY_STEPS) return
        val operationId = newOperationId()
        val startedAtNanos = System.nanoTime()
        beginImport()
        viewModelScope.launch {
            try {
                val extracted = withContext(Dispatchers.Default) { PlainTextDocumentExtractor.extract(text) }
                acceptImportedDocument(extracted)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                showError(
                    ImportFailureMapper.fromThrowable(failure, operationId),
                    diagnosticContext(startedAtNanos),
                )
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
        val operationId = AnalysisOperationId(newOperationId())
        val startedAtNanos = System.nanoTime()
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
                    onFailure = { failure ->
                        showError(
                            AnalysisFailureMapper.fromThrowable(failure, operationId.value),
                            diagnosticContext(startedAtNanos, extracted.descriptor.pageCount),
                        )
                    },
                )
            }
        }
    }

    fun cancelAnalysis() {
        val operationId = activeAnalysisId
        if (operationId == null) {
            returnToDefinitions()
            return
        }
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
        val operationId = newOperationId()
        val startedAtNanos = System.nanoTime()
        revealedOccurrenceId = null
        val plan =
            when (val planResult = RedactionPlanner.build(extracted.segments, analysisDefinitions, reviewOccurrences)) {
                is RedactionPlanResult.Ready -> {
                    planResult.plan
                }

                is RedactionPlanResult.Blocked -> {
                    showError(
                        ReviewFailureMapper.fromPlanCode(planResult.code, operationId),
                        diagnosticContext(startedAtNanos, extracted.descriptor.pageCount),
                    )
                    return
                }
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
            } catch (failure: Throwable) {
                showError(
                    ExportFailureMapper.fromThrowable(failure, operationId),
                    diagnosticContext(startedAtNanos, extracted.descriptor.pageCount),
                )
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
        failureDiagnostics.clear()
        runtime.close()
        super.onCleared()
    }

    private fun beginImport() {
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
    }

    private fun acceptImportedDocument(extracted: ExtractedDocument) {
        document = extracted
        definitionSelection.reset()
        publishDefinitions()
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
        when (
            val projection =
                ReviewFindingProjector.project(
                    occurrences = reviewOccurrences,
                    definitions = analysisDefinitions,
                    revealedOccurrenceId = revealedOccurrenceId,
                )
        ) {
            is ReviewProjectionResult.Blocked -> {
                showError(
                    ReviewFailureMapper.fromProjectionCode(projection.code, newOperationId()),
                    FailureDiagnosticContext(
                        pageCount = document?.descriptor?.pageCount,
                        appVersion = BuildConfig.VERSION_NAME,
                    ),
                )
            }

            is ReviewProjectionResult.Ready -> {
                if (projection.findings.isEmpty() || currentReviewIndex !in projection.findings.indices) {
                    showError(
                        ProductFailure(ProductFailureKind.UNKNOWN_INTERNAL, newOperationId()),
                        FailureDiagnosticContext(
                            pageCount = document?.descriptor?.pageCount,
                            appVersion = BuildConfig.VERSION_NAME,
                        ),
                    )
                    return
                }
                mutableUiState.update { state ->
                    state.copy(
                        step = ProductStep.REVIEW,
                        reviewFinding = projection.findings[currentReviewIndex],
                        reviewPosition = currentReviewIndex,
                        reviewTotal = projection.findings.size,
                        exportEnabled = projection.canExport,
                        error = null,
                    )
                }
            }
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

    private fun showError(
        failure: ProductFailure,
        context: FailureDiagnosticContext = FailureDiagnosticContext(appVersion = BuildConfig.VERSION_NAME),
    ) {
        failureDiagnostics.record(FailureDiagnosticEvent.from(failure, context))
        revealedOccurrenceId = null
        mutableUiState.update { state ->
            state.copy(
                step = ProductStep.ERROR,
                reviewFinding = null,
                exportEnabled = false,
                error = ProductFailureProjector.project(failure),
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

    private fun diagnosticContext(
        startedAtNanos: Long,
        pageCount: Int? = null,
    ): FailureDiagnosticContext =
        FailureDiagnosticContext(
            durationMs = ((System.nanoTime() - startedAtNanos).coerceAtLeast(0L) / NANOS_PER_MILLISECOND),
            pageCount = pageCount,
            appVersion = BuildConfig.VERSION_NAME,
        )

    private fun newOperationId(): String = UUID.randomUUID().toString()

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
        const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
