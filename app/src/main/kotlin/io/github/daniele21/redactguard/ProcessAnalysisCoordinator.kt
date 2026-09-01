package io.github.daniele21.redactguard

import android.app.Application
import io.github.daniele21.redactguard.domain.analysis.AnalysisOperationId
import io.github.daniele21.redactguard.domain.analysis.DocumentAnalysisRequest
import io.github.daniele21.redactguard.domain.analysis.LocalAiExecutionState
import io.github.daniele21.redactguard.domain.analysis.LocalAiRuntimeState
import io.github.daniele21.redactguard.domain.analysis.SequentialDocumentAnalyzer
import io.github.daniele21.redactguard.domain.analysis.ValidatedFinding
import io.github.daniele21.redactguard.domain.pii.PiiDefinition
import io.github.daniele21.redactguard.infrastructure.document.ExtractedDocument
import io.github.daniele21.redactguard.infrastructure.localai.BinderAnalysisRuntimeComposition
import io.github.daniele21.redactguard.infrastructure.localai.LocalAiPresetSelectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-scoped owner for the active Local AI analysis.
 *
 * Sensitive payload/result data is retained in process memory only so Activity/ViewModel recreation
 * can reattach without making Binder or presentation lifecycle the owner of inference. Nothing in
 * this class is persisted across process death.
 */
internal class ProcessAnalysisCoordinator private constructor(
    application: Application,
) {
    private val mutableConnectionState = MutableStateFlow(LocalAiRuntimeState.DISCONNECTED)
    val connectionState: StateFlow<LocalAiRuntimeState> = mutableConnectionState.asStateFlow()

    private val mutableExecutionState = MutableStateFlow<ProcessExecutionSnapshot?>(null)
    val executionState: StateFlow<ProcessExecutionSnapshot?> = mutableExecutionState.asStateFlow()

    private val mutableAnalysisState = MutableStateFlow(ProcessAnalysisSnapshot())
    val analysisState: StateFlow<ProcessAnalysisSnapshot> = mutableAnalysisState.asStateFlow()

    private val runtime =
        BinderAnalysisRuntimeComposition.create(
            context = application,
            onStateChanged = { state -> mutableConnectionState.value = state },
            onExecutionStateChanged = { operationId, state ->
                val current = mutableExecutionState.value
                mutableExecutionState.value =
                    ProcessExecutionSnapshot(
                        operationId = operationId,
                        state = state,
                        revision = (current?.revision ?: 0L) + 1L,
                    )
            },
        )
    private val analyzer = SequentialDocumentAnalyzer(runtime)

    val presetSelectionState: StateFlow<LocalAiPresetSelectionState>
        get() = runtime.presetSelectionState

    init {
        mutableConnectionState.value = runtime.connectionState
        runtime.connect()
    }

    fun connect() {
        runtime.connect()
        mutableConnectionState.value = runtime.connectionState
    }

    fun selectPresetAt(index: Int): Boolean = runtime.selectPresetAt(index)

    fun refreshPresetSelection() = runtime.refreshPresetSelection()

    fun start(
        operationId: AnalysisOperationId,
        document: ExtractedDocument,
        definitions: List<PiiDefinition>,
        startedAtNanos: Long,
    ) {
        require(definitions.isNotEmpty()) { "Process analysis requires selected definitions" }
        val previous = mutableAnalysisState.value
        previous.operationId?.takeIf { previous.phase.isActive }?.let { active ->
            analyzer.cancel(active) {}
        }
        val revision = previous.revision + 1L
        mutableAnalysisState.value =
            ProcessAnalysisSnapshot(
                operationId = operationId,
                phase = ProcessAnalysisPhase.RUNNING,
                document = document,
                definitions = definitions.toList(),
                startedAtNanos = startedAtNanos,
                revision = revision,
            )
        analyzer.analyze(
            operationId = operationId,
            request = DocumentAnalysisRequest(document.segments, definitions),
        ) { result ->
            val current = mutableAnalysisState.value
            if (current.operationId != operationId || !current.phase.isActive) return@analyze
            mutableAnalysisState.value =
                result.fold(
                    onSuccess = { findings ->
                        current.copy(
                            phase = ProcessAnalysisPhase.SUCCEEDED,
                            findings = findings,
                            failure = null,
                            revision = current.revision + 1L,
                        )
                    },
                    onFailure = { failure ->
                        current.copy(
                            phase = ProcessAnalysisPhase.FAILED,
                            findings = null,
                            failure = failure,
                            revision = current.revision + 1L,
                        )
                    },
                )
        }
    }

    fun cancel(operationId: AnalysisOperationId) {
        val current = mutableAnalysisState.value
        if (current.operationId != operationId || !current.phase.isActive) return
        mutableAnalysisState.value =
            current.copy(
                phase = ProcessAnalysisPhase.CANCEL_REQUESTED,
                revision = current.revision + 1L,
            )
        analyzer.cancel(operationId) {
            val latest = mutableAnalysisState.value
            if (latest.operationId != operationId) return@cancel
            mutableAnalysisState.value =
                latest.copy(
                    phase = ProcessAnalysisPhase.CANCELLED,
                    findings = null,
                    failure = null,
                    revision = latest.revision + 1L,
                )
        }
    }

    /** Explicit product reset; unlike ViewModel destruction this is semantic cancellation. */
    fun clearAnalysis() {
        val current = mutableAnalysisState.value
        current.operationId?.takeIf { current.phase.isActive }?.let(::cancel)
        mutableAnalysisState.value = ProcessAnalysisSnapshot(revision = current.revision + 1L)
    }
}

internal enum class ProcessAnalysisPhase {
    IDLE,
    RUNNING,
    CANCEL_REQUESTED,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    ;

    val isActive: Boolean
        get() = this == RUNNING || this == CANCEL_REQUESTED
}

/** Process-local only: [document], [definitions], [findings] and [failure] are never persisted. */
internal data class ProcessAnalysisSnapshot(
    val operationId: AnalysisOperationId? = null,
    val phase: ProcessAnalysisPhase = ProcessAnalysisPhase.IDLE,
    val document: ExtractedDocument? = null,
    val definitions: List<PiiDefinition> = emptyList(),
    val findings: List<ValidatedFinding>? = null,
    val failure: Throwable? = null,
    val startedAtNanos: Long? = null,
    val revision: Long = 0L,
)

internal data class ProcessExecutionSnapshot(
    val operationId: AnalysisOperationId,
    val state: LocalAiExecutionState,
    val revision: Long,
)

internal object ProcessAnalysisCoordinatorRegistry {
    @Volatile
    private var instance: ProcessAnalysisCoordinator? = null

    fun get(application: Application): ProcessAnalysisCoordinator =
        instance
            ?: synchronized(this) {
                instance
                    ?: ProcessAnalysisCoordinator(application).also { created ->
                        instance = created
                    }
            }
}
