package io.github.daniele21.redactguard

import android.content.Context
import io.github.daniele21.redactguard.domain.analysis.AnalysisJobId
import io.github.daniele21.redactguard.domain.analysis.AnalysisJobOutcome
import io.github.daniele21.redactguard.domain.analysis.AnalysisJobSnapshot
import io.github.daniele21.redactguard.domain.analysis.AnalysisJobSubscription
import io.github.daniele21.redactguard.domain.analysis.AnalysisOperationId
import io.github.daniele21.redactguard.domain.analysis.DocumentAnalysisRequest
import io.github.daniele21.redactguard.domain.analysis.LocalAiExecutionState
import io.github.daniele21.redactguard.domain.analysis.LocalAiRuntimeState
import io.github.daniele21.redactguard.domain.analysis.ProcessLocalAnalysisJobOwner
import io.github.daniele21.redactguard.domain.analysis.SequentialAnalysisJobEngine
import io.github.daniele21.redactguard.domain.analysis.SequentialDocumentAnalyzer
import io.github.daniele21.redactguard.domain.pii.DefinitionSelectionState
import io.github.daniele21.redactguard.domain.pii.PiiDefinition
import io.github.daniele21.redactguard.infrastructure.document.ExtractedDocument
import io.github.daniele21.redactguard.infrastructure.localai.BinderAnalysisRuntimeComposition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

internal data class ProductAnalysisExecutionUpdate(
    val operationId: AnalysisOperationId,
    val state: LocalAiExecutionState,
)

/** Sensitive reattach context. Deliberately bounded to the single process-local product job. */
internal data class ProductAnalysisContext(
    val jobId: AnalysisJobId,
    val document: ExtractedDocument,
    val definitionState: DefinitionSelectionState,
    val startedAtNanos: Long,
) {
    val analysisDefinitions: List<PiiDefinition>
        get() = definitionState.selectedDefinitions

    /** Compatibility view used while the product UI migrates to full selection-state reattach. */
    val definitions: List<PiiDefinition>
        get() = analysisDefinitions
}

/**
 * Process-local owner for the Local AI runtime and active RedactGuard analysis.
 *
 * Activity/ViewModel lifetime only owns observation. Runtime execution, stable job identity and the
 * minimum sensitive context required to reattach remain in memory until terminal consumption or
 * process death. Nothing here is durably persisted.
 */
internal class ProcessLocalProductAnalysisOwner private constructor(
    val runtime: BinderAnalysisRuntimeComposition,
    private val jobs: ProcessLocalAnalysisJobOwner,
    private val mutableConnectionState: MutableStateFlow<LocalAiRuntimeState>,
    private val mutableExecutionUpdate: MutableStateFlow<ProductAnalysisExecutionUpdate?>,
) {
    private val lock = Any()
    private var analysisContext: ProductAnalysisContext? = null

    val connectionState: StateFlow<LocalAiRuntimeState> = mutableConnectionState.asStateFlow()
    val executionUpdate: StateFlow<ProductAnalysisExecutionUpdate?> = mutableExecutionUpdate.asStateFlow()

    fun connect() {
        runtime.connect()
        mutableConnectionState.value = runtime.connectionState
    }

    fun start(
        document: ExtractedDocument,
        definitionState: DefinitionSelectionState,
        startedAtNanos: Long = System.nanoTime(),
    ): AnalysisJobSnapshot {
        require(definitionState.selectedDefinitions.isNotEmpty()) {
            "Analysis definitions must not be empty"
        }
        val jobId = AnalysisJobId(UUID.randomUUID().toString())
        val operationId = AnalysisOperationId(UUID.randomUUID().toString())
        val safeState =
            DefinitionSelectionState(
                definitions = definitionState.definitions.toList(),
                selectedIds = definitionState.selectedIds.toSet(),
            )
        val context = ProductAnalysisContext(jobId, document, safeState, startedAtNanos)
        synchronized(lock) {
            val current = jobs.currentSnapshot()
            check(current == null || current.isTerminal) { "An analysis job is already active" }
            analysisContext = context
        }
        return try {
            jobs.start(
                jobId = jobId,
                operationId = operationId,
                request = DocumentAnalysisRequest(document.segments, context.analysisDefinitions),
            )
        } catch (failure: Throwable) {
            synchronized(lock) {
                if (analysisContext?.jobId == jobId) analysisContext = null
            }
            throw failure
        }
    }

    /** Transitional overload; callers should migrate to the full DefinitionSelectionState overload. */
    fun start(
        document: ExtractedDocument,
        definitions: List<PiiDefinition>,
        startedAtNanos: Long = System.nanoTime(),
    ): AnalysisJobSnapshot =
        start(
            document = document,
            definitionState =
                DefinitionSelectionState(
                    definitions = definitions.toList(),
                    selectedIds = definitions.mapTo(linkedSetOf(), PiiDefinition::id),
                ),
            startedAtNanos = startedAtNanos,
        )

    fun currentSnapshot(): AnalysisJobSnapshot? = jobs.currentSnapshot()

    fun observe(
        jobId: AnalysisJobId,
        observer: (AnalysisJobSnapshot) -> Unit,
    ): AnalysisJobSubscription = jobs.observe(jobId, observer)

    fun outcome(jobId: AnalysisJobId): AnalysisJobOutcome? = jobs.outcome(jobId)

    fun context(jobId: AnalysisJobId): ProductAnalysisContext? = synchronized(lock) { analysisContext?.takeIf { it.jobId == jobId } }

    fun cancel(
        jobId: AnalysisJobId,
        onCancelled: () -> Unit = {},
    ) = jobs.cancel(jobId, onCancelled)

    fun consumeTerminal(jobId: AnalysisJobId) {
        val snapshot = jobs.snapshot(jobId) ?: return
        if (!snapshot.isTerminal) return
        jobs.clearTerminal(jobId)
        synchronized(lock) {
            if (analysisContext?.jobId == jobId) analysisContext = null
        }
    }

    companion object {
        @Volatile
        private var instance: ProcessLocalProductAnalysisOwner? = null

        fun get(context: Context): ProcessLocalProductAnalysisOwner =
            instance
                ?: synchronized(this) {
                    instance ?: create(context.applicationContext).also { instance = it }
                }

        private fun create(context: Context): ProcessLocalProductAnalysisOwner {
            val connectionState = MutableStateFlow(LocalAiRuntimeState.DISCONNECTED)
            val executionUpdate = MutableStateFlow<ProductAnalysisExecutionUpdate?>(null)
            val runtime =
                BinderAnalysisRuntimeComposition.create(
                    context = context,
                    onStateChanged = { state -> connectionState.value = state },
                    onExecutionStateChanged = { operationId, state ->
                        executionUpdate.value = ProductAnalysisExecutionUpdate(operationId, state)
                    },
                )
            val jobs =
                ProcessLocalAnalysisJobOwner(
                    SequentialAnalysisJobEngine(SequentialDocumentAnalyzer(runtime)),
                )
            return ProcessLocalProductAnalysisOwner(
                runtime = runtime,
                jobs = jobs,
                mutableConnectionState = connectionState,
                mutableExecutionUpdate = executionUpdate,
            ).also { owner ->
                connectionState.value = runtime.connectionState
                owner.connect()
            }
        }
    }
}
