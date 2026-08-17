package io.github.daniele21.redactguard.domain.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentDomainTest {
    @Test
    fun `segment identity is stable and one based in serialized form`() {
        assertEquals("p0001-b0001", SegmentId.fromIndices(0, 0).value)
        assertEquals("p0012-b0034", SegmentId.fromIndices(11, 33).value)
        assertEquals(SegmentId.fromIndices(11, 33), SegmentId.parse("p0012-b0034"))
    }

    @Test
    fun `segment requires matching identity and redacts content from debug output`() {
        assertTrue(
            runCatching {
                DocumentSegment(SegmentId.parse("p0002-b0001"), 0, 0, "Mario Rossi")
            }.isFailure,
        )
        val segment = DocumentSegment(SegmentId.fromIndices(0, 0), 0, 0, "Mario Rossi, CF RSSMRA80A01H501U")
        assertFalse(segment.toString().contains("Mario Rossi"))
        assertFalse(segment.toString().contains("RSSMRA80A01H501U"))
    }

    @Test
    fun `descriptor and source ranges enforce domain invariants`() {
        val descriptor = DocumentDescriptor("Mario-referto.pdf", 1)
        assertFalse(descriptor.toString().contains("Mario-referto"))
        val first = SourceRange(2, 8)
        val second = SourceRange(7, 10)
        val separate = SourceRange(8, 11)
        assertEquals(6, first.length)
        assertTrue(first.overlaps(second))
        assertFalse(first.overlaps(separate))
    }
}
