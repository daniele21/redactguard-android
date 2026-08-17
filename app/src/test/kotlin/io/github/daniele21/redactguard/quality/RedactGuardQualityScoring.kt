package io.github.daniele21.redactguard.quality

internal sealed interface QualityCaseOutcome {
    val caseId: String
    data class Structured(override val caseId: String, val findings: List<QualityOccurrence>, val invalidFindingCount: Int = 0) : QualityCaseOutcome
    data class InvalidResult(override val caseId: String) : QualityCaseOutcome
    data class Incomplete(override val caseId: String) : QualityCaseOutcome
}

internal data class ExactOccurrenceCounts(val truePositives: Int, val falsePositives: Int, val falseNegatives: Int) {
    operator fun plus(other: ExactOccurrenceCounts) = ExactOccurrenceCounts(
        truePositives + other.truePositives,
        falsePositives + other.falsePositives,
        falseNegatives + other.falseNegatives,
    )
}

internal data class ExactOccurrenceMetrics(val counts: ExactOccurrenceCounts, val precision: Double, val recall: Double, val f1: Double)

internal data class QualityScore(
    val corpusIdentity: QualityCorpusIdentity,
    val aggregate: ExactOccurrenceMetrics,
    val perType: Map<String, ExactOccurrenceMetrics>,
    val invalidFindingRate: Double,
    val invalidResultRate: Double,
    val structuredCompletionRate: Double,
)

internal object RedactGuardExactOccurrenceScorer {
    fun score(corpus: QualityCorpus, outcomes: List<QualityCaseOutcome>): QualityScore {
        val outcomeByCase = outcomes.associateBy(QualityCaseOutcome::caseId)
        require(outcomeByCase.size == outcomes.size)
        require(outcomeByCase.keys == corpus.cases.mapTo(linkedSetOf(), QualityCase::id))

        var aggregate = ZERO
        val perTypeCounts = linkedMapOf<String, ExactOccurrenceCounts>()
        var invalidFindings = 0
        var reportedFindings = 0
        var invalidResults = 0
        var structured = 0

        corpus.cases.forEach { case ->
            val predicted = when (val outcome = outcomeByCase.getValue(case.id)) {
                is QualityCaseOutcome.Structured -> {
                    invalidFindings += outcome.invalidFindingCount
                    reportedFindings += outcome.findings.size + outcome.invalidFindingCount
                    structured += 1
                    outcome.findings
                }
                is QualityCaseOutcome.InvalidResult -> {
                    invalidResults += 1
                    emptyList()
                }
                is QualityCaseOutcome.Incomplete -> emptyList()
            }
            aggregate += exactCounts(case.expectedOccurrences, predicted)
            (case.selectedTypeIds + predicted.map(QualityOccurrence::typeId)).forEach { typeId ->
                perTypeCounts[typeId] = perTypeCounts.getOrDefault(typeId, ZERO) + exactCounts(
                    case.expectedOccurrences.filter { it.typeId == typeId },
                    predicted.filter { it.typeId == typeId },
                )
            }
        }

        return QualityScore(
            corpusIdentity = corpus.identity,
            aggregate = metrics(aggregate),
            perType = perTypeCounts.toSortedMap().mapValues { metrics(it.value) },
            invalidFindingRate = ratio(invalidFindings, reportedFindings),
            invalidResultRate = ratio(invalidResults, corpus.cases.size),
            structuredCompletionRate = ratio(structured, corpus.cases.size),
        )
    }

    private fun exactCounts(expected: List<QualityOccurrence>, predicted: List<QualityOccurrence>): ExactOccurrenceCounts {
        val remaining = expected.groupingBy { it }.eachCount().toMutableMap()
        var tp = 0
        var fp = 0
        predicted.forEach { occurrence ->
            val available = remaining.getOrDefault(occurrence, 0)
            if (available > 0) {
                tp += 1
                if (available == 1) remaining.remove(occurrence) else remaining[occurrence] = available - 1
            } else {
                fp += 1
            }
        }
        return ExactOccurrenceCounts(tp, fp, remaining.values.sum())
    }

    private fun metrics(counts: ExactOccurrenceCounts): ExactOccurrenceMetrics {
        val precision = ratio(counts.truePositives, counts.truePositives + counts.falsePositives)
        val recall = ratio(counts.truePositives, counts.truePositives + counts.falseNegatives)
        val f1 = if (precision + recall == 0.0) 0.0 else 2.0 * precision * recall / (precision + recall)
        return ExactOccurrenceMetrics(counts, precision, recall, f1)
    }

    private fun ratio(numerator: Int, denominator: Int) = if (denominator == 0) 0.0 else numerator.toDouble() / denominator
    private val ZERO = ExactOccurrenceCounts(0, 0, 0)
}
