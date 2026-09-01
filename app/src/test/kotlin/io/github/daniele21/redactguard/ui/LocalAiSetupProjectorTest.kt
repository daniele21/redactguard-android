package io.github.daniele21.redactguard.ui

import io.github.daniele21.localllm.contracts.ConsumerGenerationConfiguration
import io.github.daniele21.localllm.contracts.ConsumerResolvedSetup
import io.github.daniele21.localllm.contracts.InferencePresetId
import io.github.daniele21.localllm.contracts.InferencePresetRef
import io.github.daniele21.localllm.contracts.SeedPolicyType
import io.github.daniele21.localllm.contracts.ThinkingMode
import io.github.daniele21.localllm.contracts.UseCaseId
import io.github.daniele21.redactguard.infrastructure.localai.LocalAiSetupStage
import io.github.daniele21.redactguard.infrastructure.localai.LocalAiSetupState
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalAiSetupProjectorTest {
    @Test
    fun `disconnected setup projects fail safe copy without invented identity`() {
        val model = LocalAiSetupProjector.project(LocalAiSetupState(), LocalAiPresetUiState())

        assertEquals("AI locale non connessa", model.statusLabel)
        assertEquals(StatusTone.REVIEW, model.tone)
        assertEquals("Non selezionato", model.presetLabel)
        assertEquals("Non ancora risolto", model.modelLabel)
        assertEquals("—", model.contextLabel)
        assertEquals("—", model.generationLabel)
    }

    @Test
    fun `compatible setup exposes only consumer safe resolved metadata`() {
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

        val model = LocalAiSetupProjector.project(setup, presets)

        assertEquals("Pronta per analizzare", model.statusLabel)
        assertEquals(StatusTone.READY, model.tone)
        assertEquals("Bilanciata", model.presetLabel)
        assertEquals("qwen35-0.8b-q4", model.modelLabel)
        assertEquals("4096 token", model.contextLabel)
        assertEquals("Max 512 token · temperatura 0.2 · top-p 0.9", model.generationLabel)
    }
}
