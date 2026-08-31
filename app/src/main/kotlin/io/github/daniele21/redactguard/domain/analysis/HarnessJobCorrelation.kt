package io.github.daniele21.redactguard.domain.analysis

@JvmInline
internal value class HarnessClientRequestId(
    val value: String,
) {
    init {
        require(SAFE_ID.matches(value)) { "Harness client request ID must be privacy-safe" }
    }

    private companion object {
        val SAFE_ID = Regex("^[A-Za-z0-9._:-]{1,128}$")
    }
}

@JvmInline
internal value class HarnessLogicalJobId(
    val value: String,
) {
    init {
        require(SAFE_ID.matches(value)) { "Harness logical job ID must be privacy-safe" }
    }

    private companion object {
        val SAFE_ID = Regex("^[A-Za-z0-9._:-]{1,96}$")
    }
}

@JvmInline
internal value class AnalysisUnitId(
    val value: String,
) {
    init {
        require(SAFE_ID.matches(value)) { "Analysis unit ID must be privacy-safe" }
    }

    private companion object {
        val SAFE_ID = Regex("^[A-Za-z0-9._:-]{1,48}$")
    }
}

internal enum class RedactionJobOperation(
    val wireName: String,
) {
    ANALYSIS("analysis"),
}

internal object HarnessClientRequestIds {
    fun forOperation(
        redactionJobId: RedactionJobId,
        operation: RedactionJobOperation,
        unitId: AnalysisUnitId,
        contractVersion: Int,
    ): HarnessClientRequestId {
        require(contractVersion >= 1) { "Contract version must be positive" }
        return HarnessClientRequestId(
            "${redactionJobId.value}:${operation.wireName}:${unitId.value}:v$contractVersion",
        )
    }
}

/**
 * Privacy-safe correlation state for a Harness logical job. It contains no document text, PII,
 * prompt, generated output, private path or raw Binder payload.
 */
internal data class HarnessJobCorrelation(
    val clientRequestId: HarnessClientRequestId,
    val harnessJobId: HarnessLogicalJobId? = null,
    val lastConsumedRevision: Long = -1,
    val cancelRequested: Boolean = false,
) {
    init {
        require(lastConsumedRevision >= -1) { "Harness revision must be -1 or non-negative" }
    }

    fun attach(jobId: HarnessLogicalJobId): HarnessJobCorrelation {
        require(harnessJobId == null || harnessJobId == jobId) {
            "Harness logical job identity cannot change for one client request"
        }
        return if (harnessJobId == jobId) this else copy(harnessJobId = jobId)
    }

    fun consumeRevision(revision: Long): HarnessJobCorrelation {
        require(revision >= 0) { "Harness revision must be non-negative" }
        return if (revision <= lastConsumedRevision) this else copy(lastConsumedRevision = revision)
    }

    fun requestCancel(): HarnessJobCorrelation =
        if (cancelRequested) this else copy(cancelRequested = true)
}
