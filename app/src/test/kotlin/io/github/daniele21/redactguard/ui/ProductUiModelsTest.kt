package io.github.daniele21.redactguard.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductUiModelsTest {
    @Test
    fun `connection projector reserves runtime ready wording for active analysis`() {
        val connected = ConnectionBadgeProjector.project(LocalAiConnectionStatus.CONNECTED)
        val connecting = ConnectionBadgeProjector.project(LocalAiConnectionStatus.CONNECTING)

        assertEquals("AI locale configurata", connected.label)
        assertEquals(StatusTone.READY, connected.tone)
        assertTrue(connected.analysisReady)
        assertTrue(connected.explanation.orEmpty().contains("configurazione assegnata"))
        assertFalse(connected.explanation.orEmpty().contains("modello"))
        assertFalse(connecting.analysisReady)
        assertTrue(connecting.explanation.orEmpty().contains("verificando la configurazione"))
        assertEquals(
            "AI locale non autorizzata",
            ConnectionBadgeProjector.project(LocalAiConnectionStatus.PERMISSION_DENIED).label,
        )
        assertFalse(ConnectionBadgeProjector.project(LocalAiConnectionStatus.DISCONNECTED).analysisReady)
    }

    @Test
    fun `normal and recovery readiness copy hides implementation detail`() {
        LocalAiConnectionStatus.entries.forEach { status ->
            val model = ConnectionBadgeProjector.project(status)
            assertFalse(model.label.contains("Harness"))
            assertFalse(model.explanation.orEmpty().contains("Harness"))
            assertFalse(model.explanation.orEmpty().contains("Binder"))
            assertFalse(model.explanation.orEmpty().contains("Consumer SDK"))
        }
    }

    @Test
    fun `preset state keeps a single selected host mode visible without requiring a selector`() {
        val selected =
            LocalAiPresetChoice(
                id = "preset-0",
                label = "Bilanciata",
                description = "Modalità pubblicata dall’AI locale",
                selected = true,
            )
        val state = LocalAiPresetUiState(choices = listOf(selected))

        assertEquals(selected, state.selectedChoice)
        assertFalse(state.showSelector)
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
