package io.github.daniele21.redactguard.domain.analysis

import io.github.daniele21.redactguard.domain.document.DocumentSegment
import io.github.daniele21.redactguard.domain.document.SegmentId
import io.github.daniele21.redactguard.domain.document.SourceOccurrence
import io.github.daniele21.redactguard.domain.document.SourceRange
import io.github.daniele21.redactguard.domain.pii.PiiDefinition
import io.github.daniele21.redactguard.domain.pii.PiiTypeId

internal data class UnvalidatedFinding(
    val typeId: String,
    val surface: String,
    val segmentId: String,
) {
    override fun toString(): String = "UnvalidatedFinding(typeId=$typeId, surface=<redacted>, segmentId=$segmentId)"
}

internal data class ValidatedFinding(
    val typeId: PiiTypeId,
    val definitionLabel: String,
    val surface: String,
    val source: SourceOccurrence,
) {
    override fun toString(): String =
        "ValidatedFinding(typeId=$typeId, definitionLabel=$definitionLabel, surface=<redacted>, source=$source)"
}

internal enum class AnalysisResultFailureCode {
    EMPTY_RESULT,
    RESPONSE_TOO_LARGE,
    MALFORMED_JSON,
    UNEXPECTED_ROOT_SHAPE,
    WRONG_SCHEMA_VERSION,
    UNEXPECTED_FINDING_SHAPE,
    FIELD_LIMIT_EXCEEDED,
}

internal class AnalysisResultException(
    val code: AnalysisResultFailureCode,
) : IllegalArgumentException("Invalid structured analysis result: $code")

internal object AnalysisResultParser {
    private const val MAX_RESPONSE_CHARACTERS = 65_536
    private const val MAX_TYPE_ID_CHARACTERS = 64
    private const val MAX_SEGMENT_ID_CHARACTERS = 64
    private const val MAX_SURFACE_CHARACTERS = 512
    private val segmentIdPattern = Regex("^p[0-9]{4}-b[0-9]{4}(-f[0-9]{4})?$")

    fun parse(raw: String): List<UnvalidatedFinding> {
        if (raw.isBlank()) fail(AnalysisResultFailureCode.EMPTY_RESULT)
        if (raw.length > MAX_RESPONSE_CHARACTERS) fail(AnalysisResultFailureCode.RESPONSE_TOO_LARGE)
        val root =
            try {
                StrictJsonReader(maxInputCharacters = MAX_RESPONSE_CHARACTERS).parse(raw)
            } catch (_: StrictJsonException) {
                fail(AnalysisResultFailureCode.MALFORMED_JSON)
            } as? JsonValue.ObjectValue ?: fail(AnalysisResultFailureCode.UNEXPECTED_ROOT_SHAPE)
        if (root.fields.keys != setOf("schemaVersion", "findings")) fail(AnalysisResultFailureCode.UNEXPECTED_ROOT_SHAPE)
        val schemaVersion =
            (root.fields["schemaVersion"] as? JsonValue.IntegerValue)?.value
                ?: fail(AnalysisResultFailureCode.UNEXPECTED_ROOT_SHAPE)
        if (schemaVersion != AnalysisProtocol.OUTPUT_SCHEMA_VERSION.toLong()) fail(AnalysisResultFailureCode.WRONG_SCHEMA_VERSION)
        val findings =
            (root.fields["findings"] as? JsonValue.ArrayValue)?.values
                ?: fail(AnalysisResultFailureCode.UNEXPECTED_ROOT_SHAPE)
        if (findings.size > AnalysisProtocol.MAX_FINDINGS) fail(AnalysisResultFailureCode.FIELD_LIMIT_EXCEEDED)
        return findings.map(::parseFinding)
    }

    private fun parseFinding(value: JsonValue): UnvalidatedFinding {
        val objectValue = value as? JsonValue.ObjectValue ?: fail(AnalysisResultFailureCode.UNEXPECTED_FINDING_SHAPE)
        if (objectValue.fields.keys != setOf("typeId", "surface", "segmentId")) {
            fail(AnalysisResultFailureCode.UNEXPECTED_FINDING_SHAPE)
        }
        val typeId =
            (objectValue.fields["typeId"] as? JsonValue.StringValue)?.value
                ?: fail(AnalysisResultFailureCode.UNEXPECTED_FINDING_SHAPE)
        val surface =
            (objectValue.fields["surface"] as? JsonValue.StringValue)?.value
                ?: fail(AnalysisResultFailureCode.UNEXPECTED_FINDING_SHAPE)
        val segmentId =
            (objectValue.fields["segmentId"] as? JsonValue.StringValue)?.value
                ?: fail(AnalysisResultFailureCode.UNEXPECTED_FINDING_SHAPE)
        if (
            typeId.isBlank() || typeId.length > MAX_TYPE_ID_CHARACTERS ||
            surface.isBlank() || surface.length > MAX_SURFACE_CHARACTERS || surface.contains('\uFFFD') ||
            segmentId.length > MAX_SEGMENT_ID_CHARACTERS || !segmentIdPattern.matches(segmentId)
        ) {
            fail(AnalysisResultFailureCode.FIELD_LIMIT_EXCEEDED)
        }
        return UnvalidatedFinding(typeId, surface, segmentId)
    }

    private fun fail(code: AnalysisResultFailureCode): Nothing = throw AnalysisResultException(code)
}

internal enum class FindingValidationFailureCode {
    UNKNOWN_TYPE_ID,
    UNKNOWN_SEGMENT_ID,
    SOURCE_INDEX_MISMATCH,
    SURFACE_NOT_FOUND_IN_SEGMENT,
    AMBIGUOUS_SURFACE_IN_SEGMENT,
    INVALID_UTF16_BOUNDARY,
    OVERLAPPING_FINDINGS,
}

