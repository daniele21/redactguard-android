package io.github.daniele21.redactguard.domain.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RedactionJobLifecycleTest {
    @Test
    fun `transport disconnect does not mutate product job state`() {
        val observation =
            observation(
                state = RedactionJobState.ANALYZING,
                transport = AnalysisTransportState.CONNECTED,
            )

        val reconnecting = observation.withTransport(AnalysisTransportState.RECONNECTING)

        assertEquals(RedactionJobState.ANALYZING, reconnecting.job.state)
        assertEquals(observation.job.revision, reconnecting.job.revision)
        assertEquals(AnalysisTransportState.RECONNECTING, reconnecting.transport)
    }

    @Test
    fun `stale job revision is ignored after reconnect`() {
        val current = observation(state = RedactionJobState.ANALYZING, revision = 7)

        val reduced =
            current.withJobTransition(
                RedactionJobTransition(
                    state = RedactionJobState.FAILED_FINAL,
                    revision = 6,
                    attempt = 1,
                    recoveryCapability = RecoveryCapability.NOT_RECOVERABLE,
                ),
            )

        assertEquals(current, reduced)
    }

    @Test
    fun `new attempt must begin in recovering`() {
        val current = observation(state = RedactionJobState.FAILED_RECOVERABLE, revision = 3)

        val failure =
            runCatching {
                current.withJobTransition(
                    RedactionJobTransition(
                        state = RedactionJobState.ANALYZING,
                        revision = 4,
                        attempt = 2,
                        recoveryCapability = RecoveryCapability.RECOVERABLE_WITH_PROCESS_STATE,
                    ),
                )
            }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun `recovery increments attempt then returns to analysis`() {
        val current = observation(state = RedactionJobState.FAILED_RECOVERABLE, revision = 3)
        val recovering =
            current.withJobTransition(
                RedactionJobTransition(
                    state = RedactionJobState.RECOVERING,
                    revision = 4,
                    attempt = 2,
                    recoveryCapability = RecoveryCapability.RECOVERABLE_WITH_PROCESS_STATE,
                ),
            )
        val analyzing =
            recovering.withJobTransition(
                RedactionJobTransition(
                    state = RedactionJobState.ANALYZING,
                    revision = 5,
                    attempt = 2,
                    recoveryCapability = RecoveryCapability.RECOVERABLE_WITH_PROCESS_STATE,
                ),
            )

        assertEquals(2, analyzing.job.attempt)
        assertEquals(5, analyzing.job.revision)
        assertEquals(RedactionJobState.ANALYZING, analyzing.job.state)
    }

    @Test
    fun `cancel intent is explicit and can survive transport reconnect`() {
        val current = observation(state = RedactionJobState.ANALYZING, revision = 8)
        val cancelRequested =
            current
                .withJobTransition(
                    RedactionJobTransition(
                        state = RedactionJobState.CANCEL_REQUESTED,
                        revision = 9,
                        attempt = 1,
                        recoveryCapability = RecoveryCapability.RECOVERABLE_WITH_PROCESS_STATE,
                        cancelRequested = true,
                    ),
                ).withTransport(AnalysisTransportState.RECONNECTING)

        assertTrue(cancelRequested.job.cancelRequested)
        assertEquals(RedactionJobState.CANCEL_REQUESTED, cancelRequested.job.state)
        assertEquals(AnalysisTransportState.RECONNECTING, cancelRequested.transport)

        val cancelled =
            cancelRequested.withJobTransition(
                RedactionJobTransition(
                    state = RedactionJobState.CANCELLED,
                    revision = 10,
                    attempt = 1,
                    recoveryCapability = RecoveryCapability.NOT_RECOVERABLE,
                    cancelRequested = true,
                ),
            )
        assertEquals(RedactionJobState.CANCELLED, cancelled.job.state)
    }

    @Test
    fun `non cancellation state rejects embedded cancel intent`() {
        val failure =
            runCatching {
                RedactionJobSnapshot(
                    jobId = RedactionJobId("rg-job-1"),
                    state = RedactionJobState.ANALYZING,
                    revision = 1,
                    attempt = 1,
                    recoveryCapability = RecoveryCapability.RECOVERABLE_WITH_PROCESS_STATE,
                    cancelRequested = true,
                )
            }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun `job id rejects content-like whitespace`() {
        val failure = runCatching { RedactionJobId("customer document 42") }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertFalse(runCatching { RedactionJobId("rg:42.analysis-v1") }.isFailure)
    }

    private fun observation(
        state: RedactionJobState,
        revision: Long = 1,
        transport: AnalysisTransportState = AnalysisTransportState.CONNECTED,
    ): RedactionJobObservation =
        RedactionJobObservation(
            job =
                RedactionJobSnapshot(
                    jobId = RedactionJobId("rg-job-1"),
                    state = state,
                    revision = revision,
                    attempt = 1,
                    recoveryCapability = RecoveryCapability.RECOVERABLE_WITH_PROCESS_STATE,
                ),
            transport = transport,
        )
}
