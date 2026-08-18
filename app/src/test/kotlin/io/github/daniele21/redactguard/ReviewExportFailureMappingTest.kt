package io.github.daniele21.redactguard

import io.github.daniele21.redactguard.domain.failure.ProductFailureKind
import io.github.daniele21.redactguard.domain.redaction.RedactionPlanFailureCode
import io.github.daniele21.redactguard.infrastructure.document.PdfExportFailureCode
import io.github.daniele21.redactguard.ui.ReviewProjectionFailureCode
import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewExportFailureMappingTest {
    @Test
    fun `every review plan failure preserves its own canonical code`() {
        val mapped = RedactionPlanFailureCode.entries.associateWith { ReviewFailureMapper.fromPlanCode(it).kind }

        assertEquals(ProductFailureKind.REVIEW_PENDING_DECISION, mapped[RedactionPlanFailureCode.PENDING_DECISION])
        assertEquals(ProductFailureKind.REVIEW_UNKNOWN_SEGMENT, mapped[RedactionPlanFailureCode.UNKNOWN_SEGMENT])
        assertEquals(ProductFailureKind.REVIEW_MISSING_DEFINITION, mapped[RedactionPlanFailureCode.MISSING_DEFINITION])
        assertEquals(ProductFailureKind.REVIEW_SOURCE_MISMATCH, mapped[RedactionPlanFailureCode.SOURCE_MISMATCH])
        assertEquals(ProductFailureKind.REVIEW_DUPLICATE_OCCURRENCE, mapped[RedactionPlanFailureCode.DUPLICATE_OCCURRENCE])
        assertEquals(ProductFailureKind.REVIEW_OVERLAP_CONFLICT, mapped[RedactionPlanFailureCode.OVERLAP_CONFLICT])
        assertEquals(RedactionPlanFailureCode.entries.size, mapped.values.toSet().size)
    }

    @Test
    fun `every review projection failure preserves a classified cause`() {
        val mapped = ReviewProjectionFailureCode.entries.associateWith { ReviewFailureMapper.fromProjectionCode(it).kind }

        assertEquals(ProductFailureKind.REVIEW_DUPLICATE_OCCURRENCE, mapped[ReviewProjectionFailureCode.DUPLICATE_OCCURRENCE])
        assertEquals(ProductFailureKind.REVIEW_DUPLICATE_DEFINITION, mapped[ReviewProjectionFailureCode.DUPLICATE_DEFINITION])
        assertEquals(ProductFailureKind.REVIEW_MISSING_DEFINITION, mapped[ReviewProjectionFailureCode.MISSING_DEFINITION])
        assertEquals(
            ProductFailureKind.REVIEW_UNKNOWN_REVEAL_OCCURRENCE,
            mapped[ReviewProjectionFailureCode.UNKNOWN_REVEAL_OCCURRENCE],
        )
    }

    @Test
    fun `export destination failure remains distinct from writer failure`() {
        val destination = ExportFailureMapper.fromCode(PdfExportFailureCode.DESTINATION_UNWRITABLE)
        val writer = ExportFailureMapper.fromCode(PdfExportFailureCode.WRITER_FAILED)

        assertEquals("RG-EXP-001", destination.code)
        assertEquals("RG-EXP-004", writer.code)
    }

    @Test
    fun `export source mismatch requires reanalysis rather than destination retry`() {
        val failure = ExportFailureMapper.fromCode(PdfExportFailureCode.SOURCE_MISMATCH)

        assertEquals(ProductFailureKind.SOURCE_MISMATCH, failure.kind)
    }
}
