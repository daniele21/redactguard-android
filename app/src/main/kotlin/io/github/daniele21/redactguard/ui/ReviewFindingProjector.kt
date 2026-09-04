package io.github.daniele21.redactguard.ui

import io.github.daniele21.redactguard.domain.document.DocumentSegment
import io.github.daniele21.redactguard.domain.document.SourceRange
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
    UNKNOWN_SEGMENT,
    SOURCE_MISMATCH,
    OVERLAP_CONFLICT,
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
 * exact occurrence is explicitly revealed. Context is source-backed and masks every known occurrence
 * intersecting the visible window before it reaches UI state.
 */
internal object ReviewFindingProjector {
    fun project(
        occurrences: List<ReviewOccurrence>,
        definitions: List<PiiDefinition>,
        segments: List<DocumentSegment>,
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

        val segmentById = segments.associateBy(DocumentSegment::id)
        if (segmentById.size != segments.size) {
            return ReviewProjectionResult.Blocked(ReviewProjectionFailureCode.SOURCE_MISMATCH)
        }
        occurrences.forEach { occurrence ->
            val segment =
                segmentById[occurrence.id.source.segmentId]
                    ?: return ReviewProjectionResult.Blocked(ReviewProjectionFailureCode.UNKNOWN_SEGMENT)
            if (!matchesSource(segment, occurrence)) {
                return ReviewProjectionResult.Blocked(ReviewProjectionFailureCode.SOURCE_MISMATCH)
            }
        }

        val ordered = occurrences.sortedWith(sourceComparator())
        if (hasOverlaps(ordered)) {
            return ReviewProjectionResult.Blocked(ReviewProjectionFailureCode.OVERLAP_CONFLICT)
        }

        val sensitiveSurfaces = ordered.map(ReviewOccurrence::surface)
        val placeholders = placeholders(ordered, definitions)
        val occurrencesBySegment = ordered.groupBy { it.id.source.segmentId }
        val findings =
            ordered.map { occurrence ->
                val definition = requireNotNull(definitionById[occurrence.id.typeId])
                val segment = requireNotNull(segmentById[occurrence.id.source.segmentId])
                val placeholder = placeholders.getValue(occurrence.id)
                ReviewFindingModel(
                    id = stableUiId(occurrence.id),
                    categoryLabel = safeTypeLabel(definition.label, sensitiveSurfaces),
                    placeholder = placeholder,
                    context =
                        ReviewContextModel(
                            maskedText =
                                maskedContext(
                                    segment = segment,
                                    focus = occurrence,
                                    segmentOccurrences = occurrencesBySegment[segment.id].orEmpty(),
                                    placeholders = placeholders,
                                ),
                            focusPlaceholder = placeholder,
                            pageNumber = segment.pageIndex + 1,
                        ),
                    revealedValue = occurrence.surface.takeIf { occurrence.id == revealedOccurrenceId },
                    decision = occurrence.decision.toUiDecision(),
                )
            }
        return ReviewProjectionResult.Ready(
            findings = findings,
            canExport = ordered.all { it.decision != ReviewDecisionState.PENDING },
        )
    }

    private fun matchesSource(
        segment: DocumentSegment,
        occurrence: ReviewOccurrence,
    ): Boolean {
        val range = occurrence.id.source.range
        if (range.endExclusive > segment.normalizedText.length) return false
        return segment.normalizedText.substring(range.startInclusive, range.endExclusive) == occurrence.surface
    }

    private fun hasOverlaps(ordered: List<ReviewOccurrence>): Boolean =
        ordered
            .groupBy { it.id.source.segmentId }
            .values
            .any { segmentOccurrences ->
                segmentOccurrences.zipWithNext().any { (left, right) ->
                    left.id.source.range
                        .overlaps(right.id.source.range)
                }
            }

    private fun maskedContext(
        segment: DocumentSegment,
        focus: ReviewOccurrence,
        segmentOccurrences: List<ReviewOccurrence>,
        placeholders: Map<OccurrenceId, String>,
    ): String {
        val source = segment.normalizedText
        val focusRange = focus.id.source.range
        val window =
            SourceRange(
                startInclusive = safeBoundaryStart(source, (focusRange.startInclusive - CONTEXT_CHARACTERS).coerceAtLeast(0)),
                endExclusive = safeBoundaryEnd(source, (focusRange.endExclusive + CONTEXT_CHARACTERS).coerceAtMost(source.length)),
            )
        val relevant =
            segmentOccurrences
                .filter {
                    it.id.source.range
                        .overlaps(window)
                }.sortedBy { it.id.source.range.startInclusive }
        return buildString {
            var cursor = window.startInclusive
            relevant.forEach { occurrence ->
                val range = occurrence.id.source.range
                val textEnd = range.startInclusive.coerceIn(window.startInclusive, window.endExclusive)
                if (cursor < textEnd) append(source.substring(cursor, textEnd))
                append(placeholders.getValue(occurrence.id))
                cursor = maxOf(cursor, range.endExclusive)
            }
            if (cursor < window.endExclusive) append(source.substring(cursor, window.endExclusive))
        }
    }

    private fun safeBoundaryStart(
        source: String,
        candidate: Int,
    ): Int =
        if (
            candidate in 1 until source.length &&
            Character.isLowSurrogate(source[candidate]) &&
            Character.isHighSurrogate(source[candidate - 1])
        ) {
            candidate - 1
        } else {
            candidate
        }

    private fun safeBoundaryEnd(
        source: String,
        candidate: Int,
    ): Int =
        if (
            candidate in 1 until source.length &&
            Character.isHighSurrogate(source[candidate - 1]) &&
            Character.isLowSurrogate(source[candidate])
        ) {
            candidate + 1
        } else {
            candidate
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

    private const val CONTEXT_CHARACTERS = 72
    private const val GENERIC_TYPE_LABEL = "Dato personale"
}
