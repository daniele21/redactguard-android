package io.github.daniele21.redactguard.domain.failure

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductFailureTest {
    @Test
    fun `stable failure codes are unique`() {
        val codes = ProductFailureKind.entries.map(ProductFailureKind::stableCode)

        assertEquals(codes.size, codes.toSet().size)
        assertTrue(codes.all { it.matches(Regex("^RG-[A-Z]+-[0-9]{3}$")) })
    }

    @Test
    fun `image only PDF has a distinct stable identity and recovery`() {
        val kind = ProductFailureKind.IMAGE_ONLY_PDF

        assertEquals("RG-PDF-008", kind.stableCode)
        assertEquals(FailureStage.PARSE, kind.stage)
        assertEquals(FailureRecoveryAction.USE_TEXT_PDF, kind.recoveryAction)
        assertFalse(kind.retryable)
    }

    @Test
    fun `failure diagnostics do not expose operation ID`() {
        val sensitiveOperationId = "operation-customer-123"
        val failure = ProductFailure(ProductFailureKind.CHUNK_FAILED, sensitiveOperationId)

        val diagnostics = failure.toString()

        assertFalse(diagnostics.contains(sensitiveOperationId))
        assertTrue(diagnostics.contains("RG-AI-008"))
        assertTrue(diagnostics.contains("hasOperationId=true"))
    }

    @Test
    fun `unknown internal failure is explicit rather than reusing a domain code`() {
        val unknown = ProductFailureKind.UNKNOWN_INTERNAL

        assertEquals("RG-SYS-001", unknown.stableCode)
        assertEquals(FailureCategory.INTERNAL, unknown.category)
    }
}
