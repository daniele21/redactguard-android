package io.github.daniele21.redactguard.ui

import io.github.daniele21.redactguard.domain.document.DocumentDescriptor
import io.github.daniele21.redactguard.domain.pii.PiiDefinition
import io.github.daniele21.redactguard.domain.pii.PiiSemanticCategory
import io.github.daniele21.redactguard.domain.redaction.ReviewDecisionState
import io.github.daniele21.redactguard.domain.redaction.ReviewOccurrence

internal enum class PiiVisualFamily {
    IDENTITY,
    CONTACT,
    HEALTH,
    FINANCIAL,
    LOCATION,
    OTHER,
}

/** Single product owner for mapping the richer domain taxonomy into the six target visual families. */
internal object PiiVisualFamilyProjector {
    fun project(category: PiiSemanticCategory?): PiiVisualFamily =
        when (category) {
            PiiSemanticCategory.IDENTITY -> PiiVisualFamily.IDENTITY

            PiiSemanticCategory.CONTACT -> PiiVisualFamily.CONTACT

            PiiSemanticCategory.HEALTH,
            PiiSemanticCategory.LAB,
            PiiSemanticCategory.MEASUREMENT,
            -> PiiVisualFamily.HEALTH

            PiiSemanticCategory.FINANCIAL,
            PiiSemanticCategory.DATE,
            -> PiiVisualFamily.FINANCIAL

            PiiSemanticCategory.LOCATION -> PiiVisualFamily.LOCATION

            PiiSemanticCategory.LIFESTYLE,
            PiiSemanticCategory.SECRET,
            PiiSemanticCategory.CUSTOM,
            null,
            -> PiiVisualFamily.OTHER
        }
}

internal data class ProductCategorySummary(
    val family: PiiVisualFamily,
    val count: Int,
) {
    init {
        require(count > 0)
    }
}

/**
 * Process-local, user-visible summary used only to make Review/Outcome richer without fabricating
 * target metrics. `displayName` may contain personal data and is therefore never exposed by
 * `toString()` or evidence metadata.
 */
internal data class ProductDocumentSummary(
    val displayName: String,
    val pageCount: Int,
    val totalFindings: Int,
    val redactedCount: Int,
    val keptCount: Int,
    val pendingCount: Int,
    val categoryCounts: List<ProductCategorySummary>,
) {
    init {
        require(displayName.isNotBlank())
        require(pageCount > 0)
        require(totalFindings >= 0)
        require(redactedCount >= 0)
        require(keptCount >= 0)
        require(pendingCount >= 0)
        require(redactedCount + keptCount + pendingCount == totalFindings)
        require(categoryCounts.sumOf(ProductCategorySummary::count) == totalFindings)
        require(categoryCounts.map(ProductCategorySummary::family).distinct().size == categoryCounts.size)
    }

    override fun toString(): String =
        "ProductDocumentSummary(displayName=<redacted>, pageCount=$pageCount, totalFindings=$totalFindings, " +
            "redactedCount=$redactedCount, keptCount=$keptCount, pendingCount=$pendingCount, " +
            "categoryFamilies=${categoryCounts.map(ProductCategorySummary::family)})"
}

internal object ProductSummaryProjector {
    fun project(
        descriptor: DocumentDescriptor,
        definitions: List<PiiDefinition>,
        occurrences: List<ReviewOccurrence>,
    ): ProductDocumentSummary {
        val definitionsById = definitions.associateBy { it.id }
        val familyCounts =
            occurrences
                .groupingBy { occurrence ->
                    PiiVisualFamilyProjector.project(definitionsById[occurrence.id.typeId]?.semanticCategory)
                }.eachCount()

        return ProductDocumentSummary(
            displayName = descriptor.displayName,
            pageCount = descriptor.pageCount,
            totalFindings = occurrences.size,
            redactedCount = occurrences.count { it.decision == ReviewDecisionState.ACCEPTED },
            keptCount = occurrences.count { it.decision == ReviewDecisionState.IGNORED },
            pendingCount = occurrences.count { it.decision == ReviewDecisionState.PENDING },
            categoryCounts =
                PiiVisualFamily.entries.mapNotNull { family ->
                    familyCounts[family]?.takeIf { it > 0 }?.let { count ->
                        ProductCategorySummary(family = family, count = count)
                    }
                },
        )
    }
}
