package io.github.daniele21.redactguard.domain.analysis

import io.github.daniele21.redactguard.domain.document.DocumentSegment
import io.github.daniele21.redactguard.domain.pii.PiiDefinition
import java.util.concurrent.ConcurrentHashMap

internal data class DocumentAnalysisRequest(
    val segments: List<DocumentSegment>,
    val definitions: List<PiiDefinition>,
) {
    init {
        require(segments.isNotEmpty()) { "Analysis requires document segments" }
        require(definitions.isNotEmpty()) { "Analysis requires at least one PII definition" }
    }
}

internal enum class DocumentAnalysisFailureCode {
    PLAN_REJECTED,
    INVALID_STRUCTURED_RESULT,
    INVALID_FINDINGS,
    HOST_UNAVAILABLE,
    CAPABILITY_INCOMPATIBLE,
    CHUNK_FAILED,
    DISCONNECTED,
    CANCELLED,
    RUNTIME_CLEANUP_FAILED,
    INTERNAL_FAILURE,
}

internal class DocumentAnalysisException(
    val code: DocumentAnalysisFailureCode,
) : RuntimeException("RedactGuard document analysis failed: $code")

/**
 * App-owned sequential analysis. No partial findings are exposed: every chunk must finish and all
 * untrusted outputs are parsed and source-validated before the final list is emitted.
 */
internal class SequentialDocumentAnalyzer(
    private val runtime: AnalysisRuntimePort,
    private val planner: AnalysisChunkPlanner = AnalysisChunkPlanner(),
) {
    private val operations = ConcurrentHashMap<AnalysisOperationId, ActiveOperation>()

    fun analyze(
        operationId: AnalysisOperationId,
        request: DocumentAnalysisRequest,
        onResult: (Result<List<ValidatedFinding>>) -> Unit,
    ) {
        val operation = ActiveOperation(request, onResult)
        check(operations.putIfAbsent(operationId, operation) == null) { "Duplicate document-analysis operation ID" }
        runtime.prepare(operationId) { prepared ->
            if (!isActive(operationId, operation)) return@prepare
            prepared.fold(
                onSuccess = { limits -> planAndStart(operationId, operation, limits) },
                onFailure = { failure -> completeFailure(operationId, operation, mapRuntimeFailure(failure)) },
            )
        }
    }

    fun cancel(
        operationId: AnalysisOperationId,
        onCancelled: () -> Unit,
    ) {
        val operation = operations[operationId]
        if (operation == null) {
            onCancelled()
            return
        }
        operation.cancelled = true
        runtime.cancel(operationId) {
            if (operations.remove(operationId, operation)) {
                runCatching { runtime.close(operationId) }
            }
            onCancelled()
        }
    }

    private fun planAndStart(
        operationId: AnalysisOperationId,
        operation: ActiveOperation,
        limits: AnalysisLimits,
    ) {
        val result = runCatching { planner.plan(operation.request.segments, operation.request.definitions, limits) }
        val plan = result.getOrNull()
        if (plan !is ChunkPlanResult.Planned) {
            completeFailure(operationId, operation, DocumentAnalysisException(DocumentAnalysisFailureCode.PLAN_REJECTED))
            return
        }
        operation.chunks = plan.chunks
        generateNext(operationId, operation)
    }

    private fun generateNext(
        operationId: AnalysisOperationId,
        operation: ActiveOperation,
    ) {
        if (!isActive(operationId, operation)) return
        val chunk = operation.chunks.getOrNull(operation.nextChunkIndex)
        if (chunk == null) {
            validateMergedResult(operationId, operation)
            return
        }
        runtime.generate(operationId, chunk) { generated ->
            if (!isActive(operationId, operation)) return@generate
            generated.fold(
                onSuccess = { output -> handleChunkOutput(operationId, operation, output) },
                onFailure = { failure -> completeFailure(operationId, operation, mapRuntimeFailure(failure)) },
            )
        }
    }

    private fun handleChunkOutput(
        operationId: AnalysisOperationId,
        operation: ActiveOperation,
        output: String,
    ) {
        val parsed = runCatching { AnalysisResultParser.parse(output) }
        val rawFindings = parsed.getOrNull()
        if (rawFindings == null) {
            completeFailure(
                operationId,
                operation,
                DocumentAnalysisException(DocumentAnalysisFailureCode.INVALID_STRUCTURED_RESULT),
            )
            return
        }
        operation.rawFindings += rawFindings
        operation.nextChunkIndex += 1
        generateNext(operationId, operation)
    }

    private fun validateMergedResult(
        operationId: AnalysisOperationId,
        operation: ActiveOperation,
    ) {
        val validation =
            FindingValidator.validate(
                rawFindings = operation.rawFindings,
                chunks = operation.chunks,
                canonicalSegments = operation.request.segments,
                definitions = operation.request.definitions,
            )
        val findings = (validation as? FindingValidationResult.Valid)?.findings
        if (findings == null) {
            completeFailure(
                operationId,
                operation,
                DocumentAnalysisException(DocumentAnalysisFailureCode.INVALID_FINDINGS),
            )
            return
        }
        if (!operations.remove(operationId, operation)) return
        val closeFailure = runCatching { runtime.close(operationId) }.exceptionOrNull()
        if (closeFailure == null) {
            operation.onResult(Result.success(findings))
        } else {
            operation.onResult(
                Result.failure(
                    DocumentAnalysisException(DocumentAnalysisFailureCode.RUNTIME_CLEANUP_FAILED),
                ),
            )
        }
    }

    private fun completeFailure(
        operationId: AnalysisOperationId,
        operation: ActiveOperation,
        failure: Throwable,
    ) {
        if (!operations.remove(operationId, operation)) return
        runCatching { runtime.close(operationId) }
        operation.onResult(Result.failure(failure))
    }

    private fun mapRuntimeFailure(failure: Throwable): Throwable {
        val code =
            (failure as? AnalysisRuntimeException)?.code
                ?: return DocumentAnalysisException(DocumentAnalysisFailureCode.INTERNAL_FAILURE)
        val mapped =
            when (code) {
                AnalysisRuntimeFailureCode.HOST_UNAVAILABLE -> DocumentAnalysisFailureCode.HOST_UNAVAILABLE
                AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE -> DocumentAnalysisFailureCode.CAPABILITY_INCOMPATIBLE
                AnalysisRuntimeFailureCode.GENERATION_FAILED -> DocumentAnalysisFailureCode.CHUNK_FAILED
                AnalysisRuntimeFailureCode.DISCONNECTED -> DocumentAnalysisFailureCode.DISCONNECTED
                AnalysisRuntimeFailureCode.CANCELLED -> DocumentAnalysisFailureCode.CANCELLED
            }
        return DocumentAnalysisException(mapped)
    }

    private fun isActive(
        operationId: AnalysisOperationId,
        operation: ActiveOperation,
    ): Boolean = operations[operationId] === operation && !operation.cancelled

    private class ActiveOperation(
        val request: DocumentAnalysisRequest,
        val onResult: (Result<List<ValidatedFinding>>) -> Unit,
    ) {
        @Volatile
        var cancelled = false
        var chunks: List<AnalysisChunk> = emptyList()
        var nextChunkIndex = 0
        val rawFindings = mutableListOf<UnvalidatedFinding>()
    }
}
