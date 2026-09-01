package io.github.daniele21.redactguard.domain.analysis

import java.util.LinkedHashMap
import java.util.concurrent.atomic.AtomicLong

@JvmInline
internal value class AnalysisJobId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Analysis job ID must not be blank" }
        require(value.length <= 96) { "Analysis job ID is too long" }
    }
}

internal enum class AnalysisJobState {
    ACTIVE,
    CANCEL_REQUESTED,
    SUCCEEDED,
    FAILED,
    CANCELLED,
}

internal data class AnalysisJobSnapshot(
    val jobId: AnalysisJobId,
    val operationId: AnalysisOperationId,
    val state: AnalysisJobState,
    val revision: Long,
    val failureCode: DocumentAnalysisFailureCode? = null,
) {
    val isTerminal: Boolean
        get() =
            state == AnalysisJobState.SUCCEEDED ||
                state == AnalysisJobState.FAILED ||
                state == AnalysisJobState.CANCELLED
}

internal sealed interface AnalysisJobOutcome {
    data class Success(
        val findings: List<ValidatedFinding>,
    ) : AnalysisJobOutcome

    data class Failure(
        val failure: Throwable,
    ) : AnalysisJobOutcome

    data object Cancelled : AnalysisJobOutcome
}

internal fun interface AnalysisJobSubscription : AutoCloseable {
    override fun close()
}

/** Narrow engine boundary so product job lifetime can be tested independently from runtime/Binder details. */
internal interface AnalysisJobEngine {
    fun start(
        operationId: AnalysisOperationId,
        request: DocumentAnalysisRequest,
        onResult: (Result<List<ValidatedFinding>>) -> Unit,
    )

    fun cancel(
        operationId: AnalysisOperationId,
        onCancelled: () -> Unit,
    )
}

internal class SequentialAnalysisJobEngine(
    private val analyzer: SequentialDocumentAnalyzer,
) : AnalysisJobEngine {
    override fun start(
        operationId: AnalysisOperationId,
        request: DocumentAnalysisRequest,
        onResult: (Result<List<ValidatedFinding>>) -> Unit,
    ) {
        analyzer.analyze(operationId, request, onResult)
    }

    override fun cancel(
        operationId: AnalysisOperationId,
        onCancelled: () -> Unit,
    ) {
        analyzer.cancel(operationId, onCancelled)
    }
}

/**
 * Process-local owner for one product analysis job. Observer detach never cancels execution, and a
 * later observer can attach to the same stable job identity. Sensitive findings remain in memory
 * only and are never part of the privacy-safe snapshot.
 */
