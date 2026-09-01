package io.github.daniele21.redactguard.ui

import io.github.daniele21.redactguard.infrastructure.localai.LocalAiSetupStage
import io.github.daniele21.redactguard.infrastructure.localai.LocalAiSetupState
import java.util.Locale

internal data class LocalAiSetupUiModel(
    val statusLabel: String,
    val statusDescription: String,
    val tone: StatusTone,
    val presetLabel: String,
    val modelLabel: String,
    val contextLabel: String,
    val generationLabel: String,
)

internal object LocalAiSetupProjector {
    fun project(
        setup: LocalAiSetupState,
        presets: LocalAiPresetUiState,
    ): LocalAiSetupUiModel {
        val resolved = setup.resolvedSetup
        val status = stageCopy(setup.stage)
        return LocalAiSetupUiModel(
            statusLabel = status.first,
            statusDescription = status.second,
            tone = status.third,
            presetLabel = presets.selectedChoice?.label ?: "Non selezionato",
            modelLabel = resolved?.modelProfileId ?: "Non ancora risolto",
            contextLabel = resolved?.let { "${it.contextTokens} token" } ?: "—",
            generationLabel =
                resolved?.generation?.let { generation ->
                    "Max ${generation.maxOutputTokens} token · temperatura ${compact(generation.temperature)} · top-p ${compact(generation.topP)}"
                } ?: "—",
        )
    }

    private fun stageCopy(stage: LocalAiSetupStage): Triple<String, String, StatusTone> =
        when (stage) {
            LocalAiSetupStage.DISCONNECTED -> {
                Triple(
                    "AI locale non connessa",
                    "RedactGuard non vede ancora Local AI Harness. La navigazione non avvia né carica alcun modello.",
                    StatusTone.REVIEW,
                )
            }

            LocalAiSetupStage.CONNECTED -> {
                Triple(
                    "AI locale connessa",
                    "Il servizio è raggiungibile; sto verificando la configurazione consumer-safe assegnata a RedactGuard.",
                    StatusTone.NEUTRAL,
                )
            }

            LocalAiSetupStage.CONFIGURED -> {
                Triple(
                    "Configurazione disponibile",
                    "La modalità è selezionata. La compatibilità deve ancora essere confermata dal servizio locale.",
                    StatusTone.NEUTRAL,
                )
            }

            LocalAiSetupStage.COMPATIBLE -> {
                Triple(
                    "Pronta per analizzare",
                    "Configurazione compatibile. Il runtime verrà preparato solo quando avvii un’analisi.",
                    StatusTone.READY,
                )
            }

            LocalAiSetupStage.RUNTIME_READY -> {
                Triple(
                    "Runtime locale attivo",
                    "Le risorse necessarie all’analisi corrente sono pronte o in uso sul dispositivo.",
                    StatusTone.READY,
                )
            }
        }

    private fun compact(value: Float): String =
        String.format(Locale.ROOT, "%.2f", value).trimEnd('0').trimEnd('.')
}
