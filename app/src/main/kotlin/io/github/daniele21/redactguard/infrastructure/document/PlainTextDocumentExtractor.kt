package io.github.daniele21.redactguard.infrastructure.document

import io.github.daniele21.redactguard.domain.document.DocumentDescriptor
import io.github.daniele21.redactguard.domain.document.DocumentPageText
import io.github.daniele21.redactguard.domain.document.DocumentTextSegmenter

internal enum class PlainTextExtractionFailureCode {
    EMPTY_TEXT,
    LIMIT_EXCEEDED,
    INVALID_TEXT,
}

internal class PlainTextExtractionException(
    val code: PlainTextExtractionFailureCode,
) : IllegalArgumentException("Plain-text extraction failed: $code")

/** Pure process-local adapter from pasted text to the same canonical document contract used by PDFs. */
internal object PlainTextDocumentExtractor {
    fun extract(text: String): ExtractedDocument {
        if (text.isBlank()) throw PlainTextExtractionException(PlainTextExtractionFailureCode.EMPTY_TEXT)
        if (text.length > MAX_CHARACTERS) {
            throw PlainTextExtractionException(PlainTextExtractionFailureCode.LIMIT_EXCEEDED)
        }

        val segments =
            try {
                DocumentTextSegmenter.segment(listOf(DocumentPageText(pageIndex = 0, text = text)))
            } catch (_: IllegalArgumentException) {
                throw PlainTextExtractionException(PlainTextExtractionFailureCode.INVALID_TEXT)
            }
        if (segments.isEmpty()) throw PlainTextExtractionException(PlainTextExtractionFailureCode.EMPTY_TEXT)

        return ExtractedDocument(
            descriptor = DocumentDescriptor(displayName = DEFAULT_DISPLAY_NAME, pageCount = 1),
            segments = segments,
        )
    }

    const val MAX_CHARACTERS = 1_000_000
    private const val DEFAULT_DISPLAY_NAME = "testo-incollato"
}
