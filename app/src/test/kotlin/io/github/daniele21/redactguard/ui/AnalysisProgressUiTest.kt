package io.github.daniele21.redactguard.ui

import io.github.daniele21.redactguard.domain.analysis.LocalAiExecutionPhase
import io.github.daniele21.redactguard.domain.analysis.LocalAiExecutionState
import io.github.daniele21.redactguard.domain.analysis.LocalAiPreparationAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisProgressUiTest {
    @Test
    fun `runtime ready wording appears only for source-backed ready phase`() {
        val starting = AnalysisProgressProjector.starting()
        val ready = AnalysisProgressProjector.project(LocalAiExecutionState(LocalAiExecutionPhase.READY))

        assertFalse(starting.title.contains("pronta"))
        assertEquals("AI locale pronta", ready.title)
        assertEquals(AnalysisVisualStage.PREPARING, ready.visualStage)
    }

    @Test
    fun `preparation actions remain product language and expose no model identity`() {
        val loading =
            AnalysisProgressProjector.project(
                LocalAiExecutionState(
                    phase = LocalAiExecutionPhase.PREPARING,
                    preparationAction = LocalAiPreparationAction.LOADING,
                ),
            )
        val switching =
            AnalysisProgressProjector.project(
                LocalAiExecutionState(
                    phase = LocalAiExecutionPhase.PREPARING,
                    preparationAction = LocalAiPreparationAction.SWITCHING,
                ),
            )

        assertTrue(loading.message.contains("risorse locali"))
        assertFalse(loading.message.contains("modello", ignoreCase = true))
        assertFalse(switching.message.contains("modello", ignoreCase = true))
        assertEquals(AnalysisVisualStage.PREPARING, loading.visualStage)
    }

    @Test
    fun `generating keeps truthful indeterminate analysis copy and active search stage`() {
        val generating =
            AnalysisProgressProjector.project(
                LocalAiExecutionState(LocalAiExecutionPhase.GENERATING),
            )

        assertEquals("Ricerca dei dati sensibili", generating.title)
        assertTrue(generating.message.contains("categorie selezionate"))
        assertEquals(AnalysisVisualStage.SEARCHING, generating.visualStage)
    }
}
