package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.contracts.ConsumerCapabilityResult
import io.github.daniele21.localllm.contracts.ConsumerControlPlaneClient
import io.github.daniele21.localllm.contracts.ConsumerGenerationListener
import io.github.daniele21.localllm.contracts.ConsumerGenerationRequest
import io.github.daniele21.localllm.contracts.ConsumerGenerationStartResult
import io.github.daniele21.localllm.contracts.ConsumerLocalLlmClient
import io.github.daniele21.localllm.contracts.ConsumerPrepareRequest
import io.github.daniele21.localllm.contracts.ConsumerPrepareResult
import io.github.daniele21.localllm.contracts.ConsumerPreparedId
import io.github.daniele21.localllm.contracts.ConsumerSessionResult
import io.github.daniele21.localllm.contracts.SessionId
import io.github.daniele21.localllm.contracts.UseCaseId
import io.github.daniele21.redactguard.domain.analysis.AnalysisOperationId
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeException
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeFailureCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.concurrent.Executor

class LocalAiFailureBoundaryTest {
    @Test
    fun `control plane unexpected exception is classified without message content`() {
        val coordinator = ConsumerControlPlaneCoordinator(ThrowingControlPlaneClient())

        val failure = runCatching { coordinator.activate() }.exceptionOrNull() as AnalysisRuntimeException

        assertEquals(AnalysisRuntimeFailureCode.INTERNAL_FAILURE, failure.code)
        assertEquals("control-plane.assigned-use-cases", failure.diagnostic?.step)
        assertEquals("IllegalStateException", failure.diagnostic?.type)
        assertFalse(failure.message.orEmpty().contains(SENSITIVE_MESSAGE))
    }

    @Test
    fun `consumer capability unexpected exception keeps consumer step`() {
        val runtime =
            ConsumerAnalysisRuntime(
                client = ThrowingConsumerClient(),
                lifecycleExecutor = Executor(Runnable::run),
            )
        var prepared: Result<*>? = null

        runtime.prepare(AnalysisOperationId("op-boundary")) { prepared = it }

        val failure = prepared!!.exceptionOrNull() as AnalysisRuntimeException
        assertEquals(AnalysisRuntimeFailureCode.INTERNAL_FAILURE, failure.code)
        assertEquals("consumer.capabilities", failure.diagnostic?.step)
        assertEquals("SecurityException", failure.diagnostic?.type)
        assertFalse(failure.message.orEmpty().contains(SENSITIVE_MESSAGE))
    }

    private companion object {
        const val SENSITIVE_MESSAGE = "synthetic-sensitive-payload"
    }
}

private class ThrowingControlPlaneClient : ConsumerControlPlaneClient {
    override fun assignedUseCases() = throw IllegalStateException("synthetic-sensitive-payload")

    override fun publishedPresets(useCaseId: UseCaseId) = error("must not be reached")

    override fun activate(request: io.github.daniele21.localllm.contracts.ConsumerActivationRequest) = error("must not be reached")

    override fun deactivate(activationId: io.github.daniele21.localllm.contracts.ConsumerActivationId) = error("must not be reached")
}

private class ThrowingConsumerClient : ConsumerLocalLlmClient {
    override fun capabilities(useCaseId: UseCaseId): ConsumerCapabilityResult = throw SecurityException("synthetic-sensitive-payload")

    override fun prepare(request: ConsumerPrepareRequest): ConsumerPrepareResult = error("must not be reached")

    override fun createSession(preparedId: ConsumerPreparedId): ConsumerSessionResult = error("must not be reached")

    override fun generate(
        request: ConsumerGenerationRequest,
        listener: ConsumerGenerationListener,
    ): ConsumerGenerationStartResult = error("must not be reached")

    override fun closeSession(sessionId: SessionId) = Unit
}
