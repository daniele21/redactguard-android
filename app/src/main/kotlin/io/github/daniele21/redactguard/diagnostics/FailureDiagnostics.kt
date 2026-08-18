package io.github.daniele21.redactguard.diagnostics

import io.github.daniele21.redactguard.domain.failure.FailureCategory
import io.github.daniele21.redactguard.domain.failure.FailureStage
import io.github.daniele21.redactguard.domain.failure.ProductFailure
import java.util.ArrayDeque

/** Privacy-safe metadata accepted by failure diagnostics. No arbitrary user-content field exists. */
internal data class FailureDiagnosticContext(
    val durationMs: Long? = null,
    val pageCount: Int? = null,
    val appVersion: String? = null,
    val buildId: String? = null,
    val sourceRevision: String? = null,
) {
    init {
        require(durationMs == null || durationMs >= 0) { "durationMs must be non-negative" }
        require(pageCount == null || pageCount >= 0) { "pageCount must be non-negative" }
        requireIdentity("appVersion", appVersion)
        requireIdentity("buildId", buildId)
        requireIdentity("sourceRevision", sourceRevision)
    }

    private fun requireIdentity(
        name: String,
        value: String?,
    ) {
        if (value == null) return
        require(SAFE_IDENTITY.matches(value)) { "$name contains unsupported diagnostic characters" }
    }

    private companion object {
        val SAFE_IDENTITY = Regex("^[A-Za-z0-9._:+-]{1,96}$")
    }
}

internal data class FailureDiagnosticEvent(
    val code: String,
    val stage: FailureStage,
    val category: FailureCategory,
    val retryable: Boolean,
    val operationId: String?,
    val lowLevelStep: String?,
    val lowLevelType: String?,
    val durationMs: Long?,
    val pageCount: Int?,
    val appVersion: String?,
    val buildId: String?,
    val sourceRevision: String?,
) {
    companion object {
        fun from(
            failure: ProductFailure,
            context: FailureDiagnosticContext = FailureDiagnosticContext(),
        ): FailureDiagnosticEvent =
            FailureDiagnosticEvent(
                code = failure.code,
                stage = failure.kind.stage,
                category = failure.kind.category,
                retryable = failure.kind.retryable,
                operationId = failure.operationId,
                lowLevelStep = failure.diagnostic?.step,
                lowLevelType = failure.diagnostic?.type,
                durationMs = context.durationMs,
                pageCount = context.pageCount,
                appVersion = context.appVersion,
                buildId = context.buildId,
                sourceRevision = context.sourceRevision,
            )
    }
}

/**
 * Process-local bounded diagnostic retention. The store intentionally cannot accept document text,
 * filenames, prompts, model output, findings or raw transport payloads.
 */
internal class BoundedFailureDiagnosticStore(
    private val maxEvents: Int = DEFAULT_MAX_EVENTS,
) {
    private val events = ArrayDeque<FailureDiagnosticEvent>(maxEvents)

    init {
        require(maxEvents in 1..MAX_ALLOWED_EVENTS) { "maxEvents must be within the bounded diagnostic budget" }
    }

    @Synchronized
    fun record(event: FailureDiagnosticEvent) {
        while (events.size >= maxEvents) events.removeFirst()
        events.addLast(event)
    }

    @Synchronized
    fun snapshot(): List<FailureDiagnosticEvent> = events.toList()

    @Synchronized
    fun clear() {
        events.clear()
    }

    private companion object {
        const val DEFAULT_MAX_EVENTS = 64
        const val MAX_ALLOWED_EVENTS = 256
    }
}
