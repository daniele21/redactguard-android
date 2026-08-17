package io.github.daniele21.redactguard.domain.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfSegmenterTest {
    @Test
    fun `normalizes line endings and creates stable blocks`() {
        val segments =
            PdfSegmenter.segment(
                listOf(PdfPageText(0, "First line  \r\nSecond line\r\n\r\nThird block\n")),
            )
        assertEquals(2, segments.size)
        assertEquals("p0001-b0001", segments[0].id.value)
        assertEquals("First line\nSecond line", segments[0].normalizedText)
        assertEquals("p0001-b0002", segments[1].id.value)
        assertEquals("Third block", segments[1].normalizedText)
    }

    @Test
    fun `orders pages deterministically skips blanks and keeps text private in diagnostics`() {
        val page = PdfPageText(2, "Page three secret")
        assertFalse(page.toString().contains("secret"))
        val segments =
            PdfSegmenter.segment(
                listOf(page, PdfPageText(0, "  \n\t"), PdfPageText(1, "Page two")),
            )
        assertEquals(listOf(1, 2), segments.map(DocumentSegment::pageIndex))
        assertEquals(listOf("Page two", "Page three secret"), segments.map(DocumentSegment::normalizedText))
    }

    @Test
    fun `unsafe extracted controls fail closed`() {
        assertTrue(runCatching { PdfSegmenter.segment(listOf(PdfPageText(0, "safe\u0000unsafe"))) }.isFailure)
    }
}
