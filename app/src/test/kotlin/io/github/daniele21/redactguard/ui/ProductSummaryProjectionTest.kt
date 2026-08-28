package io.github.daniele21.redactguard.ui

import io.github.daniele21.redactguard.domain.document.DocumentDescriptor
import io.github.daniele21.redactguard.domain.document.SegmentId
import io.github.daniele21.redactguard.domain.document.SourceOccurrence
import io.github.daniele21.redactguard.domain.document.SourceRange
import io.github.daniele21.redactguard.domain.pii.PiiDefinition
import io.github.daniele21.redactguard.domain.pii.PiiDefinitionSource
import io.github.daniele21.redactguard.domain.pii.PiiSemanticCategory
import io.github.daniele21.redactguard.domain.pii.PiiTypeId
import io.github.daniele21.redactguard.domain.redaction.OccurrenceId
import io.github.daniele21.redactguard.domain.redaction.ReviewDecisionState
import io.github.daniele21.redactguard.domain.redaction.ReviewOccurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ProductSummaryProjectionTest {
    @Test
    fun `projects real decision and visual-family counts without leaking document identity`() {
        val identity = definition("private-person", PiiSemanticCategory.IDENTITY)
        val email = definition("private-email", PiiSemanticCategory.CONTACT)
        val health = definition("health-condition", PiiSemanticCategory.HEALTH)
        val date = definition("private-date", PiiSemanticCategory.DATE)
        val secret = definition("secret", PiiSemanticCategory.SECRET)
        val definitions = listOf(identity, email, health, date, secret)
        val occurrences =
            listOf(
                occurrence(identity, 0, ReviewDecisionState.ACCEPTED),
                occurrence(email, 20, ReviewDecisionState.IGNORED),
                occurrence(health, 40, ReviewDecisionState.ACCEPTED),
                occurrence(date, 60, ReviewDecisionState.PENDING),
                occurrence(secret, 80, ReviewDecisionState.PENDING),
            )

        val summary =
            ProductSummaryProjector.project(
                descriptor = DocumentDescriptor(displayName = "cliente-riservato.pdf", pageCount = 3),
                definitions = definitions,
                occurrences = occurrences,
            )

        assertEquals(3, summary.pageCount)
        assertEquals(5, summary.totalFindings)
        assertEquals(2, summary.redactedCount)
        assertEquals(1, summary.keptCount)
        assertEquals(2, summary.pendingCount)
        assertEquals(
            mapOf(
                PiiVisualFamily.IDENTITY to 1,
                PiiVisualFamily.CONTACT to 1,
                PiiVisualFamily.HEALTH to 1,
                PiiVisualFamily.FINANCIAL to 1,
                PiiVisualFamily.OTHER to 1,
            ),
            summary.categoryCounts.associate { it.family to it.count },
        )
        assertFalse(summary.toString().contains("cliente-riservato.pdf"))
        assertFalse(summary.toString().contains("secret-value"))
    }

    @Test
    fun `selection family lookup uses canonical built in taxonomy and keeps custom under other`() {
        assertEquals(PiiVisualFamily.IDENTITY, PiiVisualFamilyProjector.projectTypeId("full-name"))
        assertEquals(PiiVisualFamily.CONTACT, PiiVisualFamilyProjector.projectTypeId("email"))
        assertEquals(PiiVisualFamily.HEALTH, PiiVisualFamilyProjector.projectTypeId("health-condition"))
        assertEquals(PiiVisualFamily.FINANCIAL, PiiVisualFamilyProjector.projectTypeId("iban"))
        assertEquals(PiiVisualFamily.LOCATION, PiiVisualFamilyProjector.projectTypeId("postal-address"))
        assertEquals(PiiVisualFamily.OTHER, PiiVisualFamilyProjector.projectTypeId("custom-1"))
    }

    @Test
    fun `zero findings stays a truthful empty summary`() {
        val summary =
            ProductSummaryProjector.project(
                descriptor = DocumentDescriptor(displayName = "nessuna-occorrenza.pdf", pageCount = 2),
                definitions = emptyList(),
                occurrences = emptyList(),
            )

        assertEquals(2, summary.pageCount)
        assertEquals(0, summary.totalFindings)
        assertEquals(0, summary.redactedCount)
        assertEquals(0, summary.keptCount)
        assertEquals(0, summary.pendingCount)
        assertEquals(emptyList<ProductCategorySummary>(), summary.categoryCounts)
        assertFalse(summary.toString().contains("nessuna-occorrenza.pdf"))
    }

    private fun definition(
        id: String,
        category: PiiSemanticCategory,
    ): PiiDefinition =
        PiiDefinition(
            id = PiiTypeId.parse(id),
            label = "Product label",
            definition = "Product definition",
            source = PiiDefinitionSource.BUILT_IN,
            semanticCategory = category,
        )

    private fun occurrence(
        definition: PiiDefinition,
        start: Int,
        decision: ReviewDecisionState,
    ): ReviewOccurrence =
        ReviewOccurrence(
            id =
                OccurrenceId(
                    typeId = definition.id,
                    source =
                        SourceOccurrence(
                            segmentId = SegmentId.fromIndices(pageIndex = 0, blockIndex = 0),
                            range = SourceRange(startInclusive = start, endExclusive = start + 5),
                        ),
                ),
            surface = "secret-value-$start",
            decision = decision,
        )
}
