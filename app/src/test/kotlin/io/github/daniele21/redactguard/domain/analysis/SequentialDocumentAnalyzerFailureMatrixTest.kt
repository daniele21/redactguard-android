package io.github.daniele21.redactguard.domain.analysis

import io.github.daniele21.redactguard.domain.document.DocumentSegment
import io.github.daniele21.redactguard.domain.document.SegmentId
import io.github.daniele21.redactguard.domain.pii.PiiDefinition
import io.github.daniele21.redactguard.domain.pii.PiiDefinitionSource
import io.github.daniele21.redactguard.domain.pii.PiiSemanticCategory
import io.github.daniele21.redactguard.domain.pii.PiiTypeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SequentialDocumentAnalyzerFailureMatrixTest {
    private val email =
        PiiDefinition(
            id = PiiTypeId.parse("email"),
            label = "Email",
            definition = "Personal email address",
            source = PiiDefinitionSource.BUILT_IN,
            semanticCategory = PiiSemanticCategory.CONTACT,
        )

    @Test
    fun `every typed runtime failure maps consistently from prepare and generation and preserves safe diagnostics`() {
        assertEquals(AnalysisRuntimeFailureCode.entries.toSet(), EXPECTED_FAILURES.keys)
        FailurePhase.entries.forEach { phase ->
            EXPECTED_FAILURES.forEach { (runtimeCode, documentCode) ->
                val diagnostic =
                    AnalysisRuntimeDiagnostic(
                        step = if (phase == FailurePhase.PREPARE) "consumer.prepare" else "consumer.generate",
                        type = runtimeCode.name.replace('_', '-'),
                    )
                val runtimeFailure = AnalysisRuntimeException(runtimeCode, diagnostic)
                val runtime = MatrixRuntime(phase, runtimeFailure)
                val analyzer = SequentialDocumentAnalyzer(runtime)
                var result: Result<List<ValidatedFinding>>? = null

                analyzer.analyze(
                    AnalysisOperationId("matrix-${phase.name.lowercase()}-${runtimeCode.name.lowercase()}"),
                    DocumentAnalysisRequest(listOf(segment(0, "Contact alice@example.test")), listOf(email)),
                ) { result = it }

                val failure = result!!.exceptionOrNull() as DocumentAnalysisException
                assertEquals(
                    "Unexpected mapping for phase=$phase runtimeCode=$runtimeCode",
                    documentCode,
                    failure.code,
                )
                assertEquals(diagnostic, failure.runtimeDiagnostic)
                assertEquals(1, runtime.closeCalls)
                assertEquals(if (phase == FailurePhase.GENERATE) 1 else 0, runtime.generateCalls)
            }
        }
    }

    @Test
    fun `late generation failure remains atomic after an earlier chunk produced a valid finding`() {
        val first = segment(0, "Contact alice@example.test " + "A".repeat(120))
        val second = segment(1, "Contact bob@example.test " + "B".repeat(120))
        val limits = limitsThatFitOneButNotBoth(first, second)
        val diagnostic = AnalysisRuntimeDiagnostic("consumer.generate", "RUNTIME-FAILURE")
        val runtime = LateFailureRuntime(limits, diagnostic)
        val analyzer =
            SequentialDocumentAnalyzer(
                runtime = runtime,
                planner = AnalysisChunkPlanner(AnalysisPlanningPolicy(templateOverheadCharacters = 0)),
            )
        var result: Result<List<ValidatedFinding>>? = null

        analyzer.analyze(
            AnalysisOperationId("late-runtime-failure"),
            DocumentAnalysisRequest(listOf(first, second), listOf(email)),
        ) { result = it }

        assertTrue(result!!.isFailure)
        val failure = result!!.exceptionOrNull() as DocumentAnalysisException
        assertEquals(DocumentAnalysisFailureCode.CHUNK_FAILED, failure.code)
        assertEquals(diagnostic, failure.runtimeDiagnostic)
        assertEquals(listOf(0, 1), runtime.generatedOrdinals)
        assertEquals(1, runtime.closeCalls)
    }

    private fun limitsThatFitOneButNotBoth(
        first: DocumentSegment,
        second: DocumentSegment,
    ): AnalysisLimits {
        val firstLength =
            AnalysisProtocol.instruction.length +
                AnalysisDataSerializer.serialize(listOf(email), listOf(AnalysisDataSerializer.fromDocumentSegment(first))).length
        val secondLength =
            AnalysisProtocol.instruction.length +
                AnalysisDataSerializer.serialize(listOf(email), listOf(AnalysisDataSerializer.fromDocumentSegment(second))).length
        val bothLength =
            AnalysisProtocol.instruction.length +
                AnalysisDataSerializer
                    .serialize(
                        listOf(email),
                        listOf(
                            AnalysisDataSerializer.fromDocumentSegment(first),
                            AnalysisDataSerializer.fromDocumentSegment(second),
                        ),
                    ).length
        val maxInput = maxOf(firstLength, secondLength)
        check(maxInput < bothLength)
        return AnalysisLimits(maxInput, AnalysisProtocol.outputJsonSchema.length)
    }

    private fun segment(
        block: Int,
        text: String,
    ) = DocumentSegment(
        id = SegmentId.fromIndices(0, block),
        pageIndex = 0,
        blockIndex = block,
        normalizedText = text,
    )

    private enum class FailurePhase {
        PREPARE,
        GENERATE,
    }

    private class MatrixRuntime(
        private val phase: FailurePhase,
        private val failure: AnalysisRuntimeException,
    ) : AnalysisRuntimePort {
        var generateCalls = 0
        var closeCalls = 0

        override fun prepare(
            operationId: AnalysisOperationId,
            onResult: (Result<AnalysisLimits>) -> Unit,
        ) {
            if (phase == FailurePhase.PREPARE) {
                onResult(Result.failure(failure))
            } else {
                onResult(Result.success(AnalysisLimits(20_000, 20_000)))
            }
        }

        override fun generate(
            operationId: AnalysisOperationId,
            chunk: AnalysisChunk,
            onResult: (Result<String>) -> Unit,
        ) {
            generateCalls += 1
            if (phase == FailurePhase.GENERATE) {
                onResult(Result.failure(failure))
            } else {
                onResult(Result.success("{\"schemaVersion\":1,\"findings\":[]}"))
            }
        }

        override fun cancel(
            operationId: AnalysisOperationId,
            onCancelled: () -> Unit,
        ) = onCancelled()

        override fun close(operationId: AnalysisOperationId) {
            closeCalls += 1
        }
    }

    private class LateFailureRuntime(
        private val limits: AnalysisLimits,
        private val diagnostic: AnalysisRuntimeDiagnostic,
    ) : AnalysisRuntimePort {
        val generatedOrdinals = mutableListOf<Int>()
        var closeCalls = 0

        override fun prepare(
            operationId: AnalysisOperationId,
            onResult: (Result<AnalysisLimits>) -> Unit,
        ) = onResult(Result.success(limits))

        override fun generate(
            operationId: AnalysisOperationId,
            chunk: AnalysisChunk,
            onResult: (Result<String>) -> Unit,
        ) {
            generatedOrdinals += chunk.ordinal
            if (chunk.ordinal == 0) {
                val segment = chunk.segments.single()
                onResult(
                    Result.success(
                        "{\"schemaVersion\":1,\"findings\":[{\"typeId\":\"email\",\"surface\":\"alice@example.test\",\"segmentId\":\"${segment.segmentId}\"}]}",
                    ),
                )
            } else {
                onResult(
                    Result.failure(
                        AnalysisRuntimeException(
                            AnalysisRuntimeFailureCode.GENERATION_FAILED,
                            diagnostic,
                        ),
                    ),
                )
            }
        }

        override fun cancel(
            operationId: AnalysisOperationId,
            onCancelled: () -> Unit,
        ) = onCancelled()

        override fun close(operationId: AnalysisOperationId) {
            closeCalls += 1
        }
    }

    private companion object {
        val EXPECTED_FAILURES =
            mapOf(
                AnalysisRuntimeFailureCode.HOST_UNAVAILABLE to DocumentAnalysisFailureCode.HOST_UNAVAILABLE,
                AnalysisRuntimeFailureCode.CONFIGURATION_REQUIRED to DocumentAnalysisFailureCode.CONFIGURATION_REQUIRED,
                AnalysisRuntimeFailureCode.MODEL_UNAVAILABLE to DocumentAnalysisFailureCode.MODEL_UNAVAILABLE,
                AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE to DocumentAnalysisFailureCode.CAPABILITY_INCOMPATIBLE,
                AnalysisRuntimeFailureCode.INVALID_REQUEST to DocumentAnalysisFailureCode.INVALID_REQUEST,
                AnalysisRuntimeFailureCode.GENERATION_FAILED to DocumentAnalysisFailureCode.CHUNK_FAILED,
                AnalysisRuntimeFailureCode.DISCONNECTED to DocumentAnalysisFailureCode.DISCONNECTED,
                AnalysisRuntimeFailureCode.HOST_PROCESS_LOST to DocumentAnalysisFailureCode.HOST_PROCESS_LOST,
                AnalysisRuntimeFailureCode.CANCELLED to DocumentAnalysisFailureCode.CANCELLED,
                AnalysisRuntimeFailureCode.INTERNAL_FAILURE to DocumentAnalysisFailureCode.LOCAL_AI_INTERNAL,
            )
    }
}
