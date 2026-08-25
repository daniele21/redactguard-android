package io.github.daniele21.redactguard.domain.analysis

@JvmInline
internal value class AnalysisOperationId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Analysis operation ID must not be blank" }
        require(value.length <= 96) { "Analysis operation ID is too long" }
    }
}

internal enum class AnalysisRuntimeFailureCode {
    HOST_UNAVAILABLE,
    CAPABILITY_INCOMPATIBLE,
    GENERATION_FAILED,
    DISCONNECTED,
    CANCELLED,
    INTERNAL_FAILURE,
}

/** Privacy-safe low-level identity. It must never contain exception messages or user/model content. */
internal data class AnalysisRuntimeDiagnostic(
    val step: String,
    val type: String,
) {
    init {
        require(SAFE_IDENTITY.matches(step)) { "Runtime diagnostic step contains unsupported characters" }
        require(SAFE_IDENTITY.matches(type)) { "Runtime diagnostic type contains unsupported characters" }
    }

    private companion object {
        val SAFE_IDENTITY = Regex("^[A-Za-z0-9._:+-]{1,96}$")
    }
}

internal class AnalysisRuntimeException(
    val code: AnalysisRuntimeFailureCode,
    val diagnostic: AnalysisRuntimeDiagnostic? = null,
) : RuntimeException("RedactGuard analysis runtime failed: $code")

/**
 * App-owned Local AI boundary. Harness contract, Binder, model and preset types must not cross this
 * interface.
 */
internal interface AnalysisRuntimePort {
    fun prepare(
        operationId: AnalysisOperationId,
        onResult: (Result<AnalysisLimits>) -> Unit,
    )

    fun generate(
        operationId: AnalysisOperationId,
        chunk: AnalysisChunk,
        onResult: (Result<String>) -> Unit,
    )

    fun cancel(
        operationId: AnalysisOperationId,
        onCancelled: () -> Unit,
    )

    fun close(operationId: AnalysisOperationId)
}
