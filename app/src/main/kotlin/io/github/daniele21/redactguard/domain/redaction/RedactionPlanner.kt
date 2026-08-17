package io.github.daniele21.redactguard.domain.redaction

import io.github.daniele21.redactguard.domain.document.DocumentSegment
import io.github.daniele21.redactguard.domain.document.SegmentId
import io.github.daniele21.redactguard.domain.pii.PiiDefinition
import io.github.daniele21.redactguard.domain.pii.PiiTypeId
import java.text.Normalizer
import java.util.Locale

internal enum class RedactionPlanFailureCode {
    PENDING_DECISION,
    UNKNOWN_SEGMENT,
    MISSING_DEFINITION,
    SOURCE_MISMATCH,
    DUPLICATE_OCCURRENCE,
    OVERLAP_CONFLICT,
}

internal sealed interface RedactionPlanResult {
    data class Ready(
        val plan: RedactionPlan,
    ) : RedactionPlanResult

    data class Blocked(
        val code: RedactionPlanFailureCode,
        val conflictCount: Int = 0,
    ) : RedactionPlanResult
}

internal data class RenderedSegment(
    val segmentId: SegmentId,
    val text: String,
) {
    override fun toString(): String = "RenderedSegment(segmentId=$segmentId, text=<redacted>)"
}

internal data class RedactionReplacement(
    val occurrenceId: OccurrenceId,
    val sourceSurface: String,
    val placeholder: String,
) {
    override fun toString(): String = "RedactionReplacement(occurrenceId=$occurrenceId, sourceSurface=<redacted>, placeholder=$placeholder)"
}

internal data class RedactionPlan(
    val renderedSegments: List<RenderedSegment>,
    val replacements: List<RedactionReplacement>,
    val acceptedCount: Int,
    val ignoredCount: Int,
) {
    init {
        require(acceptedCount == replacements.size) { "Accepted count must match replacement count" }
        require(ignoredCount >= 0) { "Ignored count must be non-negative" }
    }
}

/** Pure deterministic review-to-replacement planner. Android/PDF writing stays outside this boundary. */
internal object RedactionPlanner {
    fun build(
        segments: List<DocumentSegment>,
        definitions: List<PiiDefinition>,
        reviewOccurrences: List<ReviewOccurrence>,
    ): RedactionPlanResult {
        val definitionById = definitions.associateBy(PiiDefinition::id)
        val segmentById = segments.associateBy(DocumentSegment::id)
        val segmentOrder = segments.withIndex().associate { it.value.id to it.index }
        validateInputs(reviewOccurrences, definitionById, segmentById)?.let {
            return RedactionPlanResult.Blocked(it)
        }

        val accepted =
            reviewOccurrences
                .filter { it.decision == ReviewDecisionState.ACCEPTED }
                .sortedWith(sourceComparator(segmentOrder))
        val conflicts = countAcceptedOverlapConflicts(accepted)
        if (conflicts > 0) {
            return RedactionPlanResult.Blocked(RedactionPlanFailureCode.OVERLAP_CONFLICT, conflicts)
        }

        val placeholderKeys = PlaceholderKeys.fromDefinitions(definitions)
        val counters = mutableMapOf<PiiTypeId, Int>()
        val replacements =
            accepted.map { occurrence ->
                val number = counters.getOrDefault(occurrence.id.typeId, 0) + 1
                counters[occurrence.id.typeId] = number
                RedactionReplacement(
                    occurrenceId = occurrence.id,
                    sourceSurface = occurrence.surface,
                    placeholder = "[${placeholderKeys.getValue(occurrence.id.typeId)}_$number]",
                )
            }

        val replacementsBySegment = replacements.groupBy { it.occurrenceId.source.segmentId }
        val rendered =
            segments.map { segment ->
                val transformed =
                    replacementsBySegment[segment.id]
                        .orEmpty()
                        .sortedByDescending { it.occurrenceId.source.range.startInclusive }
                        .fold(segment.normalizedText) { current, replacement ->
                            val range = replacement.occurrenceId.source.range
                            current.replaceRange(range.startInclusive, range.endExclusive, replacement.placeholder)
                        }
                RenderedSegment(segment.id, transformed)
            }

        return RedactionPlanResult.Ready(
            RedactionPlan(
                renderedSegments = rendered,
                replacements = replacements,
                acceptedCount = accepted.size,
                ignoredCount = reviewOccurrences.count { it.decision == ReviewDecisionState.IGNORED },
            ),
        )
    }

