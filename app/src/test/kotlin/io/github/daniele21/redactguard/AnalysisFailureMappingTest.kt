package io.github.daniele21.redactguard

import io.github.daniele21.redactguard.domain.analysis.DocumentAnalysisFailureCode
import io.github.daniele21.redactguard.domain.analysis.LocalAiRuntimeState
import io.github.daniele21.redactguard.domain.failure.ProductFailureKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnalysisFailureMappingTest {
    @Test
    fun `analysis causes preserve distinct canonical identities`() {
        val mapped = DocumentAnalysisFailureCode.entries.associateWith { AnalysisFailureMapper.fromCode(it).kind }

        assertEquals(ProductFailureKind.PLAN_REJECTED, mapped[DocumentAnalysisFailureCode.PLAN_REJECTED])
        assertEquals(ProductFailureKind.INVALID_STRUCTURED_RESULT, mapped[DocumentAnalysisFailureCode.INVALID_STRUCTURED_RESULT])
        assertEquals(ProductFailureKind.INVALID_FINDINGS, mapped[DocumentAnalysisFailureCode.INVALID_FINDINGS])
        assertEquals(ProductFailureKind.HOST_UNAVAILABLE, mapped[DocumentAnalysisFailureCode.HOST_UNAVAILABLE])
        assertEquals(ProductFailureKind.CAPABILITY_INCOMPATIBLE, mapped[DocumentAnalysisFailureCode.CAPABILITY_INCOMPATIBLE])
        assertEquals(ProductFailureKind.CHUNK_FAILED, mapped[DocumentAnalysisFailureCode.CHUNK_FAILED])
        assertEquals(ProductFailureKind.DISCONNECTED, mapped[DocumentAnalysisFailureCode.DISCONNECTED])
        assertEquals(ProductFailureKind.CANCELLED, mapped[DocumentAnalysisFailureCode.CANCELLED])
        assertEquals(DocumentAnalysisFailureCode.entries.size, mapped.values.toSet().size)
    }

    @Test
    fun `analysis operation identity survives failure mapping`() {
        val failure = AnalysisFailureMapper.fromCode(DocumentAnalysisFailureCode.INVALID_STRUCTURED_RESULT, "analysis-42")

        assertEquals("analysis-42", failure.operationId)
        assertEquals("RG-AI-006", failure.code)
    }

    @Test
    fun `connection non failures do not produce failures`() {
        assertNull(ConnectionFailureMapper.fromRuntimeState(LocalAiRuntimeState.CONNECTED))
        assertNull(ConnectionFailureMapper.fromRuntimeState(LocalAiRuntimeState.CONNECTING))
    }

    @Test
    fun `permission and installation failures remain distinguishable`() {
        assertEquals(
            ProductFailureKind.PERMISSION_DENIED,
            ConnectionFailureMapper.fromRuntimeState(LocalAiRuntimeState.PERMISSION_DENIED)?.kind,
        )
        assertEquals(
            ProductFailureKind.HOST_NOT_INSTALLED,
            ConnectionFailureMapper.fromRuntimeState(LocalAiRuntimeState.HOST_NOT_INSTALLED)?.kind,
        )
    }
}
