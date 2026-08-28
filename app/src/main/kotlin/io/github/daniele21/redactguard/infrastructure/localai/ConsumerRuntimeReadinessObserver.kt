package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.contracts.ConsumerActivationId
import io.github.daniele21.localllm.contracts.ConsumerPreparationAction as HostPreparationAction
import io.github.daniele21.localllm.contracts.ConsumerRuntimeIssue
import io.github.daniele21.localllm.contracts.ConsumerRuntimePhase
import io.github.daniele21.localllm.contracts.ConsumerRuntimeReadiness
import io.github.daniele21.localllm.contracts.ConsumerRuntimeReadinessClient
import io.github.daniele21.localllm.contracts.ConsumerRuntimeReadinessResult
import io.github.daniele21.redactguard.domain.analysis.AnalysisOperationId
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeException
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeFailureCode
import io.github.daniele21.redactguard.domain.analysis.LocalAiExecutionPhase
import io.github.daniele21.redactguard.domain.analysis.LocalAiExecutionState
import io.github.daniele21.redactguard.domain.analysis.LocalAiPreparationAction
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal fun interface LocalAiRuntimeReadinessObserver {
    fun observe(operationId: AnalysisOperationId, activationId: ConsumerActivationId): AutoCloseable
}

/**
 * Bounded read-only poller for Harness runtime readiness. It never prepares a model or opens a
 * session; its only lifetime is the matching analysis activation.
 */
internal class ConsumerRuntimeReadinessObserver(
    private val client: ConsumerRuntimeReadinessClient,
    private val scheduler: ScheduledExecutorService,
    private val transportConnected: () -> Boolean,
    private val onStateChanged: (AnalysisOperationId, LocalAiExecutionState) -> Unit,
    private val pollIntervalMs: Long = DEFAULT_POLL_INTERVAL_MS,
) : LocalAiRuntimeReadinessObserver {
    init {
        require(pollIntervalMs in MIN_POLL_INTERVAL_MS..MAX_POLL_INTERVAL_MS)
    }

    override fun observe(operationId: AnalysisOperationId, activationId: ConsumerActivationId): AutoCloseable {
        onStateChanged(operationId, read(activationId))
        val closed = AtomicBoolean(false)
        val future =
            try {
                scheduler.scheduleWithFixedDelay(
                    {
                        if (closed.get()) return@scheduleWithFixedDelay
                        val state =
                            try {
                                read(activationId)
                            } catch (failure: RuntimeException) {
                                LocalAiExecutionState(
                                    phase = LocalAiExecutionPhase.FAILED,
                                    failureCode = (failure as? AnalysisRuntimeException)?.code
                                        ?: AnalysisRuntimeFailureCode.INTERNAL_FAILURE,
                                )
                            }
                        if (!closed.get()) onStateChanged(operationId, state)
                    },
                    pollIntervalMs,
                    pollIntervalMs,
                    TimeUnit.MILLISECONDS,
                )
            } catch (_: RejectedExecutionException) {
                throw AnalysisRuntimeException(AnalysisRuntimeFailureCode.DISCONNECTED)
            }
        return AutoCloseable {
            if (closed.compareAndSet(false, true)) future.cancel(false)
        }
    }

    private fun read(activationId: ConsumerActivationId): LocalAiExecutionState =
        when (val result = localAiBoundary(STEP_RUNTIME_READINESS) { client.runtimeReadiness(activationId) }) {
            is ConsumerRuntimeReadinessResult.Available -> result.readiness.toLocalAiExecutionState(activationId)
            is ConsumerRuntimeReadinessResult.Rejected -> {
                throw AnalysisRuntimeException(result.failure.toAnalysisFailureCode(transportConnected))
            }
        }

    private companion object {
        const val STEP_RUNTIME_READINESS = "runtime.readiness"
        const val DEFAULT_POLL_INTERVAL_MS = 150L
        const val MIN_POLL_INTERVAL_MS = 100L
        const val MAX_POLL_INTERVAL_MS = 1_000L
    }
}

internal fun ConsumerRuntimeReadiness.toLocalAiExecutionState(expectedActivationId: ConsumerActivationId): LocalAiExecutionState {
    if (activationId != expectedActivationId) {
        throw AnalysisRuntimeException(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)
    }
    return when (phase) {
        ConsumerRuntimePhase.IDLE -> LocalAiExecutionState(LocalAiExecutionPhase.ACTIVATED)
        ConsumerRuntimePhase.PREPARING -> LocalAiExecutionState(
            phase = LocalAiExecutionPhase.PREPARING,
            preparationAction = preparationAction.toLocalAction(),
        )

        ConsumerRuntimePhase.READY -> LocalAiExecutionState(LocalAiExecutionPhase.READY)
        ConsumerRuntimePhase.GENERATING -> LocalAiExecutionState(LocalAiExecutionPhase.GENERATING)
        ConsumerRuntimePhase.FAILED -> LocalAiExecutionState(
            phase = LocalAiExecutionPhase.FAILED,
            failureCode = requireNotNull(issue).toAnalysisFailureCode(),
            retryable = retryable,
        )
    }
}

private fun HostPreparationAction.toLocalAction(): LocalAiPreparationAction =
    when (this) {
        HostPreparationAction.NONE -> throw AnalysisRuntimeException(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)
        HostPreparationAction.LOADING -> LocalAiPreparationAction.LOADING
        HostPreparationAction.REUSING -> LocalAiPreparationAction.REUSING
        HostPreparationAction.SWITCHING -> LocalAiPreparationAction.SWITCHING
    }

private fun ConsumerRuntimeIssue.toAnalysisFailureCode(): AnalysisRuntimeFailureCode =
    when (this) {
        ConsumerRuntimeIssue.MODEL_UNAVAILABLE,
        ConsumerRuntimeIssue.MODEL_CONFLICT,
        -> AnalysisRuntimeFailureCode.HOST_UNAVAILABLE

        ConsumerRuntimeIssue.CONFIGURATION_STALE -> AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE

        ConsumerRuntimeIssue.PREPARATION_FAILED,
        ConsumerRuntimeIssue.RUNTIME_FAILED,
        -> AnalysisRuntimeFailureCode.GENERATION_FAILED

        ConsumerRuntimeIssue.CANCELLED -> AnalysisRuntimeFailureCode.CANCELLED
    }
