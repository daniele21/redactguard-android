package io.github.daniele21.redactguard.domain.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HarnessJobCorrelationTest {
    @Test
    fun `client request id is stable and contains only workflow identity`() {
        val requestId =
            HarnessClientRequestIds.forOperation(
                redactionJobId = RedactionJobId("rg-job-42"),
                operation = RedactionJobOperation.ANALYSIS,
                unitId = AnalysisUnitId("chunk-7"),
                contractVersion = 1,
            )

        assertEquals("rg-job-42:analysis:chunk-7:v1", requestId.value)
    }

    @Test
    fun `same Harness logical job can be attached idempotently`() {
        val correlation = correlation()
        val jobId = HarnessLogicalJobId("job-7")

        val attached = correlation.attach(jobId)
        val duplicate = attached.attach(jobId)

        assertEquals(jobId, attached.harnessJobId)
        assertSame(attached, duplicate)
    }

    @Test
    fun `different Harness logical job is rejected after attachment`() {
        val attached = correlation().attach(HarnessLogicalJobId("job-7"))

        val failure =
            runCatching {
                attached.attach(HarnessLogicalJobId("job-8"))
            }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun `stale Harness revision is ignored`() {
        val consumed = correlation().consumeRevision(8)

        val stale = consumed.consumeRevision(7)

        assertSame(consumed, stale)
        assertEquals(8, stale.lastConsumedRevision)
    }

    @Test
    fun `cancel intent is idempotent privacy-safe metadata`() {
        val requested = correlation().requestCancel()

        val duplicate = requested.requestCancel()

        assertTrue(requested.cancelRequested)
        assertSame(requested, duplicate)
    }

    @Test
    fun `content-like correlation identifiers are rejected`() {
        val failure = runCatching { AnalysisUnitId("John Smith chunk") }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    private fun correlation(): HarnessJobCorrelation =
        HarnessJobCorrelation(
            clientRequestId = HarnessClientRequestId("rg-job-42:analysis:chunk-7:v1"),
        )
}
