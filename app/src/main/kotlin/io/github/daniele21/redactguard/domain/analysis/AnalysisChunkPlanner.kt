package io.github.daniele21.redactguard.domain.analysis

import io.github.daniele21.redactguard.domain.document.DocumentSegment
import io.github.daniele21.redactguard.domain.pii.PiiDefinition

internal data class AnalysisPlanningPolicy(
    val templateOverheadCharacters: Int = DEFAULT_TEMPLATE_OVERHEAD_CHARACTERS,
    val maxFragmentsPerSegment: Int = MAX_FRAGMENT_ORDINAL,
) {
    init {
        require(templateOverheadCharacters >= 0) { "Template overhead must be non-negative" }
        require(maxFragmentsPerSegment in 1..MAX_FRAGMENT_ORDINAL) { "Invalid fragment limit" }
    }

    companion object {
        const val DEFAULT_TEMPLATE_OVERHEAD_CHARACTERS = 1_024
        const val MAX_FRAGMENT_ORDINAL = 9_999
    }
}

internal enum class ChunkPlanFailureCode {
    JSON_SCHEMA_LIMIT_EXCEEDED,
    INPUT_OVERHEAD_EXCEEDS_LIMIT,
    FRAGMENT_LIMIT_EXCEEDED,
}

internal sealed interface ChunkPlanResult {
    data class Planned(
        val chunks: List<AnalysisChunk>,
    ) : ChunkPlanResult {
        init {
            require(chunks.isNotEmpty()) { "A successful plan must contain chunks" }
        }
    }

    data class Rejected(
        val code: ChunkPlanFailureCode,
    ) : ChunkPlanResult
}

/** Deterministic stateless chunk planning without SDK/runtime dependencies. */
internal class AnalysisChunkPlanner(
    private val policy: AnalysisPlanningPolicy = AnalysisPlanningPolicy(),
) {
    fun plan(
        segments: List<DocumentSegment>,
        definitions: List<PiiDefinition>,
        limits: AnalysisLimits,
    ): ChunkPlanResult {
        require(segments.isNotEmpty()) { "Chunk planning requires document segments" }
        require(definitions.isNotEmpty()) { "Chunk planning requires PII definitions" }

        if (AnalysisProtocol.outputJsonSchema.length > limits.maxJsonSchemaCharacters) {
            return ChunkPlanResult.Rejected(ChunkPlanFailureCode.JSON_SCHEMA_LIMIT_EXCEEDED)
        }

        val emptyPayloadLength = minimumPayloadLength(definitions)
        val fixedCharacters =
            AnalysisProtocol.instruction.length +
                policy.templateOverheadCharacters +
                emptyPayloadLength
        if (fixedCharacters >= limits.maxInputCharacters) {
            return ChunkPlanResult.Rejected(ChunkPlanFailureCode.INPUT_OVERHEAD_EXCEEDS_LIMIT)
        }

        val chunks = mutableListOf<AnalysisChunk>()
        val pending = ArrayDeque<PendingSegment>()
        segments.forEach { pending += PendingSegment.whole(it) }

        while (pending.isNotEmpty()) {
            val chunkSegments =
                buildNextChunk(pending, definitions, limits)
                    ?: return ChunkPlanResult.Rejected(ChunkPlanFailureCode.INPUT_OVERHEAD_EXCEEDS_LIMIT)
            val payload = AnalysisDataSerializer.serialize(definitions, chunkSegments)
            chunks += AnalysisChunk(chunks.size, chunkSegments, payload)
        }
        return ChunkPlanResult.Planned(chunks)
    }

    private fun buildNextChunk(
        pending: ArrayDeque<PendingSegment>,
        definitions: List<PiiDefinition>,
        limits: AnalysisLimits,
    ): List<AnalysisSegmentData>? {
        val chunkSegments = mutableListOf<AnalysisSegmentData>()
        var continueChunk = true
        while (pending.isNotEmpty() && continueChunk) {
            val candidate = pending.removeFirst()
            val wholeCandidate = candidate.asAnalysisSegment()
            when {
                fits(definitions, chunkSegments + wholeCandidate, limits) -> chunkSegments += wholeCandidate
                chunkSegments.isNotEmpty() -> {
                    pending.addFirst(candidate)
                    continueChunk = false
                }
                else -> {
                    val split = largestFittingPrefix(candidate, definitions, limits) ?: return null
                    chunkSegments += split.head
                    split.tail?.let(pending::addFirst)
                }
            }
        }
        return chunkSegments
    }

    private fun largestFittingPrefix(
        pending: PendingSegment,
        definitions: List<PiiDefinition>,
        limits: AnalysisLimits,
    ): SplitResult? {
        if (pending.fragmentOrdinal > policy.maxFragmentsPerSegment) return null
        val totalCodePoints = pending.text.codePointCount(0, pending.text.length)
        var low = 1
        var high = totalCodePoints
        var best: AnalysisSegmentData? = null
        var bestEndIndex = -1
        while (low <= high) {
            val middle = (low + high) ushr 1
            val endIndex = pending.text.offsetByCodePoints(0, middle)
            val fragment = pending.fragment(pending.text.substring(0, endIndex))
            if (fits(definitions, listOf(fragment), limits)) {
                best = fragment
                bestEndIndex = endIndex
                low = middle + 1
            } else {
                high = middle - 1
            }
        }

        val head = best ?: return null
        val remaining = pending.text.substring(bestEndIndex)
        val tail =
            remaining.takeIf(String::isNotEmpty)?.let {
                val nextOrdinal = pending.fragmentOrdinal + 1
                if (nextOrdinal > policy.maxFragmentsPerSegment) return null
                pending.copy(text = it, fragmentOrdinal = nextOrdinal, forceFragmentId = true)
            }
        return SplitResult(head, tail)
    }

    private fun fits(
        definitions: List<PiiDefinition>,
        segments: List<AnalysisSegmentData>,
        limits: AnalysisLimits,
    ): Boolean {
        val payload = AnalysisDataSerializer.serialize(definitions, segments)
        return AnalysisProtocol.instruction.length + policy.templateOverheadCharacters + payload.length <= limits.maxInputCharacters
    }

    private fun minimumPayloadLength(definitions: List<PiiDefinition>): Int {
        val serialized =
            AnalysisDataSerializer.serialize(
                definitions,
                listOf(AnalysisSegmentData("p0001-b0001", "x")),
            )
        return serialized.length - 1
    }

    private data class SplitResult(
        val head: AnalysisSegmentData,
        val tail: PendingSegment?,
    )

    private data class PendingSegment(
        val baseId: String,
        val text: String,
        val fragmentOrdinal: Int,
        val forceFragmentId: Boolean,
    ) {
        fun asAnalysisSegment(): AnalysisSegmentData =
            AnalysisSegmentData(
                segmentId = if (forceFragmentId) fragmentId(fragmentOrdinal) else baseId,
                text = text,
            )

        fun fragment(prefix: String): AnalysisSegmentData =
            AnalysisSegmentData(fragmentId(fragmentOrdinal), prefix)

        private fun fragmentId(ordinal: Int): String = "$baseId-f${ordinal.toString().padStart(4, '0')}"

        companion object {
            fun whole(segment: DocumentSegment): PendingSegment =
                PendingSegment(
                    baseId = segment.id.value,
                    text = segment.normalizedText,
                    fragmentOrdinal = 1,
                    forceFragmentId = false,
                )
        }
    }
}
