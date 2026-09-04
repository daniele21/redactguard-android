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

internal enum class LocalAiSetupActionTarget {
    REFRESH,
    OPEN_LOCAL_AI,
    NONE,
}

internal data class LocalAiSetupUiModel(
    val statusLabel: String,
    val statusDescription: String,
    val tone: StatusTone,
    val actionLabel: String?,
    val actionTarget: LocalAiSetupActionTarget,
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
        val recovery = setupRecoveryAction(setup)
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
            actionLabel = actionLabel(recovery),
            actionTarget = actionTarget(recovery),
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
            ProductFailureKind.HOST_UNAVAILABLE -> {
                Triple(
                    "AI locale non disponibile",
                    "Il servizio AI locale non è raggiungibile. Riprova la connessione prima di avviare l’analisi.",
                    StatusTone.REVIEW,
                )
            }

            ProductFailureKind.DISCONNECTED,
            ProductFailureKind.HOST_PROCESS_LOST,
            ProductFailureKind.LOCAL_AI_RUNTIME_UNAVAILABLE,
            ProductFailureKind.CANCELLED,
            -> {
                Triple(
                    "AI locale temporaneamente non disponibile",
                    "Lo stato operativo precedente non è più valido. Riprova la verifica prima della prossima analisi.",
                    StatusTone.REVIEW,
                )
            }

            ProductFailureKind.LOCAL_AI_CONFIGURATION_REQUIRED -> {
                Triple(
                    "Configurazione richiesta",
                    "Completa o aggiorna la configurazione di RedactGuard nell’AI locale, quindi torna qui per verificarla.",
                    StatusTone.REVIEW,
                )
            }

            ProductFailureKind.LOCAL_AI_MODEL_UNAVAILABLE -> {
                Triple(
                    "Modello locale non disponibile",
                    "Il modello richiesto dalla configurazione non è disponibile. Apri l’AI locale per gestire il modello richiesto.",
                    StatusTone.REVIEW,
                )
            }

            ProductFailureKind.CAPABILITY_INCOMPATIBLE -> {
                Triple(
                    "Versione AI locale non compatibile",
                    "La versione o le capacità installate non supportano questa integrazione. Aggiorna l’AI locale, poi riprova la verifica.",
                    StatusTone.ERROR,
                )
            }

            ProductFailureKind.LOCAL_AI_INVALID_REQUEST -> {
                Triple(
                    "Verifica AI locale non riuscita",
                    "RedactGuard non può correggere automaticamente questa richiesta. I dettagli tecnici possono aiutare a diagnosticare il problema.",
                    StatusTone.ERROR,
                )
            }

            ProductFailureKind.LOCAL_AI_SETUP_UNEXPECTED -> {
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

    private fun setupRecoveryAction(setup: LocalAiSetupState): FailureRecoveryAction? =
        when (setup.problem) {
            ProductFailureKind.HOST_UNAVAILABLE -> FailureRecoveryAction.RECONNECT_HARNESS
            null -> null
            else -> setup.recoveryAction
        }

    private fun actionLabel(action: FailureRecoveryAction?): String? =
        when (action) {
            FailureRecoveryAction.RECONNECT_HARNESS -> "Riprova connessione"
            FailureRecoveryAction.OPEN_HARNESS -> "Apri AI locale"
            FailureRecoveryAction.UPDATE_HARNESS -> "Riprova dopo l’aggiornamento"
            FailureRecoveryAction.RETRY_SETUP -> "Riprova verifica"
            FailureRecoveryAction.NONE -> null
            else -> "Aggiorna stato"
        }

    private fun actionTarget(action: FailureRecoveryAction?): LocalAiSetupActionTarget =
        when (action) {
            FailureRecoveryAction.OPEN_HARNESS -> LocalAiSetupActionTarget.OPEN_LOCAL_AI
            FailureRecoveryAction.NONE -> LocalAiSetupActionTarget.NONE
            else -> LocalAiSetupActionTarget.REFRESH
        }

    private fun compact(value: Float): String = String.format(Locale.ROOT, "%.2f", value).trimEnd('0').trimEnd('.')
}
