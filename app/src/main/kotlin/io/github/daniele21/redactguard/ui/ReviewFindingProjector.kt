package io.github.daniele21.redactguard.ui

import io.github.daniele21.redactguard.domain.pii.PiiDefinition
import io.github.daniele21.redactguard.domain.pii.PiiTypeId
import io.github.daniele21.redactguard.domain.redaction.OccurrenceId
import io.github.daniele21.redactguard.domain.redaction.PlaceholderKeys
import io.github.daniele21.redactguard.domain.redaction.ReviewDecisionState
import io.github.daniele21.redactguard.domain.redaction.ReviewOccurrence

internal enum class ReviewProjectionFailureCode {
    DUPLICATE_OCCURRENCE,
    DUPLICATE_DEFINITION,
    MISSING_DEFINITION,
    UNKNOWN_REVEAL_OCCURRENCE,
}

internal sealed interface ReviewProjectionResult {
    data class Ready(
        val findings: List<ReviewFindingModel>,
        val canExport: Boolean,
    ) : ReviewProjectionResult

    data class Blocked(
        val code: ReviewProjectionFailureCode,
    ) : ReviewProjectionResult
}

/**
 * Content-safe default projection for Review. A source surface is copied into UI state only when its
 * exact occurrence is explicitly revealed.
 */
internal object ReviewFindingProjector {
    fun project(
        occurrences: List<ReviewOccurrence>,
        definitions: List<PiiDefinition>,
        revealedOccurrenceId: OccurrenceId? = null,
    ): ReviewProjectionResult {
        if (occurrences.map(ReviewOccurrence::id).distinct().size != occurrences.size) {
            return ReviewProjectionResult.Blocked(ReviewProjectionFailureCode.DUPLICATE_OCCURRENCE)
        }
        val definitionById = definitions.associateBy(PiiDefinition::id)
        if (definitionById.size != definitions.size) {
            return ReviewProjectionResult.Blocked(ReviewProjectionFailureCode.DUPLICATE_DEFINITION)
        }
        if (occurrences.any { it.id.typeId !in definitionById }) {
            return ReviewProjectionResult.Blocked(ReviewProjectionFailureCode.MISSING_DEFINITION)
        }
        if (revealedOccurrenceId != null && occurrences.none { it.id == revealedOccurrenceId }) {
            return ReviewProjectionResult.Blocked(ReviewProjectionFailureCode.UNKNOWN_REVEAL_OCCURRENCE)
        }

        val ordered = occurrences.sortedWith(sourceComparator())
        val sensitiveSurfaces = ordered.map(ReviewOccurrence::surface)
        val placeholders = placeholders(ordered, definitions)
        val findings =
            ordered.map { occurrence ->
                val definition = requireNotNull(definitionById[occurrence.id.typeId])
                ReviewFindingModel(
                    id = stableUiId(occurrence.id),
                    categoryLabel = safeTypeLabel(definition.label, sensitiveSurfaces),
                    placeholder = placeholders.getValue(occurrence.id),
                    revealedValue = occurrence.surface.takeIf { occurrence.id == revealedOccurrenceId },
                    decision = occurrence.decision.toUiDecision(),
                )
            }
        return ReviewProjectionResult.Ready(
            findings = findings,
            canExport = ordered.all { it.decision != ReviewDecisionState.PENDING },
        )
    }

    private fun placeholders(
        occurrences: List<ReviewOccurrence>,
        definitions: List<PiiDefinition>,
    ): Map<OccurrenceId, String> {
        val keys = PlaceholderKeys.fromDefinitions(definitions)
        val counters = mutableMapOf<PiiTypeId, Int>()
        return occurrences.associate { occurrence ->
            val ordinal = counters.getOrDefault(occurrence.id.typeId, 0) + 1
            counters[occurrence.id.typeId] = ordinal
            occurrence.id to "[${keys.getValue(occurrence.id.typeId)}_$ordinal]"
        }
    }

    private fun sourceComparator(): Comparator<ReviewOccurrence> =
        compareBy<ReviewOccurrence>(
            { it.id.source.segmentId.value },
            { it.id.source.range.startInclusive },
            { it.id.source.range.endExclusive },
            { it.id.typeId.value },
        )

    private fun stableUiId(id: OccurrenceId): String =
        buildString {
            append(id.typeId.value)
            append(':')
            append(id.source.segmentId.value)
            append(':')
            append(id.source.range.startInclusive)
            append('-')
            append(id.source.range.endExclusive)
        }

    private fun safeTypeLabel(
        label: String,
        sensitiveSurfaces: List<String>,
    ): String =
        if (sensitiveSurfaces.any { surface -> label.contains(surface, ignoreCase = true) }) {
            GENERIC_TYPE_LABEL
        } else {
            label
        }

    private fun ReviewDecisionState.toUiDecision(): ReviewDecision =
        when (this) {
            ReviewDecisionState.PENDING -> ReviewDecision.PENDING
            ReviewDecisionState.ACCEPTED -> ReviewDecision.REDACT
            ReviewDecisionState.IGNORED -> ReviewDecision.IGNORE
        }

    private const val GENERIC_TYPE_LABEL = "Dato personale"
}
