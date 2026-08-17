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
) : IOException("Document extraction failed: $code")

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
                reader.read(source.uri)
            } catch (exception: PdfParserException) {
                throw DocumentExtractionException(mapParserFailure(exception.parserErrorType))
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
            "IOException", "InvalidPDF", "ParseException" -> DocumentExtractionFailureCode.MALFORMED_PDF
            else -> DocumentExtractionFailureCode.PARSER_FAILED
        }
}
