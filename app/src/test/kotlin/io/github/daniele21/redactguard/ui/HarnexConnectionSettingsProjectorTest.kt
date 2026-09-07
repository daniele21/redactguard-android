package io.github.daniele21.redactguard.ui

import io.github.daniele21.redactguard.domain.analysis.LocalAiRuntimeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HarnexConnectionSettingsProjectorTest {
    @Test
    fun `explicitly disabled connection projects connect action`() {
        val model =
            HarnexConnectionSettingsProjector.project(
                connectionEnabled = false,
                state = LocalAiRuntimeState.CONNECTED,
                analysisActive = false,
            )

        assertEquals("Disconnesso", model.statusLabel)
        assertEquals(HarnexConnectionPrimaryAction.CONNECT, model.primaryAction)
        assertEquals("Connetti a Harnex", model.primaryActionLabel)
        assertFalse(model.connectionEnabled)
        assertFalse(model.disconnectEnabled)
    }

    @Test
    fun `permission denied directs user to Harnex authorization`() {
        val model =
            HarnexConnectionSettingsProjector.project(
                connectionEnabled = true,
                state = LocalAiRuntimeState.PERMISSION_DENIED,
                analysisActive = false,
            )

        assertEquals("Autorizzazione Harnex richiesta", model.statusLabel)
        assertEquals(HarnexConnectionPrimaryAction.OPEN_HARNEX, model.primaryAction)
        assertEquals("Apri Harnex", model.primaryActionLabel)
        assertTrue(model.disconnectEnabled)
    }

    @Test
    fun `enabled but disconnected connection offers retry`() {
        val model =
            HarnexConnectionSettingsProjector.project(
                connectionEnabled = true,
                state = LocalAiRuntimeState.DISCONNECTED,
                analysisActive = false,
            )

        assertEquals("Connessione interrotta", model.statusLabel)
        assertEquals(HarnexConnectionPrimaryAction.RETRY, model.primaryAction)
        assertEquals("Riprova connessione", model.primaryActionLabel)
        assertTrue(model.connectionEnabled)
    }

    @Test
    fun `active analysis blocks transport disconnect`() {
        val model =
            HarnexConnectionSettingsProjector.project(
                connectionEnabled = true,
                state = LocalAiRuntimeState.CONNECTED,
                analysisActive = true,
            )

        assertFalse(model.disconnectEnabled)
        assertTrue(model.disconnectBlockedReason?.contains("analisi in corso") == true)
    }
}
