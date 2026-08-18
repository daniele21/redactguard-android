package io.github.daniele21.redactguard

import io.github.daniele21.redactguard.domain.failure.ProductFailure
import io.github.daniele21.redactguard.domain.failure.ProductFailureKind
import io.github.daniele21.redactguard.infrastructure.document.DocumentExtractionException
import io.github.daniele21.redactguard.infrastructure.document.DocumentExtractionFailureCode

/** Application-boundary mapping that preserves every known document extraction cause. */
internal object ImportFailureMapper {
    fun fromThrowable(
        failure: Throwable,
        operationId: String? = null,
    ): ProductFailure {
        val extraction =
            failure as? DocumentExtractionException
                ?: return ProductFailure(ProductFailureKind.UNKNOWN_INTERNAL, operationId)
        return fromExtractionCode(extraction.code, operationId)
    }

    fun fromExtractionCode(
        code: DocumentExtractionFailureCode,
        operationId: String? = null,
    ): ProductFailure =
        ProductFailure(
            kind =
                when (code) {
                    DocumentExtractionFailureCode.SOURCE_NOT_FOUND -> ProductFailureKind.SOURCE_NOT_FOUND
                    DocumentExtractionFailureCode.SOURCE_UNREADABLE -> ProductFailureKind.SOURCE_UNREADABLE
                    DocumentExtractionFailureCode.ENCRYPTED_PDF -> ProductFailureKind.ENCRYPTED_PDF
                    DocumentExtractionFailureCode.MALFORMED_PDF -> ProductFailureKind.MALFORMED_PDF
                    DocumentExtractionFailureCode.PARSER_FAILED -> ProductFailureKind.PARSER_FAILED
                    DocumentExtractionFailureCode.LIMIT_EXCEEDED -> ProductFailureKind.LIMIT_EXCEEDED
                    DocumentExtractionFailureCode.EMPTY_PDF -> ProductFailureKind.EMPTY_PDF
                    DocumentExtractionFailureCode.IMAGE_ONLY_PDF -> ProductFailureKind.IMAGE_ONLY_PDF
                },
            operationId = operationId,
        )
}
