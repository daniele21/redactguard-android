package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.contracts.ConsumerActivationId
import io.github.daniele21.localllm.contracts.ConsumerActivationRequest
import io.github.daniele21.localllm.contracts.ConsumerControlPlaneClient
import io.github.daniele21.localllm.contracts.UseCaseId
import io.github.daniele21.localllm.transport.binder.client.SharedRuntimeConnectionSnapshot
import io.github.daniele21.localllm.transport.binder.client.SharedRuntimeConnectionState
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeException
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeFailureCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class LocalAiTechnicalDiagnosticsTest {
    @Test
    fun `event rendering contains only bounded technical identity`() {
        val event =
            LocalAiTechnicalEvent(
                step = "control-plane.activate",
                result = "REJECTED",
                reason = "CONFIGURATION_REQUIRED",
                count = 1,
            )

        assertEquals(
            "step=control-plane.activate result=REJECTED reason=CONFIGURATION_REQUIRED count=1",
            event.render(),
        )
    }

    @Test
    fun `event rejects arbitrary free-form reason text`() {
        assertThrows(IllegalArgumentException::class.java) {
            LocalAiTechnicalEvent(
                step = "control-plane.activate",
                result = "REJECTED",
                reason = "document=synthetic-sensitive-payload",
            )
        }
    }

    @Test
    fun `transport snapshot maps unknown detail without logging raw detail`() {
        val event =
            SharedRuntimeConnectionSnapshot(
                state = SharedRuntimeConnectionState.CONNECTION_LOST,
                detail = SENSITIVE_DETAIL,
            ).toTechnicalEvent()

        assertEquals("transport", event.step)
        assertEquals("CONNECTION_LOST", event.result)
        assertEquals("OTHER", event.reason)
        assertFalse(event.render().contains(SENSITIVE_DETAIL))
    }

    @Test
    fun `unexpected control plane failure records safe failing step`() {
        val events = mutableListOf<LocalAiTechnicalEvent>()
        val coordinator =
            ConsumerControlPlaneCoordinator(
                client = DiagnosticsThrowingControlPlaneClient(),
                technicalDiagnostics = LocalAiTechnicalDiagnostics { event -> events.add(event) },
            )

        val failure = runCatching { coordinator.activate() }.exceptionOrNull() as AnalysisRuntimeException

        assertEquals(AnalysisRuntimeFailureCode.INTERNAL_FAILURE, failure.code)
        assertEquals(
            LocalAiTechnicalEvent(
                step = "control-plane.assigned-use-cases",
                result = "FAILED",
                reason = "IllegalStateException",
            ),
            events.single(),
        )
        assertFalse(events.single().render().contains(SENSITIVE_DETAIL))
    }

    private companion object {
        const val SENSITIVE_DETAIL = "synthetic sensitive document payload"
    }
}

private class DiagnosticsThrowingControlPlaneClient : ConsumerControlPlaneClient {
    override fun assignedUseCases() = throw IllegalStateException("synthetic sensitive document payload")

    override fun publishedPresets(useCaseId: UseCaseId) = error("must not be reached")

    override fun activate(request: ConsumerActivationRequest) = error("must not be reached")

    override fun deactivate(activationId: ConsumerActivationId) = error("must not be reached")
}