internal sealed interface FindingValidationResult {
    data class Valid(
        val findings: List<ValidatedFinding>,
    ) : FindingValidationResult

    data class Rejected(
        val code: FindingValidationFailureCode,
    ) : FindingValidationResult
}

/** Maps model-visible segment IDs back to canonical source coordinates without text search across fragments. */
internal object FindingValidator {
    fun validate(
        rawFindings: List<UnvalidatedFinding>,
        chunks: List<AnalysisChunk>,
        canonicalSegments: List<DocumentSegment>,
        definitions: List<PiiDefinition>,
    ): FindingValidationResult {
        val definitionById = definitions.associateBy { it.id.value }
        val sourceIndex =
            buildSourceIndex(chunks, canonicalSegments)
                ?: return FindingValidationResult.Rejected(FindingValidationFailureCode.SOURCE_INDEX_MISMATCH)
        val analysisSegments = chunks.flatMap(AnalysisChunk::segments).associateBy(AnalysisSegmentData::segmentId)
        val validated = mutableListOf<ValidatedFinding>()

        for (raw in rawFindings) {
            val definition =
                definitionById[raw.typeId]
                    ?: return FindingValidationResult.Rejected(FindingValidationFailureCode.UNKNOWN_TYPE_ID)
            val analysisSegment =
                analysisSegments[raw.segmentId]
                    ?: return FindingValidationResult.Rejected(FindingValidationFailureCode.UNKNOWN_SEGMENT_ID)
            val mapping =
                sourceIndex[raw.segmentId]
                    ?: return FindingValidationResult.Rejected(FindingValidationFailureCode.SOURCE_INDEX_MISMATCH)
            val occurrences = exactOccurrences(analysisSegment.text, raw.surface)
            if (occurrences.isEmpty()) {
                return FindingValidationResult.Rejected(FindingValidationFailureCode.SURFACE_NOT_FOUND_IN_SEGMENT)
            }
            if (occurrences.size != 1) {
                return FindingValidationResult.Rejected(FindingValidationFailureCode.AMBIGUOUS_SURFACE_IN_SEGMENT)
            }
            val localStart = occurrences.single()
            val localEnd = localStart + raw.surface.length
            if (!isUtf16Boundary(analysisSegment.text, localStart) || !isUtf16Boundary(analysisSegment.text, localEnd)) {
                return FindingValidationResult.Rejected(FindingValidationFailureCode.INVALID_UTF16_BOUNDARY)
            }
            validated +=
                ValidatedFinding(
                    definition.id,
                    definition.label,
                    raw.surface,
                    SourceOccurrence(
                        mapping.canonicalSegmentId,
                        SourceRange(mapping.sourceStart + localStart, mapping.sourceStart + localEnd),
                    ),
                )
        }

        val deduplicated =
            validated
                .distinctBy { Triple(it.typeId, it.source.segmentId, it.source.range) }
                .sortedWith(
                    compareBy({
                        it.source.segmentId.value
                    }, { it.source.range.startInclusive }, { it.source.range.endExclusive }, { it.typeId.value }),
                )
        if (hasOverlap(deduplicated)) {
            return FindingValidationResult.Rejected(FindingValidationFailureCode.OVERLAPPING_FINDINGS)
        }
        return FindingValidationResult.Valid(deduplicated)
    }

    private data class SourceMapping(
        val canonicalSegmentId: SegmentId,
        val sourceStart: Int,
    )

    private fun buildSourceIndex(
        chunks: List<AnalysisChunk>,
        canonicalSegments: List<DocumentSegment>,
    ): Map<String, SourceMapping>? {
        val canonicalById = canonicalSegments.associateBy { it.id.value }
        val flattened = chunks.sortedBy(AnalysisChunk::ordinal).flatMap(AnalysisChunk::segments)
        val result = linkedMapOf<String, SourceMapping>()
        val fragmentsByBase = flattened.filter { "-f" in it.segmentId }.groupBy { it.segmentId.substringBeforeLast("-f") }

        flattened.filterNot { "-f" in it.segmentId }.forEach { segment ->
            val canonical = canonicalById[segment.segmentId] ?: return null
            if (canonical.normalizedText != segment.text) return null
            if (result.put(segment.segmentId, SourceMapping(canonical.id, 0)) != null) return null
        }
        fragmentsByBase.forEach { (baseId, fragments) ->
            val canonical = canonicalById[baseId] ?: return null
            var offset = 0
            fragments.forEachIndexed { index, fragment ->
                val expectedId = "$baseId-f${(index + 1).toString().padStart(4, '0')}"
                if (fragment.segmentId != expectedId ||
                    result.put(fragment.segmentId, SourceMapping(canonical.id, offset)) != null
                ) {
                    return null
                }
                offset += fragment.text.length
            }
            if (fragments.joinToString(separator = "") { it.text } != canonical.normalizedText) return null
        }
        return result
    }

    private fun exactOccurrences(
        text: String,
        surface: String,
    ): List<Int> {
        val result = mutableListOf<Int>()
        var cursor = 0
        while (cursor <= text.length - surface.length) {
            val index = text.indexOf(surface, cursor)
            if (index < 0) break
            result += index
            cursor = index + 1
        }
        return result
    }

    private fun isUtf16Boundary(
        text: String,
        offset: Int,
    ): Boolean = offset == 0 || offset == text.length || !(text[offset - 1].isHighSurrogate() && text[offset].isLowSurrogate())

    private fun hasOverlap(findings: List<ValidatedFinding>): Boolean =
        findings.groupBy { it.source.segmentId }.values.any { group ->
            group.zipWithNext().any { (left, right) -> left.source.range.overlaps(right.source.range) }
        }
}
