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

class SequentialDocumentAnalyzerTest {
    private val email =
        PiiDefinition(
            id = PiiTypeId.parse("email"),
            label = "Email",
            definition = "Personal email address",
            source = PiiDefinitionSource.BUILT_IN,
            semanticCategory = PiiSemanticCategory.CONTACT,
        )

    @Test
    fun `multiple chunks complete sequentially and expose only merged validated findings`() {
        val first = segment(0, "Contact alice@example.test " + "A".repeat(120))
        val second = segment(1, "Contact bob@example.test " + "B".repeat(120))
        val limits = limitsThatFitOneButNotBoth(first, second)
        val runtime = ScriptedRuntime(limits = limits)
        val analyzer =
            SequentialDocumentAnalyzer(
                runtime = runtime,
                planner = AnalysisChunkPlanner(AnalysisPlanningPolicy(templateOverheadCharacters = 0)),
            )
        var result: Result<List<ValidatedFinding>>? = null

        analyzer.analyze(
            AnalysisOperationId("multi"),
            DocumentAnalysisRequest(listOf(first, second), listOf(email)),
        ) { result = it }

        val findings = result!!.getOrThrow()
        assertEquals(listOf(0, 1), runtime.generatedOrdinals)
        assertEquals(2, findings.size)
        assertEquals(
            listOf("alice@example.test", "bob@example.test"),
            findings.map(ValidatedFinding::surface),
        )
        assertEquals(1, runtime.closeCalls)
    }

    @Test
    fun `malformed later chunk fails atomically without partial findings`() {
        val first = segment(0, "Contact alice@example.test " + "A".repeat(120))
        val second = segment(1, "Contact bob@example.test " + "B".repeat(120))
        val runtime =
            ScriptedRuntime(
                limits = limitsThatFitOneButNotBoth(first, second),
                outputForOrdinal = { ordinal, chunk ->
                    if (ordinal == 0) validOutput(chunk) else "not-json"
                },
            )
        val analyzer =
            SequentialDocumentAnalyzer(
                runtime = runtime,
                planner = AnalysisChunkPlanner(AnalysisPlanningPolicy(templateOverheadCharacters = 0)),
            )
        var result: Result<List<ValidatedFinding>>? = null

        analyzer.analyze(
            AnalysisOperationId("malformed"),
            DocumentAnalysisRequest(listOf(first, second), listOf(email)),
        ) { result = it }

        val failure = result!!.exceptionOrNull() as DocumentAnalysisException
        assertEquals(DocumentAnalysisFailureCode.INVALID_STRUCTURED_RESULT, failure.code)
        assertEquals(listOf(0, 1), runtime.generatedOrdinals)
        assertEquals(1, runtime.closeCalls)
    }

    @Test
    fun `runtime close failure after valid findings has a distinct cleanup cause`() {
        val source = segment(0, "Contact alice@example.test")
        val runtime =
            ScriptedRuntime(
                limits = AnalysisLimits(20_000, 20_000),
                closeFailure = IllegalStateException("close failed"),
            )
        val analyzer = SequentialDocumentAnalyzer(runtime)
        var result: Result<List<ValidatedFinding>>? = null

        analyzer.analyze(
            AnalysisOperationId("close-failure"),
            DocumentAnalysisRequest(listOf(source), listOf(email)),
        ) { result = it }

        val failure = result!!.exceptionOrNull() as DocumentAnalysisException
        assertEquals(DocumentAnalysisFailureCode.RUNTIME_CLEANUP_FAILED, failure.code)
        assertEquals(1, runtime.closeCalls)
    }

    @Test
    fun `unknown runtime exception does not masquerade as chunk failure`() {
        val source = segment(0, "Contact alice@example.test")
        val runtime =
            ScriptedRuntime(
                limits = AnalysisLimits(20_000, 20_000),
                generationFailure = IllegalStateException("unexpected runtime failure"),
            )
        val analyzer = SequentialDocumentAnalyzer(runtime)
        var result: Result<List<ValidatedFinding>>? = null

        analyzer.analyze(
            AnalysisOperationId("unknown-runtime"),
            DocumentAnalysisRequest(listOf(source), listOf(email)),
        ) { result = it }

        val failure = result!!.exceptionOrNull() as DocumentAnalysisException
        assertEquals(DocumentAnalysisFailureCode.INTERNAL_FAILURE, failure.code)
        assertEquals(1, runtime.closeCalls)
    }

    @Test
    fun `cancellation stops active operation and never emits a partial result`() {
        val source = segment(0, "Contact alice@example.test")
        val runtime = ScriptedRuntime(limits = AnalysisLimits(20_000, 20_000), holdGeneration = true)
        val analyzer = SequentialDocumentAnalyzer(runtime)
        val operationId = AnalysisOperationId("cancel")
        var result: Result<List<ValidatedFinding>>? = null
        var cancelled = false

        analyzer.analyze(operationId, DocumentAnalysisRequest(listOf(source), listOf(email))) { result = it }
        assertEquals(listOf(0), runtime.generatedOrdinals)
        assertNull(result)

        analyzer.cancel(operationId) { cancelled = true }

        assertTrue(cancelled)
        assertNull(result)
        assertEquals(1, runtime.cancelCalls)
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

    private fun validOutput(chunk: AnalysisChunk): String {
        val segment = chunk.segments.single()
        val surface = if ("alice@example.test" in segment.text) "alice@example.test" else "bob@example.test"
        return structuredOutput(surface, segment.segmentId)
    }

    private class ScriptedRuntime(
        private val limits: AnalysisLimits,
        private val outputForOrdinal: (Int, AnalysisChunk) -> String = { _, chunk -> validOutputStatic(chunk) },
        private val holdGeneration: Boolean = false,
        private val generationFailure: Throwable? = null,
        private val closeFailure: Throwable? = null,
    ) : AnalysisRuntimePort {
        val generatedOrdinals = mutableListOf<Int>()
        var cancelCalls = 0
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
            if (holdGeneration) return
            val failure = generationFailure
            if (failure == null) {
                onResult(Result.success(outputForOrdinal(chunk.ordinal, chunk)))
            } else {
                onResult(Result.failure(failure))
            }
        }

        override fun cancel(
            operationId: AnalysisOperationId,
            onCancelled: () -> Unit,
        ) {
            cancelCalls += 1
            onCancelled()
        }

        override fun close(operationId: AnalysisOperationId) {
            closeCalls += 1
            closeFailure?.let { throw it }
        }

        companion object {
            private fun validOutputStatic(chunk: AnalysisChunk): String {
                val segment = chunk.segments.single()
                val surface = if ("alice@example.test" in segment.text) "alice@example.test" else "bob@example.test"
                return structuredOutput(surface, segment.segmentId)
            }
        }
    }

    companion object {
        private fun structuredOutput(
            surface: String,
            segmentId: String,
        ): String =
            buildString {
                append("{\"schemaVersion\":1,\"findings\":[{\"typeId\":\"email\",\"surface\":\"")
                append(surface)
                append("\",\"segmentId\":\"")
                append(segmentId)
                append("\"}]}")
            }
    }
}
