package io.github.daniele21.redactguard

import io.github.daniele21.redactguard.domain.analysis.DocumentAnalysisException
import io.github.daniele21.redactguard.domain.analysis.DocumentAnalysisFailureCode
import io.github.daniele21.redactguard.domain.analysis.LocalAiRuntimeState
import io.github.daniele21.redactguard.domain.failure.ProductFailure
import io.github.daniele21.redactguard.domain.failure.ProductFailureKind

/** Application-boundary mapping for analysis and external Harness connection failures. */
internal object AnalysisFailureMapper {
    fun fromThrowable(
        failure: Throwable,
        operationId: String? = null,
    ): ProductFailure {
        val analysis =
            failure as? DocumentAnalysisException
                ?: return ProductFailure(ProductFailureKind.UNKNOWN_INTERNAL, operationId)
        return fromCode(analysis.code, operationId)
    }

    fun fromCode(
        code: DocumentAnalysisFailureCode,
        operationId: String? = null,
    ): ProductFailure =
        ProductFailure(
            kind =
                when (code) {
                    DocumentAnalysisFailureCode.PLAN_REJECTED -> ProductFailureKind.PLAN_REJECTED
                    DocumentAnalysisFailureCode.INVALID_STRUCTURED_RESULT -> ProductFailureKind.INVALID_STRUCTURED_RESULT
                    DocumentAnalysisFailureCode.INVALID_FINDINGS -> ProductFailureKind.INVALID_FINDINGS
                    DocumentAnalysisFailureCode.HOST_UNAVAILABLE -> ProductFailureKind.HOST_UNAVAILABLE
                    DocumentAnalysisFailureCode.CAPABILITY_INCOMPATIBLE -> ProductFailureKind.CAPABILITY_INCOMPATIBLE
                    DocumentAnalysisFailureCode.CHUNK_FAILED -> ProductFailureKind.CHUNK_FAILED
                    DocumentAnalysisFailureCode.DISCONNECTED -> ProductFailureKind.DISCONNECTED
                    DocumentAnalysisFailureCode.CANCELLED -> ProductFailureKind.CANCELLED
                    DocumentAnalysisFailureCode.RUNTIME_CLEANUP_FAILED -> ProductFailureKind.RUNTIME_CLEANUP_FAILED
                    DocumentAnalysisFailureCode.INTERNAL_FAILURE -> ProductFailureKind.UNKNOWN_INTERNAL
                },
            operationId = operationId,
        )
}

internal object ConnectionFailureMapper {
    fun fromRuntimeState(state: LocalAiRuntimeState): ProductFailure? =
        when (state) {
            LocalAiRuntimeState.CONNECTED,
            LocalAiRuntimeState.CONNECTING,
            -> null

            LocalAiRuntimeState.PERMISSION_DENIED -> ProductFailure(ProductFailureKind.PERMISSION_DENIED)

            LocalAiRuntimeState.INCOMPATIBLE -> ProductFailure(ProductFailureKind.CAPABILITY_INCOMPATIBLE)

            LocalAiRuntimeState.HOST_NOT_INSTALLED -> ProductFailure(ProductFailureKind.HOST_NOT_INSTALLED)

            LocalAiRuntimeState.DISCONNECTED -> ProductFailure(ProductFailureKind.DISCONNECTED)
        }
}
