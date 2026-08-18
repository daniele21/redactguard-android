package io.github.daniele21.redactguard.infrastructure.document

import io.github.daniele21.redactguard.domain.document.DocumentDescriptor
import io.github.daniele21.redactguard.domain.document.DocumentSegment
import io.github.daniele21.redactguard.domain.document.PdfSegmenter
import java.io.IOException

internal data class ExtractedDocument(
    val descriptor: DocumentDescriptor,
    val segments: List<DocumentSegment>,
)

internal enum class DocumentExtractionFailureCode {
    SOURCE_NOT_FOUND,
    SOURCE_UNREADABLE,
    ENCRYPTED_PDF,
    MALFORMED_PDF,
    PARSER_FAILED,
    LIMIT_EXCEEDED,
    EMPTY_PDF,
    IMAGE_ONLY_PDF,
}

internal class DocumentExtractionException(
    val code: DocumentExtractionFailureCode,
    val parserErrorType: String? = null,
    val parserStep: String? = null,
) : IOException("Document extraction failed: $code") {
    init {
        require(parserErrorType == null || SAFE_DIAGNOSTIC_ID.matches(parserErrorType))
        require(parserStep == null || SAFE_DIAGNOSTIC_ID.matches(parserStep))
    }

    companion object {
        private val SAFE_DIAGNOSTIC_ID = Regex("^[A-Za-z0-9._:+-]{1,96}$")
    }
}

/** Suspended application adapter; coroutine cancellation propagates into isolated-parser unbinding/descriptor cleanup. */
internal class AndroidDocumentExtractor(
    private val sourceResolver: DocumentSourceResolver,
    private val reader: PdfTextReader,
) {
    suspend fun extract(sourceRef: DocumentSourceRef): ExtractedDocument {
        val source =
            sourceResolver.resolve(sourceRef)
                ?: throw DocumentExtractionException(DocumentExtractionFailureCode.SOURCE_NOT_FOUND)
        val parsed =
            try {
                reader.read(source.locator)
            } catch (exception: PdfParserException) {
                throw DocumentExtractionException(
                    code = mapParserFailure(exception.parserErrorType),
                    parserErrorType = exception.parserErrorType,
                    parserStep = exception.parserStep,
                )
            } catch (_: SecurityException) {
                throw DocumentExtractionException(DocumentExtractionFailureCode.SOURCE_UNREADABLE)
            } catch (_: IOException) {
                throw DocumentExtractionException(DocumentExtractionFailureCode.PARSER_FAILED)
            }

        if (parsed.truncated) throw DocumentExtractionException(DocumentExtractionFailureCode.LIMIT_EXCEEDED)
        if (parsed.pageCount <= 0) throw DocumentExtractionException(DocumentExtractionFailureCode.EMPTY_PDF)
        val segments = PdfSegmenter.segment(parsed.pages)
        if (segments.isEmpty()) throw DocumentExtractionException(DocumentExtractionFailureCode.IMAGE_ONLY_PDF)
        return ExtractedDocument(DocumentDescriptor(source.displayName, parsed.pageCount), segments)
    }

    private fun mapParserFailure(errorType: String): DocumentExtractionFailureCode =
        when (errorType) {
            "InvalidPasswordException" -> DocumentExtractionFailureCode.ENCRYPTED_PDF
            "InvalidPDF", "ParseException" -> DocumentExtractionFailureCode.MALFORMED_PDF
            else -> DocumentExtractionFailureCode.PARSER_FAILED
        }
}
