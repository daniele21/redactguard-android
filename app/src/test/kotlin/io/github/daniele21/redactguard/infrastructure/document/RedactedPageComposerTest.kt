package io.github.daniele21.redactguard.infrastructure.document

import io.github.daniele21.redactguard.domain.document.DocumentDescriptor
import io.github.daniele21.redactguard.domain.document.DocumentSegment
import io.github.daniele21.redactguard.domain.document.SegmentId
import io.github.daniele21.redactguard.domain.redaction.RedactionPlan
import io.github.daniele21.redactguard.domain.redaction.RenderedSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RedactedPageComposerTest {
    @Test
    fun `renders normalized pages in page and block order`() {
        val second = segment(page = 0, block = 1, text = "second")
        val first = segment(page = 0, block = 0, text = "secret")
        val third = segment(page = 1, block = 0, text = "third")
        val plan =
            RedactionPlan(
                renderedSegments =
                    listOf(
                        RenderedSegment(second.id, "second"),
                        RenderedSegment(first.id, "[EMAIL_1]"),
                        RenderedSegment(third.id, "third"),
                    ),
                replacements = emptyList(),
                acceptedCount = 0,
                ignoredCount = 0,
            )

        val pages =
            RedactedPageComposer.compose(
                DocumentDescriptor("fixture.pdf", 2),
                listOf(second, third, first),
                plan,
            )

        assertEquals(listOf("[EMAIL_1]\n\nsecond", "third"), pages)
    }

    @Test
    fun `missing rendered segment fails closed`() {
        val source = segment(page = 0, block = 0, text = "secret")
        val plan = RedactionPlan(emptyList(), emptyList(), acceptedCount = 0, ignoredCount = 0)

        val failure =
            assertThrows(PdfExportException::class.java) {
                RedactedPageComposer.compose(DocumentDescriptor("fixture.pdf", 1), listOf(source), plan)
            }

        assertEquals(PdfExportFailureCode.SOURCE_MISMATCH, failure.code)
    }

    private fun segment(
        page: Int,
        block: Int,
        text: String,
    ) = DocumentSegment(
        id = SegmentId.fromIndices(page, block),
        pageIndex = page,
        blockIndex = block,
        normalizedText = text,
    )
}
