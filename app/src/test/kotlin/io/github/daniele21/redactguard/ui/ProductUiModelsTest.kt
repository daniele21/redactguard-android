package io.github.daniele21.redactguard.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductUiModelsTest {
    @Test
    fun `connection projector preserves user-visible typed states without Binder dependency`() {
        assertEquals("Harness connesso", ConnectionBadgeProjector.project(LocalAiConnectionStatus.CONNECTED).label)
        assertTrue(ConnectionBadgeProjector.project(LocalAiConnectionStatus.CONNECTED).analysisReady)
        assertEquals(
            "Accesso a Harness negato",
            ConnectionBadgeProjector.project(LocalAiConnectionStatus.PERMISSION_DENIED).label,
        )
        assertFalse(ConnectionBadgeProjector.project(LocalAiConnectionStatus.DISCONNECTED).analysisReady)
    }

    @Test
    fun `review diagnostic string never exposes revealed source value`() {
        val finding = ReviewFindingModel(
            id = "finding-1",
            categoryLabel = "Email",
            placeholder = "[EMAIL 1]",
            revealedValue = "mario.rossi@example.test",
        )
        assertFalse(finding.toString().contains("mario.rossi@example.test"))
        assertTrue(finding.toString().contains("[EMAIL 1]"))
    }
}
