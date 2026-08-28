package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.contracts.ConsumerActivationId
import io.github.daniele21.localllm.contracts.ConsumerPreparationAction
import io.github.daniele21.localllm.contracts.ConsumerRuntimeIssue
import io.github.daniele21.localllm.contracts.ConsumerRuntimePhase
import io.github.daniele21.localllm.contracts.ConsumerRuntimeReadiness
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeException
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeFailureCode
import io.github.daniele21.redactguard.domain.analysis.LocalAiExecutionPhase
import io.github.daniele21.redactguard.domain.analysis.LocalAiPreparationAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsumerRuntimeReadinessProjectionTest {
    @Test
    fun `preparing actions project without model identity`() {
        val activationId = ConsumerActivationId("activation-1")

        val loading =
            ConsumerRuntimeReadiness(
                activationId = activationId,
                phase = ConsumerRuntimePhase.PREPARING,
                preparationAction = ConsumerPreparationAction.LOADING,
            ).toLocalAiExecutionState(activationId)
        val reusing =
            ConsumerRuntimeReadiness(
                activationId = activationId,
                phase = ConsumerRuntimePhase.PREPARING,
                preparationAction = ConsumerPreparationAction.REUSING,
            ).toLocalAiExecutionState(activationId)
        val switching =
            ConsumerRuntimeReadiness(
                activationId = activationId,
                phase = ConsumerRuntimePhase.PREPARING,
                preparationAction = ConsumerPreparationAction.SWITCHING,
            ).toLocalAiExecutionState(activationId)

        assertEquals(LocalAiExecutionPhase.PREPARING, loading.phase)
        assertEquals(LocalAiPreparationAction.LOADING, loading.preparationAction)
        assertEquals(LocalAiPreparationAction.REUSING, reusing.preparationAction)
        assertEquals(LocalAiPreparationAction.SWITCHING, switching.preparationAction)
    }

    @Test
    fun `runtime failure remains product safe and preserves retryability`() {
        val activationId = ConsumerActivationId("activation-1")

        val state =
            ConsumerRuntimeReadiness(
                activationId = activationId,
                phase = ConsumerRuntimePhase.FAILED,
                issue = ConsumerRuntimeIssue.RUNTIME_FAILED,
                retryable = true,
            ).toLocalAiExecutionState(activationId)

        assertEquals(LocalAiExecutionPhase.FAILED, state.phase)
        assertEquals(AnalysisRuntimeFailureCode.GENERATION_FAILED, state.failureCode)
        assertTrue(state.retryable)
    }

    @Test
    fun `wrong activation fails closed`() {
        val readiness =
            ConsumerRuntimeReadiness(
                activationId = ConsumerActivationId("activation-other"),
                phase = ConsumerRuntimePhase.READY,
            )

        val failure =
            runCatching {
                readiness.toLocalAiExecutionState(ConsumerActivationId("activation-expected"))
            }.exceptionOrNull() as AnalysisRuntimeException

        assertEquals(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE, failure.code)
    }
}
