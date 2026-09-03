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
 * inference adapter. One activation and one read-only readiness observation are owned for the
 * complete multi-chunk analysis operation and released on success, failure, cancellation or close.
 */
internal class ControlPlaneAnalysisRuntime(
    private val delegate: AnalysisRuntimePort,
    private val controlPlane: ConsumerControlPlaneCoordinator,
    private val readinessObserver: LocalAiRuntimeReadinessObserver,
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
            releaseOperation(operation, bestEffort = true)
            onCancelled()
            return
        }
        delegate.cancel(operationId) {
            runCatching { delegate.close(operationId) }
            operations.remove(operationId, operation)
            releaseOperation(operation, bestEffort = true)
            onCancelled()
        }
    }

    override fun close(operationId: AnalysisOperationId) {
        val operation = operations.remove(operationId) ?: return
        val delegateStarted = synchronized(operation) { operation.delegateStarted }
        if (delegateStarted) delegate.close(operationId)
        releaseOperation(operation, bestEffort = false)
    }

    /**
     * Harness activations are scoped to the Binder client token. Once that connection is lost,
     * Harness releases every activation owned by the old token. Keep the durable data-plane
     * operation alive, but drop connection-scoped control-plane resources so a later close does
     * not try to release an already-invalid activation through the replacement client token.
     */
    internal fun onTransportConnectionInvalidated() {
        operations.values.forEach { operation ->
            val observation =
                synchronized(operation) {
                    operation.connectionScopeInvalidated = true
                    operation.activation = null
                    operation.readinessObservation.also { operation.readinessObservation = null }
                }
            runCatching { observation?.close() }
        }
    }

    private fun activateAndPrepare(
        operationId: AnalysisOperationId,
        operation: OperationState,
    ) {
        if (operations[operationId] !== operation) return
        val preflight = runCatching { controlPlane.inspectSetup(selectedPreset()) }
        val inspection = preflight.getOrNull()
        if (inspection == null) {
            if (operations.remove(operationId, operation)) {
                operation.onPrepared(
                    Result.failure(preflight.exceptionOrNull() ?: runtimeFailure(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)),
                )
            }
            return
        }
        if (operations[operationId] !== operation) return

        val activated = runCatching { controlPlane.activate(inspection) }
        val activation = activated.getOrNull()
        if (activation == null) {
            if (operations.remove(operationId, operation)) {
                operation.onPrepared(
                    Result.failure(activated.exceptionOrNull() ?: runtimeFailure(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)),
                )
            }
            return
        }
        val connectionInvalidated =
            synchronized(operation) {
                operation.activation = activation
                operation.connectionScopeInvalidated
            }
        if (connectionInvalidated) {
            if (operations.remove(operationId, operation)) {
                releaseOperation(operation, bestEffort = true)
                operation.onPrepared(Result.failure(runtimeFailure(AnalysisRuntimeFailureCode.DISCONNECTED)))
            } else {
                releaseOperation(operation, bestEffort = true)
            }
            return
        }
        if (operations[operationId] !== operation) {
            releaseOperation(operation, bestEffort = true)
            return
        }

        val observed = runCatching { readinessObserver.observe(operationId, activation.activationId) }
        val observation = observed.getOrNull()
        if (observation == null) {
            if (operations.remove(operationId, operation)) {
                releaseOperation(operation, bestEffort = true)
                operation.onPrepared(
                    Result.failure(observed.exceptionOrNull() ?: runtimeFailure(AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE)),
                )
            } else {
                releaseOperation(operation, bestEffort = true)
            }
            return
        }
        operation.readinessObservation = observation
        if (operations[operationId] !== operation) {
            releaseOperation(operation, bestEffort = true)
            return
        }

        val cancelled = synchronized(operation) { operation.cancelled }
        if (cancelled) {
            operations.remove(operationId, operation)
            releaseOperation(operation, bestEffort = true)
            return
        }
        synchronized(operation) { operation.delegateStarted = true }
        try {
            delegate.prepare(operationId) { prepared -> handlePrepared(operationId, operation, prepared) }
        } catch (failure: Throwable) {
            operations.remove(operationId, operation)
            runCatching { delegate.close(operationId) }
            releaseOperation(operation, bestEffort = true)
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
            releaseOperation(operation, bestEffort = true)
            operation.onPrepared(prepared)
            return
        }
        val cancelled = synchronized(operation) { operation.cancelled }
        if (cancelled) {
            delegate.cancel(operationId) {
                runCatching { delegate.close(operationId) }
                operations.remove(operationId, operation)
                releaseOperation(operation, bestEffort = true)
            }
            return
        }
        operation.onPrepared(prepared)
    }

    private fun releaseOperation(
        operation: OperationState,
        bestEffort: Boolean,
    ) {
        val resources =
            synchronized(operation) {
                val observation = operation.readinessObservation
                val activation = operation.activation
                operation.readinessObservation = null
                operation.activation = null
                OperationResources(
                    observation = observation,
                    activation = activation,
                    connectionScopeInvalidated = operation.connectionScopeInvalidated,
                )
            }
        runCatching { resources.observation?.close() }
        resources.activation?.let { activation ->
            if (bestEffort || resources.connectionScopeInvalidated) {
                controlPlane.deactivateBestEffort(activation.activationId)
            } else {
                controlPlane.deactivate(activation.activationId)
            }
        }
    }

    private data class OperationResources(
        val observation: AutoCloseable?,
        val activation: AnalysisActivation?,
        val connectionScopeInvalidated: Boolean,
    )

    private class OperationState(
        val onPrepared: (Result<AnalysisLimits>) -> Unit,
    ) {
        @Volatile
        var activation: AnalysisActivation? = null

        @Volatile
        var readinessObservation: AutoCloseable? = null

        var delegateStarted: Boolean = false
        var cancelled: Boolean = false
        var connectionScopeInvalidated: Boolean = false
    }
}

private fun runtimeFailure(code: AnalysisRuntimeFailureCode): AnalysisRuntimeException = AnalysisRuntimeException(code)
