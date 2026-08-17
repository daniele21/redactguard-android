package io.github.daniele21.redactguard.domain.redaction

import io.github.daniele21.redactguard.domain.document.DocumentSegment
import io.github.daniele21.redactguard.domain.document.SegmentId
import io.github.daniele21.redactguard.domain.document.SourceOccurrence
import io.github.daniele21.redactguard.domain.document.SourceRange
import io.github.daniele21.redactguard.domain.pii.PiiDefinition
import io.github.daniele21.redactguard.domain.pii.PiiDefinitionSource
import io.github.daniele21.redactguard.domain.pii.PiiTypeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RedactionPlannerTest {
    @Test
    fun `accepted placeholders follow source order while ignored values remain`() {
        val text = "alice@example.test and bob@example.test; keep carol@example.test"
        val segment = segment(text)
        val email = definition("email", "Email")
        val result =
            RedactionPlanner.build(
                listOf(segment),
                listOf(email),
                listOf(
                    occurrence(segment, email.id, text, "carol@example.test", ReviewDecisionState.IGNORED),
                    occurrence(segment, email.id, text, "bob@example.test", ReviewDecisionState.ACCEPTED),
                    occurrence(segment, email.id, text, "alice@example.test", ReviewDecisionState.ACCEPTED),
                ),
            )

        val plan = (result as RedactionPlanResult.Ready).plan
        assertEquals(listOf("[EMAIL_1]", "[EMAIL_2]"), plan.replacements.map { it.placeholder })
        assertEquals("[EMAIL_1] and [EMAIL_2]; keep carol@example.test", plan.renderedSegments.single().text)
        assertEquals(1, plan.ignoredCount)
    }

    @Test
    fun `accepted overlap blocks export instead of choosing a winner`() {
        val segment = segment("ABCD")
        val first = definition("first", "First")
        val second = definition("second", "Second")

        val result =
            RedactionPlanner.build(
                listOf(segment),
                listOf(first, second),
                listOf(
                    explicitOccurrence(segment, first.id, "ABC", 0, 3, ReviewDecisionState.ACCEPTED),
                    explicitOccurrence(segment, second.id, "BCD", 1, 4, ReviewDecisionState.ACCEPTED),
                ),
            )

        assertEquals(RedactionPlanResult.Blocked(RedactionPlanFailureCode.OVERLAP_CONFLICT, 1), result)
    }

    @Test
    fun `pending and source mismatch fail closed`() {
        val segment = segment("alice@example.test")
        val email = definition("email", "Email")
        val pending = occurrence(segment, email.id, segment.normalizedText, segment.normalizedText, ReviewDecisionState.PENDING)
        assertEquals(
            RedactionPlanResult.Blocked(RedactionPlanFailureCode.PENDING_DECISION),
            RedactionPlanner.build(listOf(segment), listOf(email), listOf(pending)),
        )

        val mismatch = explicitOccurrence(segment, email.id, "wrong@example.test", 0, segment.normalizedText.length, ReviewDecisionState.ACCEPTED)
        assertEquals(
            RedactionPlanResult.Blocked(RedactionPlanFailureCode.SOURCE_MISMATCH),
            RedactionPlanner.build(listOf(segment), listOf(email), listOf(mismatch)),
        )
    }

    @Test
    fun `placeholder keys are bounded deterministic and collision safe`() {
        val first = definition("custom-a", "Matrìcola dipendente")
        val second = definition("custom-b", "Matricola dipendente")
        val long = definition("custom-c", "Identificativo dipendente estremamente lungo per il documento")
        val keys = PlaceholderKeys.fromDefinitions(listOf(second, long, first))
        assertEquals("MATRICOLA_DIPENDENTE", keys.getValue(first.id))
        assertEquals("MATRICOLA_DIPENDENTE_2", keys.getValue(second.id))
        assertTrue(keys.getValue(long.id).length <= 32)
        assertTrue(keys.values.all { it.matches(Regex("[A-Z0-9_]+")) })
    }

    @Test
    fun `zero findings preserves normalized document`() {
        val segment = segment("No personal information in this synthetic fixture.")
        val plan =
            (RedactionPlanner.build(listOf(segment), listOf(definition("email", "Email")), emptyList()) as RedactionPlanResult.Ready).plan
        assertEquals(segment.normalizedText, plan.renderedSegments.single().text)
        assertTrue(plan.replacements.isEmpty())
    }

    private fun segment(text: String) =
        DocumentSegment(SegmentId.fromIndices(0, 0), 0, 0, text)

    private fun definition(
        id: String,
        label: String,
    ) = PiiDefinition(PiiTypeId.parse(id), label, "Synthetic definition for $label", null, PiiDefinitionSource.CUSTOM)

    private fun occurrence(
        segment: DocumentSegment,
        typeId: PiiTypeId,
        sourceText: String,
        surface: String,
        decision: ReviewDecisionState,
    ): ReviewOccurrence {
        val start = sourceText.indexOf(surface)
        require(start >= 0)
        return explicitOccurrence(segment, typeId, surface, start, start + surface.length, decision)
    }

    private fun explicitOccurrence(
        segment: DocumentSegment,
        typeId: PiiTypeId,
        surface: String,
        start: Int,
        end: Int,
        decision: ReviewDecisionState,
    ) = ReviewOccurrence(
        OccurrenceId(typeId, SourceOccurrence(segment.id, SourceRange(start, end))),
        surface,
        decision,
    )
}
