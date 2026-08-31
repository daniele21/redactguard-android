package io.github.daniele21.redactguard

import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeDiagnostic
import io.github.daniele21.redactguard.domain.analysis.DocumentAnalysisException
import io.github.daniele21.redactguard.domain.analysis.DocumentAnalysisFailureCode
import io.github.daniele21.redactguard.domain.failure.ProductFailureKind
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalysisChunkFailurePropagationTest {
    @Test
    fun `chunk failure preserves operation and safe low-level identity through product mapping`() {
        val diagnostic = AnalysisRuntimeDiagnostic("consumer.generate", "Consumer:RUNTIME_FAILURE")

        val failure =
            AnalysisFailureMapper.fromThrowable(
                DocumentAnalysisException(DocumentAnalysisFailureCode.CHUNK_FAILED, diagnostic),
                operationId = "analysis-chunk-42",
            )

        assertEquals(ProductFailureKind.CHUNK_FAILED, failure.kind)
        assertEquals("RG-AI-008", failure.code)
        assertEquals("analysis-chunk-42", failure.operationId)
        assertEquals("consumer.generate", failure.diagnostic?.step)
        assertEquals("Consumer:RUNTIME_FAILURE", failure.diagnostic?.type)
    }
}
