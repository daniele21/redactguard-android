package io.github.daniele21.redactguard.quality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RedactGuardQualityMigrationTest {
    @Test
    fun `migration preserves frozen corpus identity and coverage floor`() {
        val corpus = RedactGuardSyntheticQualityCorpus.load()
        assertEquals("ombra-pii-synthetic-v2", corpus.identity.corpusVersion)
        assertEquals(RedactGuardSyntheticQualityCorpus.EXPECTED_SHA256, corpus.identity.sha256)
        assertEquals(32, corpus.cases.size)

        val positiveByType = corpus.cases.flatMap(QualityCase::expectedOccurrences).groupingBy(QualityOccurrence::typeId).eachCount()
        RedactGuardQualitySupportPolicyV1.policy.requiredTypeIds.forEach { typeId ->
            assertTrue("Expected >=5 positive occurrences for $typeId", positiveByType.getOrDefault(typeId, 0) >= 5)
        }
    }

    @Test
    fun `pre-registered support policy remains bound to exact v2 identity`() {
        val corpus = RedactGuardSyntheticQualityCorpus.load()
        val policy = RedactGuardQualitySupportPolicyV1.policy
        assertEquals(corpus.identity, policy.corpusIdentity)
        assertEquals(0.90, policy.thresholds.minAggregatePrecision, 0.0)
        assertEquals(0.98, policy.thresholds.minAggregateRecall, 0.0)
        assertEquals(0.94, policy.thresholds.minAggregateF1, 0.0)
        assertEquals(0.0, policy.thresholds.maxInvalidResultRate, 0.0)
    }

    @Test
    fun `perfect frozen-corpus outcomes pass unchanged acceptance policy`() {
        val corpus = RedactGuardSyntheticQualityCorpus.load()
        val outcomes = corpus.cases.map { QualityCaseOutcome.Structured(it.id, it.expectedOccurrences) }
        val score = RedactGuardExactOccurrenceScorer.score(corpus, outcomes)
        val report = RedactGuardQualityAcceptanceGate.evaluate(RedactGuardQualitySupportPolicyV1.policy, score)
        assertTrue(report.failures.toString(), report.accepted)
        assertEquals(1.0, score.aggregate.precision, 0.0)
        assertEquals(1.0, score.aggregate.recall, 0.0)
        assertEquals(1.0, score.structuredCompletionRate, 0.0)
    }
}
