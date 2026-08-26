package io.github.daniele21.redactguard.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductUiModelsTest {
    @Test
    fun `connection projector distinguishes transport connectivity from verified analysis readiness`() {
        val connected = ConnectionBadgeProjector.project(LocalAiConnectionStatus.CONNECTED)

        assertEquals("AI locale collegata", connected.label)
        assertEquals(StatusTone.NEUTRAL, connected.tone)
        assertTrue(connected.analysisReady)
        assertTrue(connected.explanation.orEmpty().contains("verificati quando avvii l’analisi"))
        assertEquals(
            "AI locale non autorizzata",
            ConnectionBadgeProjector.project(LocalAiConnectionStatus.PERMISSION_DENIED).label,
        )
        assertFalse(ConnectionBadgeProjector.project(LocalAiConnectionStatus.DISCONNECTED).analysisReady)
    }

    @Test
    fun `normal connection copy hides infrastructure naming until recovery needs it`() {
        val connected = ConnectionBadgeProjector.project(LocalAiConnectionStatus.CONNECTED)
        val connecting = ConnectionBadgeProjector.project(LocalAiConnectionStatus.CONNECTING)
        val unavailable = ConnectionBadgeProjector.project(LocalAiConnectionStatus.UNAVAILABLE)
        val permissionDenied = ConnectionBadgeProjector.project(LocalAiConnectionStatus.PERMISSION_DENIED)

        assertFalse(connected.label.contains("Harness"))
        assertFalse(connecting.label.contains("Harness"))
        assertFalse(unavailable.label.contains("Harness"))
        assertTrue(permissionDenied.explanation.orEmpty().contains("Local AI Harness"))
    }

    @Test
    fun `review diagnostic string never exposes revealed source or masked context text`() {
        val context =
            ReviewContextModel(
                maskedText = "Contatta [EMAIL_1] per assistenza riservata",
                focusPlaceholder = "[EMAIL_1]",
                pageNumber = 1,
            )
        val finding =
            ReviewFindingModel(
                id = "finding-1",
                categoryLabel = "Email",
                placeholder = "[EMAIL_1]",
                context = context,
                revealedValue = "mario.rossi@example.test",
            )
        val diagnostics = finding.toString()

        assertFalse(diagnostics.contains("mario.rossi@example.test"))
        assertFalse(diagnostics.contains("assistenza riservata"))
        assertTrue(diagnostics.contains("[EMAIL_1]"))
        assertFalse(context.toString().contains("assistenza riservata"))
    }
}
