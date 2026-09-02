package io.github.daniele21.redactguard.ui

import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeFailureCode
import io.github.daniele21.redactguard.infrastructure.localai.LocalAiSetupStage
import io.github.daniele21.redactguard.infrastructure.localai.LocalAiSetupState
import java.util.Locale

internal data class LocalAiSetupDetail(
    val label: String,
    val value: String,
)

internal data class LocalAiSetupUiModel(
    val statusLabel: String,
    val statusDescription: String,
    val tone: StatusTone,
    val refreshLabel: String,
    val replacementNotice: String?,
    val contextualDetails: List<LocalAiSetupDetail>,
    val advancedDetails: List<LocalAiSetupDetail>,
    val technicalDetails: List<LocalAiSetupDetail>,
)

internal object LocalAiSetupProjector {
    fun project(
        connection: ConnectionBadgeModel,
        setup: LocalAiSetupState,
        presets: LocalAiPresetUiState,
    ): LocalAiSetupUiModel {
        val resolved = setup.resolvedSetup
        val status = statusCopy(connection, setup)
        val presetLabel =
            presets.selectedChoice?.label
                ?: setup.selectedPreset?.id?.value
                ?: "Non selezionato"
        val advanced =
            listOf(
                LocalAiSetupDetail("Modello", resolved?.modelProfileId ?: "Non disponibile"),
                LocalAiSetupDetail("Contesto", resolved?.let { "${it.contextTokens} token" } ?: "Non disponibile"),
                LocalAiSetupDetail(
                    "Generazione",
                    resolved?.generation?.let { generation ->
                        "Max ${generation.maxOutputTokens} token · temperatura ${compact(
                            generation.temperature,
                        )} · top-p ${compact(generation.topP)}"
                    } ?: "Non disponibile",
                ),
            )
        val technical = buildList {
            setup.failureCode?.let { add(LocalAiSetupDetail("Codice stato", it.name)) }
            resolved?.let { current ->
                add(LocalAiSetupDetail("Use case ID", current.useCaseId.value))
                add(LocalAiSetupDetail("Revisione use case", current.useCaseRevision.toString()))
                add(LocalAiSetupDetail("Revisione binding", current.bindingRevision.toString()))
                add(LocalAiSetupDetail("Versione preset", current.preset.version.toString()))
            }
        }
        return LocalAiSetupUiModel(
            statusLabel = status.first,
            statusDescription = status.second,
            tone = status.third,
            refreshLabel = if (setup.compatible && setup.failureCode == null) "Aggiorna stato" else "Riprova verifica",
            replacementNotice = presets.replacementNotice,
            contextualDetails =
                listOf(
                    LocalAiSetupDetail("Uso", "Protezione documenti"),
                    LocalAiSetupDetail("Modalità", presetLabel),
                ),
            advancedDetails = advanced,
            technicalDetails = technical,
        )
    }

    private fun statusCopy(
        connection: ConnectionBadgeModel,
        setup: LocalAiSetupState,
    ): Triple<String, String, StatusTone> {
        if (setup.stage == LocalAiSetupStage.DISCONNECTED) {
            return Triple(
                connection.label,
                connection.explanation
                    ?: "Il servizio AI locale non è raggiungibile. Puoi riprovare senza modificare il documento.",
                connection.tone,
            )
        }
        if (!connection.analysisReady && connection.tone == StatusTone.ERROR) {
            return Triple(
                connection.label,
                connection.explanation ?: "La configurazione dell’AI locale richiede un intervento prima dell’analisi.",
                connection.tone,
            )
        }
        setup.failureCode?.let { return failureCopy(it) }
        return stageCopy(setup.stage)
    }

    private fun failureCopy(code: AnalysisRuntimeFailureCode): Triple<String, String, StatusTone> =
        when (code) {
            AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE -> {
                Triple(
                    "Configurazione non compatibile",
                    "La configurazione disponibile non è valida per questa analisi. Aggiorna lo stato dopo aver corretto la configurazione dell’AI locale.",
                    StatusTone.ERROR,
                )
            }

            AnalysisRuntimeFailureCode.HOST_UNAVAILABLE,
            AnalysisRuntimeFailureCode.DISCONNECTED,
            AnalysisRuntimeFailureCode.HOST_PROCESS_LOST,
            -> {
                Triple(
                    "AI locale da riconnettere",
                    "Il servizio AI locale non è al momento disponibile. Riprova la verifica prima di avviare l’analisi.",
                    StatusTone.REVIEW,
                )
            }

            AnalysisRuntimeFailureCode.GENERATION_FAILED,
            AnalysisRuntimeFailureCode.CANCELLED,
            -> {
                Triple(
                    "Verifica AI locale necessaria",
                    "Lo stato precedente non è più sufficiente. Aggiorna la configurazione prima della prossima analisi.",
                    StatusTone.REVIEW,
                )
            }

            AnalysisRuntimeFailureCode.INTERNAL_FAILURE -> {
                Triple(
                    "Verifica AI locale non riuscita",
                    "Non è stato possibile confermare la configurazione. Riprova senza modificare il documento.",
                    StatusTone.ERROR,
                )
            }
        }

    private fun stageCopy(stage: LocalAiSetupStage): Triple<String, String, StatusTone> =
        when (stage) {
            LocalAiSetupStage.DISCONNECTED -> error("Disconnected setup is projected from connection state")

            LocalAiSetupStage.CONNECTED -> {
                Triple(
                    "AI locale connessa",
                    "Il servizio è raggiungibile. La configurazione per RedactGuard deve ancora essere verificata.",
                    StatusTone.NEUTRAL,
                )
            }

            LocalAiSetupStage.CONFIGURED -> {
                Triple(
                    "Configurazione da verificare",
                    "La modalità è selezionata, ma la compatibilità non è ancora confermata.",
                    StatusTone.NEUTRAL,
                )
            }

            LocalAiSetupStage.COMPATIBLE -> {
                Triple(
                    "Configurazione compatibile",
                    "La configurazione è compatibile. RedactGuard la verificherà di nuovo subito prima che il documento entri nell’analisi.",
                    StatusTone.NEUTRAL,
                )
            }

            LocalAiSetupStage.RUNTIME_READY -> {
                Triple(
                    "AI locale attiva",
                    "Le risorse necessarie all’analisi corrente sono pronte o in uso sul dispositivo.",
                    StatusTone.READY,
                )
            }
        }

    private fun compact(value: Float): String = String.format(Locale.ROOT, "%.2f", value).trimEnd('0').trimEnd('.')
}