internal class ProcessLocalAnalysisJobOwner(
    private val engine: AnalysisJobEngine,
) {
    private data class Entry(
        var snapshot: AnalysisJobSnapshot,
        var outcome: AnalysisJobOutcome? = null,
    )

    private val lock = Any()
    private val observerIds = AtomicLong()
    private val observers = LinkedHashMap<Long, Pair<AnalysisJobId, (AnalysisJobSnapshot) -> Unit>>()
    private var entry: Entry? = null

    fun start(
        jobId: AnalysisJobId,
        operationId: AnalysisOperationId,
        request: DocumentAnalysisRequest,
    ): AnalysisJobSnapshot {
        val started =
            synchronized(lock) {
                val current = entry?.snapshot
                check(current == null || current.isTerminal) { "An analysis job is already active" }
                AnalysisJobSnapshot(
                    jobId = jobId,
                    operationId = operationId,
                    state = AnalysisJobState.ACTIVE,
                    revision = 0,
                ).also { entry = Entry(it) }
            }
        notifyObservers(started)
        engine.start(operationId, request) { result -> complete(jobId, result) }
        return snapshot(jobId) ?: started
    }

    fun snapshot(jobId: AnalysisJobId): AnalysisJobSnapshot? = synchronized(lock) { entry?.snapshot?.takeIf { it.jobId == jobId } }

    fun currentSnapshot(): AnalysisJobSnapshot? = synchronized(lock) { entry?.snapshot }

    fun outcome(jobId: AnalysisJobId): AnalysisJobOutcome? =
        synchronized(lock) {
            entry?.takeIf { it.snapshot.jobId == jobId }?.outcome
        }

    fun observe(
        jobId: AnalysisJobId,
        observer: (AnalysisJobSnapshot) -> Unit,
    ): AnalysisJobSubscription {
        val observerId = observerIds.incrementAndGet()
        val current =
            synchronized(lock) {
                observers[observerId] = jobId to observer
                entry?.snapshot?.takeIf { it.jobId == jobId }
            }
        current?.let(observer)
        return AnalysisJobSubscription {
            synchronized(lock) { observers.remove(observerId) }
        }
    }

    fun cancel(
        jobId: AnalysisJobId,
        onCancelled: () -> Unit = {},
    ) {
        val cancelling =
            synchronized(lock) {
                val current = entry?.snapshot?.takeIf { it.jobId == jobId }
                if (
                    current == null ||
                    current.isTerminal ||
                    current.state == AnalysisJobState.CANCEL_REQUESTED
                ) {
                    null
                } else {
                    current
                        .copy(
                            state = AnalysisJobState.CANCEL_REQUESTED,
                            revision = current.revision + 1,
                        ).also { entry?.snapshot = it }
                }
            }
        if (cancelling == null) {
            onCancelled()
            return
        }
        notifyObservers(cancelling)
        engine.cancel(cancelling.operationId) {
            val cancelled = transitionCancelled(jobId)
            if (cancelled != null) notifyObservers(cancelled)
            onCancelled()
        }
    }

    fun clearTerminal(jobId: AnalysisJobId) {
        synchronized(lock) {
            val current = entry?.snapshot ?: return
            if (current.jobId == jobId && current.isTerminal) entry = null
        }
    }

    private fun complete(
        jobId: AnalysisJobId,
        result: Result<List<ValidatedFinding>>,
    ) {
        val completed =
            synchronized(lock) {
                val current = entry?.snapshot?.takeIf { it.jobId == jobId } ?: return
                if (current.isTerminal || current.state == AnalysisJobState.CANCEL_REQUESTED) return
                val failure = result.exceptionOrNull()
                val state = if (failure == null) AnalysisJobState.SUCCEEDED else AnalysisJobState.FAILED
                val failureCode = (failure as? DocumentAnalysisException)?.code
                val next =
                    current.copy(
                        state = state,
                        revision = current.revision + 1,
                        failureCode = failureCode,
                    )
                entry?.snapshot = next
                entry?.outcome =
                    if (failure == null) {
                        AnalysisJobOutcome.Success(result.getOrThrow())
                    } else {
                        AnalysisJobOutcome.Failure(failure)
                    }
                next
            }
        notifyObservers(completed)
    }

    private fun transitionCancelled(jobId: AnalysisJobId): AnalysisJobSnapshot? =
        synchronized(lock) {
            val current = entry?.snapshot?.takeIf { it.jobId == jobId } ?: return@synchronized null
            if (current.isTerminal) return@synchronized current
            current
                .copy(
                    state = AnalysisJobState.CANCELLED,
                    revision = current.revision + 1,
                    failureCode = DocumentAnalysisFailureCode.CANCELLED,
                ).also { next ->
                    entry?.snapshot = next
                    entry?.outcome = AnalysisJobOutcome.Cancelled
                }
        }

    private fun notifyObservers(snapshot: AnalysisJobSnapshot) {
        val callbacks =
            synchronized(lock) {
                observers.values
                    .filter { (jobId, _) -> jobId == snapshot.jobId }
                    .map { (_, callback) -> callback }
            }
        callbacks.forEach { callback -> callback(snapshot) }
    }
}
