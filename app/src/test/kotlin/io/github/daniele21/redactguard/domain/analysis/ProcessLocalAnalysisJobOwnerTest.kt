package io.github.daniele21.redactguard.domain.analysis

import io.github.daniele21.redactguard.domain.document.DocumentSegment
import io.github.daniele21.redactguard.domain.document.SegmentId
import io.github.daniele21.redactguard.domain.pii.PiiDefinition
import io.github.daniele21.redactguard.domain.pii.PiiDefinitionSource
import io.github.daniele21.redactguard.domain.pii.PiiSemanticCategory
import io.github.daniele21.redactguard.domain.pii.PiiTypeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessLocalAnalysisJobOwnerTest {
    @Test
    fun `observer detach does not cancel and reattach sees same completed job`() {
        val engine = FakeAnalysisJobEngine()
        val owner = ProcessLocalAnalysisJobOwner(engine)
        val jobId = AnalysisJobId("job-1")
        val operationId = AnalysisOperationId("operation-1")
        val firstObserver = mutableListOf<AnalysisJobSnapshot>()

        val subscription = owner.observe(jobId, firstObserver::add)
        owner.start(jobId, operationId, request())
        subscription.close()

        assertEquals(1, engine.startCalls)
        assertEquals(0, engine.cancelCalls)
        assertEquals(AnalysisJobState.ACTIVE, owner.snapshot(jobId)?.state)

        engine.complete(Result.success(emptyList()))

        val reattached = mutableListOf<AnalysisJobSnapshot>()
        owner.observe(jobId, reattached::add)

        assertEquals(AnalysisJobState.SUCCEEDED, reattached.single().state)
        assertEquals(jobId, reattached.single().jobId)
        assertEquals(operationId, reattached.single().operationId)
        assertTrue(owner.outcome(jobId) is AnalysisJobOutcome.Success)
        assertEquals(1, engine.startCalls)
        assertEquals(0, engine.cancelCalls)
    }

    @Test
    fun `reattach while active observes current revision without duplicate execution`() {
        val engine = FakeAnalysisJobEngine()
        val owner = ProcessLocalAnalysisJobOwner(engine)
        val jobId = AnalysisJobId("job-active")
        owner.start(jobId, AnalysisOperationId("operation-active"), request())

        val observed = mutableListOf<AnalysisJobSnapshot>()
        owner.observe(jobId, observed::add)

        assertEquals(listOf(AnalysisJobState.ACTIVE), observed.map(AnalysisJobSnapshot::state))
        assertEquals(1, engine.startCalls)
    }

    @Test
    fun `explicit cancel targets exact active job and publishes terminal cancellation`() {
        val engine = FakeAnalysisJobEngine()
        val owner = ProcessLocalAnalysisJobOwner(engine)
        val jobId = AnalysisJobId("job-cancel")
        val observed = mutableListOf<AnalysisJobSnapshot>()
        owner.observe(jobId, observed::add)
        owner.start(jobId, AnalysisOperationId("operation-cancel"), request())

        var callbackInvoked = false
        owner.cancel(jobId) { callbackInvoked = true }

        assertEquals(1, engine.cancelCalls)
        assertEquals(AnalysisJobState.CANCEL_REQUESTED, owner.snapshot(jobId)?.state)
        assertEquals(
            listOf(AnalysisJobState.ACTIVE, AnalysisJobState.CANCEL_REQUESTED),
            observed.map(AnalysisJobSnapshot::state),
        )

        engine.acknowledgeCancel()

        assertTrue(callbackInvoked)
        assertEquals(AnalysisJobState.CANCELLED, owner.snapshot(jobId)?.state)
        assertTrue(owner.outcome(jobId) is AnalysisJobOutcome.Cancelled)
        assertEquals(
            listOf(
                AnalysisJobState.ACTIVE,
                AnalysisJobState.CANCEL_REQUESTED,
                AnalysisJobState.CANCELLED,
            ),
            observed.map(AnalysisJobSnapshot::state),
        )
    }

    @Test
    fun `new job is rejected while previous job remains active`() {
        val engine = FakeAnalysisJobEngine()
        val owner = ProcessLocalAnalysisJobOwner(engine)
        owner.start(AnalysisJobId("job-first"), AnalysisOperationId("operation-first"), request())

        var rejected = false
        try {
            owner.start(AnalysisJobId("job-second"), AnalysisOperationId("operation-second"), request())
        } catch (_: IllegalStateException) {
            rejected = true
        }

        assertTrue(rejected)
        assertEquals(1, engine.startCalls)
        assertEquals(AnalysisJobId("job-first"), owner.currentSnapshot()?.jobId)
    }

    @Test
    fun `failure snapshot keeps only typed failure identity while outcome stays process local`() {
        val engine = FakeAnalysisJobEngine()
        val owner = ProcessLocalAnalysisJobOwner(engine)
        val jobId = AnalysisJobId("job-failed")
        owner.start(jobId, AnalysisOperationId("operation-failed"), request())

        engine.complete(
            Result.failure(
                DocumentAnalysisException(DocumentAnalysisFailureCode.CHUNK_FAILED),
            ),
        )

        val snapshot = owner.snapshot(jobId)
        assertEquals(AnalysisJobState.FAILED, snapshot?.state)
        assertEquals(DocumentAnalysisFailureCode.CHUNK_FAILED, snapshot?.failureCode)
        assertTrue(owner.outcome(jobId) is AnalysisJobOutcome.Failure)

        owner.clearTerminal(jobId)
        assertNull(owner.snapshot(jobId))
        assertNull(owner.outcome(jobId))
    }

    private fun request(): DocumentAnalysisRequest =
        DocumentAnalysisRequest(
            segments =
                listOf(
                    DocumentSegment(
                        id = SegmentId.fromIndices(0, 0),
                        pageIndex = 0,
                        blockIndex = 0,
                        normalizedText = "Contact alice@example.test",
                    ),
                ),
            definitions =
                listOf(
                    PiiDefinition(
                        id = PiiTypeId.parse("email"),
                        label = "Email",
                        definition = "Personal email address",
                        source = PiiDefinitionSource.BUILT_IN,
                        semanticCategory = PiiSemanticCategory.CONTACT,
                    ),
                ),
        )

    private class FakeAnalysisJobEngine : AnalysisJobEngine {
        var startCalls = 0
        var cancelCalls = 0
        private var resultCallback: ((Result<List<ValidatedFinding>>) -> Unit)? = null
        private var cancelCallback: (() -> Unit)? = null

        override fun start(
            operationId: AnalysisOperationId,
            request: DocumentAnalysisRequest,
            onResult: (Result<List<ValidatedFinding>>) -> Unit,
        ) {
            startCalls += 1
            resultCallback = onResult
        }

        override fun cancel(
            operationId: AnalysisOperationId,
            onCancelled: () -> Unit,
        ) {
            cancelCalls += 1
            cancelCallback = onCancelled
        }

        fun complete(result: Result<List<ValidatedFinding>>) {
            resultCallback?.invoke(result)
        }

        fun acknowledgeCancel() {
            cancelCallback?.invoke()
        }
    }
}
