package io.github.daniele21.redactguard.quality

internal data class QualityThresholds(
    val minAggregatePrecision: Double,
    val minAggregateRecall: Double,
    val minAggregateF1: Double,
    val minPerTypePrecision: Double,
    val minPerTypeRecall: Double,
    val minPerTypeF1: Double,
    val minStructuredCompletionRate: Double,
    val maxInvalidFindingRate: Double,
    val maxInvalidResultRate: Double,
)

internal data class QualityAcceptancePolicy(
    val policyVersion: Int,
    val corpusIdentity: QualityCorpusIdentity,
    val requiredTypeIds: Set<String>,
    val thresholds: QualityThresholds,
)

internal enum class QualityGateFailureCode {
    CORPUS_IDENTITY_MISMATCH, MISSING_REQUIRED_TYPE,
    AGGREGATE_PRECISION_BELOW_MINIMUM, AGGREGATE_RECALL_BELOW_MINIMUM, AGGREGATE_F1_BELOW_MINIMUM,
    PER_TYPE_PRECISION_BELOW_MINIMUM, PER_TYPE_RECALL_BELOW_MINIMUM, PER_TYPE_F1_BELOW_MINIMUM,
    STRUCTURED_COMPLETION_BELOW_MINIMUM, INVALID_FINDING_RATE_ABOVE_MAXIMUM, INVALID_RESULT_RATE_ABOVE_MAXIMUM,
}

internal data class QualityGateFailure(val code: QualityGateFailureCode, val typeId: String? = null)
internal data class QualityAcceptanceReport(val accepted: Boolean, val failures: List<QualityGateFailure>)

internal object RedactGuardQualitySupportPolicyV1 {
    val policy = QualityAcceptancePolicy(
        policyVersion = 1,
        corpusIdentity = QualityCorpusIdentity(
            schemaVersion = 1,
            corpusVersion = "ombra-pii-synthetic-v2",
            sha256 = RedactGuardSyntheticQualityCorpus.EXPECTED_SHA256,
        ),
        requiredTypeIds = setOf("full-name", "email", "telephone", "postal-address", "italian-tax-code", "iban", "custom-1"),
        thresholds = QualityThresholds(
            minAggregatePrecision = 0.90,
            minAggregateRecall = 0.98,
            minAggregateF1 = 0.94,
            minPerTypePrecision = 0.80,
            minPerTypeRecall = 0.90,
            minPerTypeF1 = 0.85,
            minStructuredCompletionRate = 0.98,
            maxInvalidFindingRate = 0.02,
            maxInvalidResultRate = 0.0,
        ),
    )
}

internal object RedactGuardQualityAcceptanceGate {
    fun evaluate(policy: QualityAcceptancePolicy, score: QualityScore): QualityAcceptanceReport {
        if (score.corpusIdentity != policy.corpusIdentity) {
            return QualityAcceptanceReport(false, listOf(QualityGateFailure(QualityGateFailureCode.CORPUS_IDENTITY_MISMATCH)))
        }
        val failures = mutableListOf<QualityGateFailure>()
        val t = policy.thresholds
        fun below(actual: Double, minimum: Double, code: QualityGateFailureCode, type: String? = null) {
            if (actual < minimum) failures += QualityGateFailure(code, type)
        }
        fun above(actual: Double, maximum: Double, code: QualityGateFailureCode) {
            if (actual > maximum) failures += QualityGateFailure(code)
        }
        below(score.aggregate.precision, t.minAggregatePrecision, QualityGateFailureCode.AGGREGATE_PRECISION_BELOW_MINIMUM)
        below(score.aggregate.recall, t.minAggregateRecall, QualityGateFailureCode.AGGREGATE_RECALL_BELOW_MINIMUM)
        below(score.aggregate.f1, t.minAggregateF1, QualityGateFailureCode.AGGREGATE_F1_BELOW_MINIMUM)
        below(score.structuredCompletionRate, t.minStructuredCompletionRate, QualityGateFailureCode.STRUCTURED_COMPLETION_BELOW_MINIMUM)
        above(score.invalidFindingRate, t.maxInvalidFindingRate, QualityGateFailureCode.INVALID_FINDING_RATE_ABOVE_MAXIMUM)
        above(score.invalidResultRate, t.maxInvalidResultRate, QualityGateFailureCode.INVALID_RESULT_RATE_ABOVE_MAXIMUM)
        policy.requiredTypeIds.sorted().forEach { typeId ->
            val metrics = score.perType[typeId]
            if (metrics == null) {
                failures += QualityGateFailure(QualityGateFailureCode.MISSING_REQUIRED_TYPE, typeId)
            } else {
                below(metrics.precision, t.minPerTypePrecision, QualityGateFailureCode.PER_TYPE_PRECISION_BELOW_MINIMUM, typeId)
                below(metrics.recall, t.minPerTypeRecall, QualityGateFailureCode.PER_TYPE_RECALL_BELOW_MINIMUM, typeId)
                below(metrics.f1, t.minPerTypeF1, QualityGateFailureCode.PER_TYPE_F1_BELOW_MINIMUM, typeId)
            }
        }
        return QualityAcceptanceReport(failures.isEmpty(), failures)
    }
}
