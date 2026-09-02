package io.github.daniele21.redactguard.ui

import io.github.daniele21.redactguard.domain.failure.FailureRecoveryAction
import io.github.daniele21.redactguard.domain.failure.ProductFailureKind
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
        val technical =
            buildList {
                setup.technicalIdentity?.let { identity ->
                    add(LocalAiSetupDetail("Codice stato", identity.type))
                    add(LocalAiSetupDetail("Passaggio", identity.step))
                }
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
            refreshLabel = recoveryLabel(setup.recoveryAction),
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
        if (setup.stage == LocalAiSetupStage.DISCONNECTED && setup.problem == null) {
            return Triple(
                connection.label,
                connection.explanation
                    ?: "Il servizio AI locale non è raggiungibile. Puoi riprovare senza modificare il documento.",
                connection.tone,
            )
        }
        setup.problem?.let { return problemCopy(it) }
        if (!connection.analysisReady && connection.tone == StatusTone.ERROR) {
            return Triple(
                connection.label,
                connection.explanation ?: "La configurazione dell’AI locale richiede un intervento prima dell’analisi.",
                connection.tone,
            )
        }
        if (setup.runtimeReady) {
            return Triple(
                "AI locale attiva",
                "Le risorse necessarie all’analisi corrente sono pronte o in uso sul dispositivo.",
                StatusTone.READY,
            )
        }
        return stageCopy(setup.stage)
    }

    private fun problemCopy(problem: ProductFailureKind): Triple<String, String, StatusTone> =
        when (problem) {
            ProductFailureKind.HOST_UNAVAILABLE,
            ProductFailureKind.DISCONNECTED,
            ProductFailureKind.HOST_PROCESS_LOST,
            -> {
                Triple(
                    "AI locale da riconnettere",
                    "Il servizio AI locale non è al momento disponibile. Riprova la verifica prima di avviare l’analisi.",
                    StatusTone.REVIEW,
                )
            }

            ProductFailureKind.LOCAL_AI_CONFIGURATION_REQUIRED -> {
                Triple(
                    "Configurazione richiesta",
                    "La configurazione per RedactGuard deve essere completata o aggiornata nell’AI locale.",
                    StatusTone.REVIEW,
                )
            }

            ProductFailureKind.LOCAL_AI_MODEL_UNAVAILABLE -> {
                Triple(
                    "Modello non disponibile",
                    "Il modello richiesto dalla configurazione non è al momento disponibile nell’AI locale.",
                    StatusTone.REVIEW,
                )
            }

            ProductFailureKind.CAPABILITY_INCOMPATIBLE -> {
                Triple(
                    "AI locale non compatibile",
                    "La versione o le capacità disponibili non supportano questa integrazione. Aggiorna l’AI locale e riprova.",
                    StatusTone.ERROR,
                )
            }

            ProductFailureKind.LOCAL_AI_RUNTIME_UNAVAILABLE,
            ProductFailureKind.CANCELLED,
            -> {
                Triple(
                    "Verifica AI locale necessaria",
                    "Lo stato operativo precedente non è più sufficiente. Riprova la verifica prima della prossima analisi.",
                    StatusTone.REVIEW,
                )
            }

            ProductFailureKind.LOCAL_AI_INVALID_REQUEST,
            ProductFailureKind.LOCAL_AI_SETUP_UNEXPECTED,
            -> {
                Triple(
                    "Verifica AI locale non riuscita",
                    "Non è stato possibile confermare la configurazione. Riprova senza modificare il documento.",
                    StatusTone.ERROR,
                )
            }

            else -> {
                Triple(
                    "Verifica AI locale non riuscita",
                    "Non è stato possibile confermare la configurazione. Riprova senza modificare il documento.",
                    StatusTone.ERROR,
                )
            }
        }

    private fun stageCopy(stage: LocalAiSetupStage): Triple<String, String, StatusTone> =
        when (stage) {
            LocalAiSetupStage.DISCONNECTED -> {
                Triple(
                    "AI locale non connessa",
                    "Connetti il servizio AI locale per verificare la configurazione.",
                    StatusTone.NEUTRAL,
                )
            }

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
        }

    private fun recoveryLabel(action: FailureRecoveryAction?): String =
        when (action) {
            FailureRecoveryAction.RECONNECT_HARNESS -> "Riconnetti"
            FailureRecoveryAction.OPEN_HARNESS -> "Apri AI locale"
            FailureRecoveryAction.UPDATE_HARNESS -> "Aggiorna AI locale"
            FailureRecoveryAction.RETRY_SETUP -> "Riprova verifica"
            FailureRecoveryAction.NONE -> "Dettagli tecnici"
            else -> "Aggiorna stato"
        }

    private fun compact(value: Float): String = String.format(Locale.ROOT, "%.2f", value).trimEnd('0').trimEnd('.')
}