    private fun validateInputs(
        reviewOccurrences: List<ReviewOccurrence>,
        definitionById: Map<PiiTypeId, PiiDefinition>,
        segmentById: Map<SegmentId, DocumentSegment>,
    ): RedactionPlanFailureCode? =
        when {
            reviewOccurrences.any { it.decision == ReviewDecisionState.PENDING } -> {
                RedactionPlanFailureCode.PENDING_DECISION
            }

            reviewOccurrences.map(ReviewOccurrence::id).distinct().size != reviewOccurrences.size -> {
                RedactionPlanFailureCode.DUPLICATE_OCCURRENCE
            }

            reviewOccurrences.any { it.id.typeId !in definitionById } -> {
                RedactionPlanFailureCode.MISSING_DEFINITION
            }

            reviewOccurrences.any { it.id.source.segmentId !in segmentById } -> {
                RedactionPlanFailureCode.UNKNOWN_SEGMENT
            }

            reviewOccurrences.any { occurrence ->
                !matchesSource(occurrence, requireNotNull(segmentById[occurrence.id.source.segmentId]))
            } -> {
                RedactionPlanFailureCode.SOURCE_MISMATCH
            }

            else -> {
                null
            }
        }

    private fun matchesSource(
        occurrence: ReviewOccurrence,
        segment: DocumentSegment,
    ): Boolean {
        val range = occurrence.id.source.range
        if (range.endExclusive > segment.normalizedText.length) return false
        return segment.normalizedText.substring(range.startInclusive, range.endExclusive) == occurrence.surface
    }

    private fun countAcceptedOverlapConflicts(accepted: List<ReviewOccurrence>): Int =
        accepted.groupBy { it.id.source.segmentId }.values.sumOf { group ->
            var conflicts = 0
            for (left in group.indices) {
                for (right in left + 1 until group.size) {
                    if (group[left]
                            .id.source.range
                            .overlaps(group[right].id.source.range)
                    ) {
                        conflicts += 1
                    }
                }
            }
            conflicts
        }

    private fun sourceComparator(segmentOrder: Map<SegmentId, Int>): Comparator<ReviewOccurrence> =
        compareBy(
            { segmentOrder.getValue(it.id.source.segmentId) },
            { it.id.source.range.startInclusive },
            { it.id.source.range.endExclusive },
            { it.id.typeId.value },
        )
}

internal object PlaceholderKeys {
    private const val MAX_KEY_LENGTH = 32

    fun fromDefinitions(definitions: List<PiiDefinition>): Map<PiiTypeId, String> {
        val baseKeys = definitions.associate { it.id to sanitize(it.label) }
        val result = mutableMapOf<PiiTypeId, String>()
        baseKeys.entries
            .groupBy { it.value }
            .toSortedMap()
            .forEach { (baseKey, entries) ->
                entries.sortedBy { it.key.value }.forEachIndexed { index, entry ->
                    result[entry.key] = if (index == 0) baseKey else withCollisionSuffix(baseKey, index + 1)
                }
            }
        return result
    }

    private fun sanitize(label: String): String {
        val decomposed = Normalizer.normalize(label, Normalizer.Form.NFD)
        val ascii =
            buildString {
                decomposed.forEach { character ->
                    when {
                        character.isLetterOrDigit() && character.code < 128 -> append(character.uppercaseChar())
                        Character.getType(character) == Character.NON_SPACING_MARK.toInt() -> Unit
                        else -> append('_')
                    }
                }
            }
        val collapsed = ascii.replace(Regex("_+"), "_").trim('_')
        return bounded(collapsed.ifEmpty { "PII" }.uppercase(Locale.ROOT))
    }

    private fun withCollisionSuffix(
        baseKey: String,
        ordinal: Int,
    ): String {
        val suffix = "_$ordinal"
        val prefix = baseKey.take((MAX_KEY_LENGTH - suffix.length).coerceAtLeast(1)).trimEnd('_')
        return "$prefix$suffix"
    }

    private fun bounded(value: String): String = value.take(MAX_KEY_LENGTH).trimEnd('_').ifEmpty { "PII" }
}
