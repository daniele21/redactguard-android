package io.github.daniele21.redactguard.ui

import io.github.daniele21.redactguard.domain.document.DocumentSegment
import io.github.daniele21.redactguard.domain.document.SegmentId
import io.github.daniele21.redactguard.domain.document.SourceOccurrence
import io.github.daniele21.redactguard.domain.document.SourceRange
import io.github.daniele21.redactguard.domain.pii.PiiDefinition
import io.github.daniele21.redactguard.domain.pii.PiiDefinitionSource
import io.github.daniele21.redactguard.domain.pii.PiiTypeId
import io.github.daniele21.redactguard.domain.redaction.OccurrenceId
import io.github.daniele21.redactguard.domain.redaction.ReviewDecisionState
import io.github.daniele21.redactguard.domain.redaction.ReviewOccurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewFindingProjectorTest {
    private val email =
        PiiDefinition(
            id = PiiTypeId.parse("email"),
            label = "Email",
            definition = "Personal email address",
            source = PiiDefinitionSource.BUILT_IN,
        )
    private val sourceText = "Contatta alice@example.test oppure bob@example.test per assistenza."
    private val sourceSegment =
        DocumentSegment(
            id = SegmentId.fromIndices(0, 0),
            pageIndex = 0,
            blockIndex = 0,
            normalizedText = sourceText,
        )

    @Test
    fun `default projection masks every known surface in source context`() {
        val first = occurrence("alice@example.test", ReviewDecisionState.PENDING)
        val second = occurrence("bob@example.test", ReviewDecisionState.ACCEPTED)

        val result =
            ReviewFindingProjector.project(
                occurrences = listOf(second, first),
                definitions = listOf(email),
                segments = listOf(sourceSegment),
            ) as ReviewProjectionResult.Ready

        assertEquals(listOf("[EMAIL_1]", "[EMAIL_2]"), result.findings.map(ReviewFindingModel::placeholder))
        assertTrue(result.findings.all { it.revealedValue == null })
        assertEquals(
            listOf(ReviewDecision.PENDING, ReviewDecision.REDACT),
            result.findings.map(ReviewFindingModel::decision),
        )
        assertEquals(false, result.canExport)
        result.findings.forEach { finding ->
            assertEquals(1, finding.context.pageNumber)
            assertFalse(finding.context.maskedText.contains("alice@example.test"))
            assertFalse(finding.context.maskedText.contains("bob@example.test"))
            assertTrue(finding.context.maskedText.contains("[EMAIL_1]"))
            assertTrue(finding.context.maskedText.contains("[EMAIL_2]"))
        }
        assertTrue(result.findings.none { it.toString().contains("alice@example.test") })
    }

    @Test
    fun `explicit reveal exposes only requested value while context remains masked`() {
        val first = occurrence("alice@example.test", ReviewDecisionState.ACCEPTED)
        val second = occurrence("bob@example.test", ReviewDecisionState.IGNORED)

        val result =
            ReviewFindingProjector.project(
                occurrences = listOf(first, second),
                definitions = listOf(email),
                segments = listOf(sourceSegment),
                revealedOccurrenceId = second.id,
            ) as ReviewProjectionResult.Ready

        assertNull(result.findings[0].revealedValue)
        assertEquals("bob@example.test", result.findings[1].revealedValue)
        assertFalse(result.findings[1].context.maskedText.contains("bob@example.test"))
        assertEquals(true, result.canExport)
    }

    @Test
    fun `label containing sensitive surface falls back to generic type label`() {
        val occurrence = occurrence("alice@example.test", ReviewDecisionState.PENDING)
        val unsafeDefinition =
            PiiDefinition(
                id = PiiTypeId.parse("email"),
                label = "Email alice@example.test",
                definition = "Synthetic unsafe label fixture",
                source = PiiDefinitionSource.CUSTOM,
            )

        val result =
            ReviewFindingProjector.project(
                occurrences = listOf(occurrence),
                definitions = listOf(unsafeDefinition),
                segments = listOf(sourceSegment),
            ) as ReviewProjectionResult.Ready

        assertEquals("Dato personale", result.findings.single().categoryLabel)
    }

    @Test
    fun `unknown reveal fails closed`() {
        val occurrence = occurrence("alice@example.test", ReviewDecisionState.PENDING)
        val unknown =
            ReviewOccurrence(
                id =
                    OccurrenceId(
                        typeId = email.id,
                        source = SourceOccurrence(SegmentId.fromIndices(0, 0), SourceRange(0, 1)),
                    ),
                surface = "x",
            )

        val result =
            ReviewFindingProjector.project(
                occurrences = listOf(occurrence),
                definitions = listOf(email),
                segments = listOf(sourceSegment),
                revealedOccurrenceId = unknown.id,
            )

        assertEquals(
            ReviewProjectionResult.Blocked(ReviewProjectionFailureCode.UNKNOWN_REVEAL_OCCURRENCE),
            result,
        )
    }

    @Test
    fun `source mismatch fails closed before context reaches UI`() {
        val occurrence = occurrence("alice@example.test", ReviewDecisionState.PENDING)
        val corrupted = occurrence.copy(surface = "wrong@example.test")

        val result =
            ReviewFindingProjector.project(
                occurrences = listOf(corrupted),
                definitions = listOf(email),
                segments = listOf(sourceSegment),
            )

        assertEquals(ReviewProjectionResult.Blocked(ReviewProjectionFailureCode.SOURCE_MISMATCH), result)
    }

    @Test
    fun `unknown source segment fails closed`() {
        val occurrence = occurrence("alice@example.test", ReviewDecisionState.PENDING)
        val unknownSegmentOccurrence =
            occurrence.copy(
                id =
                    occurrence.id.copy(
                        source = occurrence.id.source.copy(segmentId = SegmentId.fromIndices(1, 0)),
                    ),
            )

        val result =
            ReviewFindingProjector.project(
                occurrences = listOf(unknownSegmentOccurrence),
                definitions = listOf(email),
                segments = listOf(sourceSegment),
            )

        assertEquals(ReviewProjectionResult.Blocked(ReviewProjectionFailureCode.UNKNOWN_SEGMENT), result)
    }

    @Test
    fun `overlapping findings fail closed instead of leaking ambiguous context`() {
        val full = occurrence("alice@example.test", ReviewDecisionState.PENDING)
        val start = sourceText.indexOf("alice@example.test")
        val partial =
            ReviewOccurrence(
                id =
                    OccurrenceId(
                        typeId = email.id,
                        source = SourceOccurrence(sourceSegment.id, SourceRange(start, start + "alice".length)),
                    ),
                surface = "alice",
            )

        val result =
            ReviewFindingProjector.project(
                occurrences = listOf(full, partial),
                definitions = listOf(email),
                segments = listOf(sourceSegment),
            )

        assertEquals(ReviewProjectionResult.Blocked(ReviewProjectionFailureCode.OVERLAP_CONFLICT), result)
    }

    private fun occurrence(
        surface: String,
        decision: ReviewDecisionState,
    ): ReviewOccurrence {
        val start = sourceText.indexOf(surface)
        require(start >= 0)
        return ReviewOccurrence(
            id =
                OccurrenceId(
                    typeId = email.id,
                    source = SourceOccurrence(sourceSegment.id, SourceRange(start, start + surface.length)),
                ),
            surface = surface,
            decision = decision,
        )
    }
}
