package io.github.daniele21.redactguard.ui

import io.github.daniele21.redactguard.domain.failure.ProductFailure
import io.github.daniele21.redactguard.domain.failure.ProductFailureDiagnostic
import io.github.daniele21.redactguard.domain.failure.ProductFailureKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalFailureProjectionTest {
    @Test
    fun `every canonical failure has technical details and actionable copy`() {
        ProductFailureKind.entries.forEach { kind ->
            val projected = ProductFailureProjector.project(ProductFailure(kind, operationId = "operation-1"))

            assertTrue(projected.title.isNotBlank())
            assertTrue(projected.message.isNotBlank())
            assertNotNull(projected.technicalDetails)
            assertEquals(kind.stableCode, projected.technicalDetails.code)
            assertEquals(kind.name, projected.technicalDetails.cause)
            assertEquals(kind.stage.name, projected.technicalDetails.stage)
            assertEquals("operation-1", projected.technicalDetails.operationId)
        }
    }

    @Test
    fun `local AI failures keep runtime product names out of user titles`() {
        val localAiFailures =
            listOf(
                ProductFailureKind.HOST_NOT_INSTALLED,
                ProductFailureKind.HOST_UNAVAILABLE,
                ProductFailureKind.PERMISSION_DENIED,
                ProductFailureKind.CAPABILITY_INCOMPATIBLE,
                ProductFailureKind.DISCONNECTED,
                ProductFailureKind.RUNTIME_CLEANUP_FAILED,
            )

        localAiFailures.forEach { kind ->
            val projected = ProductFailureProjector.project(ProductFailure(kind))

            assertFalse(projected.title.contains("Harness", ignoreCase = true))
        }
    }

    @Test
    fun `image only PDF never renders the generic omnibus unsupported message`() {
        val projected = ProductFailureProjector.project(ProductFailure(ProductFailureKind.IMAGE_ONLY_PDF))

        assertEquals("PDF senza testo estraibile", projected.title)
        assertTrue(projected.message.contains("OCR"))
        assertFalse(projected.message.contains("cifrato, non valido, troppo grande"))
        assertEquals("RG-PDF-008", projected.technicalDetails.code)
    }

    @Test
    fun `encrypted malformed empty and image only PDF remain visibly distinct`() {
        val kinds =
            listOf(
                ProductFailureKind.ENCRYPTED_PDF,
                ProductFailureKind.MALFORMED_PDF,
                ProductFailureKind.EMPTY_PDF,
                ProductFailureKind.IMAGE_ONLY_PDF,
            )
        val titles = kinds.map { ProductFailureProjector.project(ProductFailure(it)).title }

        assertEquals(titles.size, titles.toSet().size)
    }

    @Test
    fun `pasted text failures have source specific user copy`() {
        val empty = ProductFailureProjector.project(ProductFailure(ProductFailureKind.PASTED_TEXT_EMPTY))
        val tooLong = ProductFailureProjector.project(ProductFailure(ProductFailureKind.PASTED_TEXT_LIMIT_EXCEEDED))
        val invalid = ProductFailureProjector.project(ProductFailure(ProductFailureKind.PASTED_TEXT_INVALID))

        assertEquals("Testo vuoto", empty.title)
        assertEquals("Testo troppo lungo", tooLong.title)
        assertEquals("Testo non valido", invalid.title)
        assertEquals(
            setOf("RG-TXT-001", "RG-TXT-002", "RG-TXT-003"),
            listOf(empty, tooLong, invalid).map { it.technicalDetails.code }.toSet(),
        )
    }

    @Test
    fun `safe parser step and type are projected only as technical detail`() {
        val projected =
            ProductFailureProjector.project(
                ProductFailure(
                    kind = ProductFailureKind.PARSER_FAILED,
                    operationId = "operation-2",
                    diagnostic = ProductFailureDiagnostic(step = "LOAD_DOCUMENT", type = "IOException"),
                ),
            )

        assertEquals("Impossibile elaborare il PDF", projected.title)
        assertEquals("LOAD_DOCUMENT", projected.technicalDetails.lowLevelStep)
        assertEquals("IOException", projected.technicalDetails.lowLevelType)
    }

    @Test
    fun `destination failure retries export while source mismatch retries analysis`() {
        val destination = ProductFailureProjector.project(ProductFailure(ProductFailureKind.DESTINATION_UNWRITABLE))
        val sourceMismatch = ProductFailureProjector.project(ProductFailure(ProductFailureKind.SOURCE_MISMATCH))

        assertEquals(ProductRetryTarget.EXPORT, destination.retryTarget)
        assertEquals(ProductRetryTarget.ANALYSIS, sourceMismatch.retryTarget)
    }

    @Test
    fun `cancellation is not presented as retryable analysis failure`() {
        val projected = ProductFailureProjector.project(ProductFailure(ProductFailureKind.CANCELLED))

        assertEquals("Analisi annullata", projected.title)
        assertEquals(ProductRetryTarget.NONE, projected.retryTarget)
    }
}
