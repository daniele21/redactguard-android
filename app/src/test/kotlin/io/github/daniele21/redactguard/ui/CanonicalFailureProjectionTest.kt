package io.github.daniele21.redactguard.ui

import io.github.daniele21.redactguard.domain.failure.ProductFailure
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
            assertEquals(kind.stableCode, projected.technicalDetails?.code)
            assertEquals(kind.name, projected.technicalDetails?.cause)
            assertEquals(kind.stage.name, projected.technicalDetails?.stage)
            assertEquals("operation-1", projected.technicalDetails?.operationId)
        }
    }

    @Test
    fun `image only PDF never renders the generic omnibus unsupported message`() {
        val projected = ProductFailureProjector.project(ProductFailure(ProductFailureKind.IMAGE_ONLY_PDF))

        assertEquals("PDF senza testo estraibile", projected.title)
        assertTrue(projected.message.contains("OCR"))
        assertFalse(projected.message.contains("cifrato, non valido, troppo grande"))
        assertEquals("RG-PDF-008", projected.technicalDetails?.code)
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
