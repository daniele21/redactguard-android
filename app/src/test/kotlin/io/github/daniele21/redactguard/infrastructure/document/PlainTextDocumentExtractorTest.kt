package io.github.daniele21.redactguard.infrastructure.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PlainTextDocumentExtractorTest {
    @Test
    fun `pasted text converges on stable canonical segments`() {
        val document = PlainTextDocumentExtractor.extract("First block\r\n\r\nSecond block")

        assertEquals(1, document.descriptor.pageCount)
        assertEquals("testo-incollato", document.descriptor.displayName)
        assertEquals(listOf("p0001-b0001", "p0001-b0002"), document.segments.map { it.id.value })
        assertEquals(listOf("First block", "Second block"), document.segments.map { it.normalizedText })
    }

    @Test
    fun `blank pasted text fails explicitly`() {
        val error =
            assertThrows(PlainTextExtractionException::class.java) {
                PlainTextDocumentExtractor.extract(" \n\t ")
            }

        assertEquals(PlainTextExtractionFailureCode.EMPTY_TEXT, error.code)
    }

    @Test
    fun `over limit pasted text fails before segmentation`() {
        val error =
            assertThrows(PlainTextExtractionException::class.java) {
                PlainTextDocumentExtractor.extract("a".repeat(PlainTextDocumentExtractor.MAX_CHARACTERS + 1))
            }

        assertEquals(PlainTextExtractionFailureCode.LIMIT_EXCEEDED, error.code)
    }

    @Test
    fun `unsupported controls fail explicitly`() {
        val error =
            assertThrows(PlainTextExtractionException::class.java) {
                PlainTextDocumentExtractor.extract("valid\u0000invalid")
            }

        assertEquals(PlainTextExtractionFailureCode.INVALID_TEXT, error.code)
    }
}
