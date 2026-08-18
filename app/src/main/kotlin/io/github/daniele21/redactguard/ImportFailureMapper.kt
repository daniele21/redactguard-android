package io.github.daniele21.redactguard

import io.github.daniele21.redactguard.domain.failure.ProductFailure
import io.github.daniele21.redactguard.domain.failure.ProductFailureDiagnostic
import io.github.daniele21.redactguard.domain.failure.ProductFailureKind
import io.github.daniele21.redactguard.infrastructure.document.DocumentExtractionException
import io.github.daniele21.redactguard.infrastructure.document.DocumentExtractionFailureCode
import io.github.daniele21.redactguard.infrastructure.document.PlainTextExtractionException
import io.github.daniele21.redactguard.infrastructure.document.PlainTextExtractionFailureCode

/** Application-boundary mapping that preserves every known document ingestion cause. */
internal object ImportFailureMapper {
    fun fromThrowable(
        failure: Throwable,
        operationId: String? = null,
    ): ProductFailure =
        when (failure) {
            is DocumentExtractionException -> fromExtractionFailure(failure, operationId)
            is PlainTextExtractionException -> fromPlainTextCode(failure.code, operationId)
            else -> ProductFailure(ProductFailureKind.UNKNOWN_INTERNAL, operationId)
        }

    private fun fromExtractionFailure(
        failure: DocumentExtractionException,
        operationId: String?,
    ): ProductFailure =
        ProductFailure(
            kind = kindForExtractionCode(failure.code),
            operationId = operationId,
            diagnostic =
                if (failure.parserStep != null || failure.parserErrorType != null) {
                    ProductFailureDiagnostic(
                        step = failure.parserStep,
                        type = failure.parserErrorType,
                    )
                } else {
                    null
                },
        )

    fun fromExtractionCode(
        code: DocumentExtractionFailureCode,
        operationId: String? = null,
    ): ProductFailure = ProductFailure(kindForExtractionCode(code), operationId)

    fun fromPlainTextCode(
        code: PlainTextExtractionFailureCode,
        operationId: String? = null,
    ): ProductFailure =
        ProductFailure(
            kind =
                when (code) {
                    PlainTextExtractionFailureCode.EMPTY_TEXT -> ProductFailureKind.PASTED_TEXT_EMPTY
                    PlainTextExtractionFailureCode.LIMIT_EXCEEDED -> ProductFailureKind.PASTED_TEXT_LIMIT_EXCEEDED
                    PlainTextExtractionFailureCode.INVALID_TEXT -> ProductFailureKind.PASTED_TEXT_INVALID
                },
            operationId = operationId,
        )

    private fun kindForExtractionCode(code: DocumentExtractionFailureCode): ProductFailureKind =
        when (code) {
            DocumentExtractionFailureCode.SOURCE_NOT_FOUND -> ProductFailureKind.SOURCE_NOT_FOUND
            DocumentExtractionFailureCode.SOURCE_UNREADABLE -> ProductFailureKind.SOURCE_UNREADABLE
            DocumentExtractionFailureCode.ENCRYPTED_PDF -> ProductFailureKind.ENCRYPTED_PDF
            DocumentExtractionFailureCode.MALFORMED_PDF -> ProductFailureKind.MALFORMED_PDF
            DocumentExtractionFailureCode.PARSER_FAILED -> ProductFailureKind.PARSER_FAILED
            DocumentExtractionFailureCode.LIMIT_EXCEEDED -> ProductFailureKind.LIMIT_EXCEEDED
            DocumentExtractionFailureCode.EMPTY_PDF -> ProductFailureKind.EMPTY_PDF
            DocumentExtractionFailureCode.IMAGE_ONLY_PDF -> ProductFailureKind.IMAGE_ONLY_PDF
        }
}
