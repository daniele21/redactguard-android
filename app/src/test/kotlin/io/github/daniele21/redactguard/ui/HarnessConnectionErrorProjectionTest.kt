package io.github.daniele21.redactguard.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HarnessConnectionErrorProjectionTest {
    @Test
    fun `permission denial is explicit actionable and never analysis ready`() {
        val model = ConnectionBadgeProjector.project(LocalAiConnectionStatus.PERMISSION_DENIED)

        assertFalse(model.analysisReady)
        assertTrue(model.label.contains("non autorizzata", ignoreCase = true))
        assertTrue(requireNotNull(model.explanation).contains("servizio AI locale", ignoreCase = true))
        assertFalse(model.explanation.contains("Harness"))
        assertTrue(model.explanation.contains("autorizzazione", ignoreCase = true))
        assertTrue(model.explanation.contains("riprova", ignoreCase = true))
    }

    @Test
    fun `incompatible host explains configuration recovery path`() {
        val model = ConnectionBadgeProjector.project(LocalAiConnectionStatus.INCOMPATIBLE)

        assertFalse(model.analysisReady)
        assertTrue(model.label.contains("Configurazione", ignoreCase = true))
        assertTrue(requireNotNull(model.explanation).contains("non espone una configurazione compatibile"))
        assertTrue(model.explanation.contains("configurazione assegnata", ignoreCase = true))
        assertTrue(model.explanation.contains("riprova", ignoreCase = true))
        assertFalse(model.explanation.contains("Harness"))
    }

    @Test
    fun `missing and disconnected host remain recoverable states`() {
        val missing = ConnectionBadgeProjector.project(LocalAiConnectionStatus.HOST_NOT_INSTALLED)
        val disconnected = ConnectionBadgeProjector.project(LocalAiConnectionStatus.DISCONNECTED)

        assertFalse(missing.analysisReady)
        assertFalse(disconnected.analysisReady)
        assertTrue(requireNotNull(missing.explanation).contains("Installa il servizio AI locale"))
        assertTrue(requireNotNull(disconnected.explanation).contains("riprovare"))
        assertFalse(missing.explanation.contains("Harness"))
        assertFalse(disconnected.explanation.contains("Harness"))
    }
}
