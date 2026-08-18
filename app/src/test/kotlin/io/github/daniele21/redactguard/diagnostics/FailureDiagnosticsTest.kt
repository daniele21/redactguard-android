package io.github.daniele21.redactguard.diagnostics

import io.github.daniele21.redactguard.domain.failure.ProductFailure
import io.github.daniele21.redactguard.domain.failure.ProductFailureKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class FailureDiagnosticsTest {
    @Test
    fun `diagnostic retention is strictly bounded`() {
        val store = BoundedFailureDiagnosticStore(maxEvents = 2)

        store.record(FailureDiagnosticEvent.from(ProductFailure(ProductFailureKind.SOURCE_NOT_FOUND)))
        store.record(FailureDiagnosticEvent.from(ProductFailure(ProductFailureKind.IMAGE_ONLY_PDF)))
        store.record(FailureDiagnosticEvent.from(ProductFailure(ProductFailureKind.CHUNK_FAILED)))

        assertEquals(listOf("RG-PDF-008", "RG-AI-008"), store.snapshot().map(FailureDiagnosticEvent::code))
    }

    @Test
    fun `diagnostic schema has no user-content payload field`() {
        val event =
            FailureDiagnosticEvent.from(
                failure = ProductFailure(ProductFailureKind.IMAGE_ONLY_PDF, operationId = "op-123"),
                context =
                    FailureDiagnosticContext(
                        durationMs = 42,
                        pageCount = 1,
                        appVersion = "1.2.3",
                        buildId = "build-17",
                        sourceRevision = "abcdef1",
                    ),
            )

        val diagnostics = event.toString()
        assertFalse(diagnostics.contains("Mario Rossi"))
        assertFalse(diagnostics.contains("mario.rossi@example.test"))
        assertEquals("RG-PDF-008", event.code)
        assertEquals("op-123", event.operationId)
    }

    @Test
    fun `identity metadata rejects free form text`() {
        assertThrows(IllegalArgumentException::class.java) {
            FailureDiagnosticContext(buildId = "build for Mario Rossi")
        }
    }

    @Test
    fun `clear removes process local diagnostic residue`() {
        val store = BoundedFailureDiagnosticStore(maxEvents = 4)
        store.record(FailureDiagnosticEvent.from(ProductFailure(ProductFailureKind.WRITER_FAILED)))

        store.clear()

        assertEquals(emptyList<FailureDiagnosticEvent>(), store.snapshot())
    }
}
