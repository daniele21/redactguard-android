package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.contracts.InferencePresetRef
import io.github.daniele21.redactguard.domain.analysis.AnalysisChunk
import io.github.daniele21.redactguard.domain.analysis.AnalysisLimits
import io.github.daniele21.redactguard.domain.analysis.AnalysisOperationId
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeException
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeFailureCode
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimePort
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException

/**
 * Adds Harness assigned-use-case/preset activation lifecycle around the existing strict Consumer
 * inference adapter. One activation is owned for the complete multi-chunk analysis operation and
 * released on success, failure, cancellation or explicit close.
 */
internal class ControlPlaneAnalysisRuntime(
    private val delegate: AnalysisRuntimePort,
    private val controlPlane: ConsumerControlPlaneCoordinator,
    private val lifecycleExecutor: Executor,
    private val selectedPreset: () -> InferencePresetRef? = { null },
) : AnalysisRuntimePort {
    private val operations = ConcurrentHashMap<AnalysisOperationId, OperationState>()

    override fun prepare(
        operationId: AnalysisOperationId,
        onResult: (Result<AnalysisLimits>) -> Unit,
    ) {
        val operation = OperationState(onPrepared = onResult)
        check(operations.putIfAbsent(operationId, operation) == null) { "Duplicate analysis operation ID" }
        try {
            lifecycleExecutor.execute { activateAndPrepare(operationId, operation) }
        } catch (_: RejectedExecutionException) {
            operations.remove(operationId, operation)
            onResult(Result.failure(runtimeFailure(AnalysisRuntimeFailureCode.DISCONNECTED)))
        }
    }

    override fun generate(
        operationId: AnalysisOperationId,
        chunk: AnalysisChunk,
        onResult: (Result<String>) -> Unit,
    ) {
        val operation = operations[operationId]
        val active = operation != null && synchronized(operation) { operation.delegateStarted && !operation.cancelled }
        if (!active) {
            onResult(Result.failure(runtimeFailure(AnalysisRuntimeFailureCode.DISCONNECTED)))
            return
        }
        delegate.generate(operationId, chunk, onResult)
    }

    override fun cancel(
        operationId: AnalysisOperationId,
        onCancelled: () -> Unit,
    ) {
        val operation = operations[operationId]
        if (operation == null) {
            onCancelled()
            return
        }
        val delegateStarted =
            synchronized(operation) {
                if (operation.cancelled) return@synchronized operation.delegateStarted
                operation.cancelled = true
                operation.delegateStarted
            }
        if (!delegateStarted) {
            operations.remove(operationId, operation)
            operation.activation?.let { controlPlane.deactivateBestEffort(it.activationId) }
            onCancelled()
            return
        }
        delegate.cancel(operationId) {
            runCatching { delegate.close(operationId) }
            operations.remove(operationId, operation)
            operation.activation?.let { controlPlane.deactivateBestEffort(it.activationId) }
            onCancelled()
        }
    }

    override fun close(operationId: AnalysisOperationId) {
        val operation = operations.remove(operationId) ?: return
        val delegateStarted = synchronized(operation) { operation.delegateStarted }
        if (delegateStarted) delegate.close(operationId)
        operation.activation?.let { controlPlane.deactivate(it.activationId) }
    }

    private fun activateAndPrepare(
        operationId: AnalysisOperationId,
        operation: OperationState,
    ) {
        if (operations[operationId] !== operation) return
        val activated = runCatching { controlPlane.activate(selectedPreset()) }
        val activation = activated.getOrNull()
        if (activation == null) {
            if (operations.remove(operationId, operation)) {
                operation.onPrepared(
                    Result.failure(activated.exceptionOrNull() ?: runtimeFailure(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)),
                )
            }
            return
        }
        operation.activation = activation
        if (operations[operationId] !== operation) {
            controlPlane.deactivateBestEffort(activation.activationId)
            return
        }
        val cancelled = synchronized(operation) { operation.cancelled }
        if (cancelled) {
            operations.remove(operationId, operation)
            controlPlane.deactivateBestEffort(activation.activationId)
            return
        }
        synchronized(operation) { operation.delegateStarted = true }
        try {
            delegate.prepare(operationId) { prepared -> handlePrepared(operationId, operation, prepared) }
        } catch (failure: Throwable) {
            operations.remove(operationId, operation)
            runCatching { delegate.close(operationId) }
            controlPlane.deactivateBestEffort(activation.activationId)
            operation.onPrepared(Result.failure(failure))
        }
    }

    private fun handlePrepared(
        operationId: AnalysisOperationId,
        operation: OperationState,
        prepared: Result<AnalysisLimits>,
    ) {
        if (operations[operationId] !== operation) return
        if (prepared.isFailure) {
            operations.remove(operationId, operation)
            runCatching { delegate.close(operationId) }
            operation.activation?.let { controlPlane.deactivateBestEffort(it.activationId) }
            operation.onPrepared(prepared)
            return
        }
        val cancelled = synchronized(operation) { operation.cancelled }
        if (cancelled) {
            delegate.cancel(operationId) {
                runCatching { delegate.close(operationId) }
                operations.remove(operationId, operation)
                operation.activation?.let { controlPlane.deactivateBestEffort(it.activationId) }
            }
            return
        }
        operation.onPrepared(prepared)
    }

    private class OperationState(
        val onPrepared: (Result<AnalysisLimits>) -> Unit,
    ) {
        @Volatile
        var activation: AnalysisActivation? = null

        var delegateStarted: Boolean = false
        var cancelled: Boolean = false
    }
}

private fun runtimeFailure(code: AnalysisRuntimeFailureCode): AnalysisRuntimeException = AnalysisRuntimeException(code)
