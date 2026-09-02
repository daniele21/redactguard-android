package io.github.daniele21.redactguard.ui

import io.github.daniele21.localllm.contracts.ConsumerGenerationConfiguration
import io.github.daniele21.localllm.contracts.ConsumerResolvedSetup
import io.github.daniele21.localllm.contracts.InferencePresetId
import io.github.daniele21.localllm.contracts.InferencePresetRef
import io.github.daniele21.localllm.contracts.SeedPolicyType
import io.github.daniele21.localllm.contracts.ThinkingMode
import io.github.daniele21.localllm.contracts.UseCaseId
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeDiagnostic
import io.github.daniele21.redactguard.domain.failure.ProductFailureKind
import io.github.daniele21.redactguard.infrastructure.localai.LocalAiSetupStage
import io.github.daniele21.redactguard.infrastructure.localai.LocalAiSetupState
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalAiSetupProjectorTest {
    @Test
    fun `disconnected setup uses cause specific connection copy without invented identity`() {
        val model = project(LocalAiSetupState(), status = LocalAiConnectionStatus.DISCONNECTED)

        assertEquals("AI locale disconnessa", model.statusLabel)
        assertEquals(StatusTone.REVIEW, model.tone)
        assertEquals("Aggiorna stato", model.refreshLabel)
        assertEquals("Non selezionato", model.contextualDetails.valueFor("Modalità"))
        assertEquals("Non disponibile", model.advancedDetails.valueFor("Modello"))
        assertEquals(emptyList<LocalAiSetupDetail>(), model.technicalDetails)
    }

    @Test
    fun `configured setup keeps active preset visible when selector has no display label`() {
        val preset = InferencePresetRef(InferencePresetId("balanced"), 3)
        val setup =
            LocalAiSetupState(
                stage = LocalAiSetupStage.CONFIGURED,
                selectedPreset = preset,
            )

        val model = project(setup)

        assertEquals("balanced", model.contextualDetails.valueFor("Modalità"))
        assertEquals("Configurazione da verificare", model.statusLabel)
    }

    @Test
    fun `compatible setup exposes progressive metadata without claiming fresh preflight readiness`() {
        val preset = InferencePresetRef(InferencePresetId("balanced"), 3)
        val setup =
            LocalAiSetupState(
                stage = LocalAiSetupStage.COMPATIBLE,
                selectedPreset = preset,
                resolvedSetup =
                    ConsumerResolvedSetup(
                        useCaseId = UseCaseId("document-pii-detection"),
                        useCaseRevision = 7,
                        bindingRevision = 11,
                        preset = preset,
                        modelProfileId = "qwen35-0.8b-q4",
                        contextTokens = 4096,
                        generation =
                            ConsumerGenerationConfiguration(
                                maxOutputTokens = 512,
                                temperature = 0.2f,
                                topP = 0.9f,
                                topK = 40,
                                minP = 0f,
                                presencePenalty = 0f,
                                repeatPenalty = 1.05f,
                                repeatLastN = 64,
                                thinkingMode = ThinkingMode.DISABLED,
                                seedPolicy = SeedPolicyType.FIXED,
                            ),
                    ),
            )
        val presets =
            LocalAiPresetUiState(
                choices =
                    listOf(
                        LocalAiPresetChoice(
                            id = "balanced:3",
                            label = "Bilanciata",
                            selected = true,
                        ),
                    ),
            )

        val model = project(setup, presets)

        assertEquals("Configurazione compatibile", model.statusLabel)
        assertEquals(StatusTone.NEUTRAL, model.tone)
        assertEquals("Aggiorna stato", model.refreshLabel)
        assertEquals("Bilanciata", model.contextualDetails.valueFor("Modalità"))
        assertEquals("qwen35-0.8b-q4", model.advancedDetails.valueFor("Modello"))
        assertEquals("4096 token", model.advancedDetails.valueFor("Contesto"))
        assertEquals(
            "Max 512 token · temperatura 0.2 · top-p 0.9",
            model.advancedDetails.valueFor("Generazione"),
        )
        assertEquals("document-pii-detection", model.technicalDetails.valueFor("Use case ID"))
        assertEquals("7", model.technicalDetails.valueFor("Revisione use case"))
        assertEquals("11", model.technicalDetails.valueFor("Revisione binding"))
        assertEquals("3", model.technicalDetails.valueFor("Versione preset"))
    }

    @Test
    fun `runtime ready is shown without replacing compatible setup stage`() {
        val setup =
            LocalAiSetupState(
                stage = LocalAiSetupStage.COMPATIBLE,
                runtimeReady = true,
            )

        val model = project(setup)

        assertEquals("AI locale attiva", model.statusLabel)
        assertEquals(StatusTone.READY, model.tone)
    }

    @Test
    fun `configuration required is distinct from incompatibility and exposes typed recovery`() {
        val preset = InferencePresetRef(InferencePresetId("balanced"), 3)
        val setup =
            LocalAiSetupState(
                stage = LocalAiSetupStage.CONFIGURED,
                selectedPreset = preset,
                problem = ProductFailureKind.LOCAL_AI_CONFIGURATION_REQUIRED,
                technicalIdentity =
                    AnalysisRuntimeDiagnostic(
                        step = "control-plane.setup-resolution",
                        type = "ControlPlane:CONFIGURATION_REQUIRED",
                    ),
            )

        val model = project(setup)

        assertEquals("Configurazione richiesta", model.statusLabel)
        assertEquals(StatusTone.REVIEW, model.tone)
        assertEquals("Apri AI locale", model.refreshLabel)
        assertEquals("ControlPlane:CONFIGURATION_REQUIRED", model.technicalDetails.valueFor("Codice stato"))
        assertEquals("control-plane.setup-resolution", model.technicalDetails.valueFor("Passaggio"))
    }

    @Test
    fun `true incompatibility remains explicit and uses update recovery`() {
        val setup =
            LocalAiSetupState(
                stage = LocalAiSetupStage.CONFIGURED,
                problem = ProductFailureKind.CAPABILITY_INCOMPATIBLE,
                technicalIdentity =
                    AnalysisRuntimeDiagnostic(
                        step = "control-plane.setup-resolution",
                        type = "ControlPlane:FEATURE_UNAVAILABLE",
                    ),
            )

        val model = project(setup)

        assertEquals("AI locale non compatibile", model.statusLabel)
        assertEquals(StatusTone.ERROR, model.tone)
        assertEquals("Aggiorna AI locale", model.refreshLabel)
        assertEquals("ControlPlane:FEATURE_UNAVAILABLE", model.technicalDetails.valueFor("Codice stato"))
    }

    private fun project(
        setup: LocalAiSetupState,
        presets: LocalAiPresetUiState = LocalAiPresetUiState(),
        status: LocalAiConnectionStatus = LocalAiConnectionStatus.CONNECTED,
    ): LocalAiSetupUiModel =
        LocalAiSetupProjector.project(
            connection = ConnectionBadgeProjector.project(status),
            setup = setup,
            presets = presets,
        )

    private fun List<LocalAiSetupDetail>.valueFor(label: String): String = single { it.label == label }.value
}
