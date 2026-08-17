package io.github.daniele21.redactguard.ui

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

    @Test
    fun `default projection never includes source surfaces`() {
        val first = occurrence("alice@example.test", 0, ReviewDecisionState.PENDING)
        val second = occurrence("bob@example.test", 30, ReviewDecisionState.ACCEPTED)

        val result = ReviewFindingProjector.project(listOf(second, first), listOf(email)) as ReviewProjectionResult.Ready

        assertEquals(listOf("[EMAIL_1]", "[EMAIL_2]"), result.findings.map(ReviewFindingModel::placeholder))
        assertTrue(result.findings.all { it.revealedValue == null })
        assertEquals(
            listOf(ReviewDecision.PENDING, ReviewDecision.REDACT),
            result.findings.map(ReviewFindingModel::decision),
        )
        assertEquals(false, result.canExport)
        assertTrue(result.findings.none { it.toString().contains("alice@example.test") })
    }

    @Test
    fun `explicit reveal exposes only the requested occurrence`() {
        val first = occurrence("alice@example.test", 0, ReviewDecisionState.ACCEPTED)
        val second = occurrence("bob@example.test", 30, ReviewDecisionState.IGNORED)

        val result =
            ReviewFindingProjector.project(
                occurrences = listOf(first, second),
                definitions = listOf(email),
                revealedOccurrenceId = second.id,
            ) as ReviewProjectionResult.Ready

        assertNull(result.findings[0].revealedValue)
        assertEquals("bob@example.test", result.findings[1].revealedValue)
        assertEquals(true, result.canExport)
    }

    @Test
    fun `label containing sensitive surface falls back to generic type label`() {
        val occurrence = occurrence("alice@example.test", 0, ReviewDecisionState.PENDING)
        val unsafeDefinition =
            PiiDefinition(
                id = PiiTypeId.parse("email"),
                label = "Email alice@example.test",
                definition = "Synthetic unsafe label fixture",
                source = PiiDefinitionSource.CUSTOM,
            )

        val result =
            ReviewFindingProjector.project(listOf(occurrence), listOf(unsafeDefinition)) as ReviewProjectionResult.Ready

        assertEquals("Dato personale", result.findings.single().categoryLabel)
    }

    @Test
    fun `unknown reveal fails closed`() {
        val occurrence = occurrence("alice@example.test", 0, ReviewDecisionState.PENDING)
        val unknown = occurrence("other@example.test", 40, ReviewDecisionState.PENDING)

        val result =
            ReviewFindingProjector.project(
                occurrences = listOf(occurrence),
                definitions = listOf(email),
                revealedOccurrenceId = unknown.id,
            )

        assertEquals(
            ReviewProjectionResult.Blocked(ReviewProjectionFailureCode.UNKNOWN_REVEAL_OCCURRENCE),
            result,
        )
    }

    private fun occurrence(
        surface: String,
        start: Int,
        decision: ReviewDecisionState,
    ): ReviewOccurrence =
        ReviewOccurrence(
            id =
                OccurrenceId(
                    typeId = email.id,
                    source = SourceOccurrence(SegmentId.fromIndices(0, 0), SourceRange(start, start + surface.length)),
                ),
            surface = surface,
            decision = decision,
        )
}
