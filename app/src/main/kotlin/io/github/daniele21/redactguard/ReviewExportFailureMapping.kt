package io.github.daniele21.redactguard

import io.github.daniele21.redactguard.domain.failure.ProductFailure
import io.github.daniele21.redactguard.domain.failure.ProductFailureKind
import io.github.daniele21.redactguard.domain.redaction.RedactionPlanFailureCode
import io.github.daniele21.redactguard.infrastructure.document.PdfExportException
import io.github.daniele21.redactguard.infrastructure.document.PdfExportFailureCode
import io.github.daniele21.redactguard.ui.ReviewProjectionFailureCode

internal object ReviewFailureMapper {
    fun fromPlanCode(
        code: RedactionPlanFailureCode,
        operationId: String? = null,
    ): ProductFailure =
        ProductFailure(
            kind =
                when (code) {
                    RedactionPlanFailureCode.PENDING_DECISION -> ProductFailureKind.REVIEW_PENDING_DECISION
                    RedactionPlanFailureCode.UNKNOWN_SEGMENT -> ProductFailureKind.REVIEW_UNKNOWN_SEGMENT
                    RedactionPlanFailureCode.MISSING_DEFINITION -> ProductFailureKind.REVIEW_MISSING_DEFINITION
                    RedactionPlanFailureCode.SOURCE_MISMATCH -> ProductFailureKind.REVIEW_SOURCE_MISMATCH
                    RedactionPlanFailureCode.DUPLICATE_OCCURRENCE -> ProductFailureKind.REVIEW_DUPLICATE_OCCURRENCE
                    RedactionPlanFailureCode.OVERLAP_CONFLICT -> ProductFailureKind.REVIEW_OVERLAP_CONFLICT
                },
            operationId = operationId,
        )

    fun fromProjectionCode(
        code: ReviewProjectionFailureCode,
        operationId: String? = null,
    ): ProductFailure =
        ProductFailure(
            kind =
                when (code) {
                    ReviewProjectionFailureCode.DUPLICATE_OCCURRENCE -> ProductFailureKind.REVIEW_DUPLICATE_OCCURRENCE
                    ReviewProjectionFailureCode.DUPLICATE_DEFINITION -> ProductFailureKind.REVIEW_DUPLICATE_DEFINITION
                    ReviewProjectionFailureCode.MISSING_DEFINITION -> ProductFailureKind.REVIEW_MISSING_DEFINITION
                    ReviewProjectionFailureCode.UNKNOWN_REVEAL_OCCURRENCE ->
                        ProductFailureKind.REVIEW_UNKNOWN_REVEAL_OCCURRENCE
                },
            operationId = operationId,
        )
}

internal object ExportFailureMapper {
    fun fromThrowable(
        failure: Throwable,
        operationId: String? = null,
    ): ProductFailure {
        val export =
            failure as? PdfExportException
                ?: return ProductFailure(ProductFailureKind.UNKNOWN_INTERNAL, operationId)
        return fromCode(export.code, operationId)
    }

    fun fromCode(
        code: PdfExportFailureCode,
        operationId: String? = null,
    ): ProductFailure =
        ProductFailure(
            kind =
                when (code) {
                    PdfExportFailureCode.DESTINATION_UNWRITABLE -> ProductFailureKind.DESTINATION_UNWRITABLE
                    PdfExportFailureCode.SOURCE_MISMATCH -> ProductFailureKind.SOURCE_MISMATCH
                    PdfExportFailureCode.OUTPUT_LIMIT_EXCEEDED -> ProductFailureKind.OUTPUT_LIMIT_EXCEEDED
                    PdfExportFailureCode.WRITER_FAILED -> ProductFailureKind.WRITER_FAILED
                },
            operationId = operationId,
        )
}
