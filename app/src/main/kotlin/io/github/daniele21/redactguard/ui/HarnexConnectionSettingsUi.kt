package io.github.daniele21.redactguard.ui

import io.github.daniele21.redactguard.domain.analysis.LocalAiRuntimeState

internal enum class HarnexConnectionPrimaryAction {
    CONNECT,
    RETRY,
    OPEN_HARNEX,
    NONE,
}

internal data class HarnexConnectionSettingsUiModel(
    val statusLabel: String,
    val statusDescription: String,
    val tone: StatusTone,
    val primaryAction: HarnexConnectionPrimaryAction,
    val primaryActionLabel: String?,
    val connectionEnabled: Boolean,
    val disconnectEnabled: Boolean,
    val disconnectBlockedReason: String? = null,
)

internal object HarnexConnectionSettingsProjector {
    fun project(
        connectionEnabled: Boolean,
        state: LocalAiRuntimeState,
        analysisActive: Boolean,
    ): HarnexConnectionSettingsUiModel {
        if (!connectionEnabled) {
            return HarnexConnectionSettingsUiModel(
                statusLabel = "Disconnesso",
                statusDescription = "RedactGuard non proverà a collegarsi a Harnex finché non riattivi la connessione.",
                tone = StatusTone.NEUTRAL,
                primaryAction = HarnexConnectionPrimaryAction.CONNECT,
                primaryActionLabel = "Connetti a Harnex",
                connectionEnabled = false,
                disconnectEnabled = false,
            )
        }

        val disconnectEnabled = !analysisActive
        val blockedReason =
            if (analysisActive) {
                "Termina o annulla l'analisi in corso prima di disconnettere Harnex."
            } else {
                null
            }
        return when (state) {
            LocalAiRuntimeState.CONNECTED -> {
                model(
                    "Connesso a Harnex",
                    "La connessione è disponibile. Modello e configurazione restano verificabili nella sezione AI locale.",
                    StatusTone.READY,
                    HarnexConnectionPrimaryAction.NONE,
                    null,
                    disconnectEnabled,
                    blockedReason,
                )
            }

            LocalAiRuntimeState.CONNECTING -> {
                model(
                    "Connessione in corso",
                    "RedactGuard sta verificando Harnex e la propria autorizzazione.",
                    StatusTone.REVIEW,
                    HarnexConnectionPrimaryAction.NONE,
                    null,
                    disconnectEnabled,
                    blockedReason,
                )
            }

            LocalAiRuntimeState.PERMISSION_DENIED -> {
                model(
                    "Autorizzazione Harnex richiesta",
                    "Harnex è raggiungibile, ma RedactGuard non è autorizzato. Apri Harnex, autorizza RedactGuard nelle connessioni app e poi riprova.",
                    StatusTone.ERROR,
                    HarnexConnectionPrimaryAction.OPEN_HARNEX,
                    "Apri Harnex",
                    disconnectEnabled,
                    blockedReason,
                )
            }

            LocalAiRuntimeState.HOST_NOT_INSTALLED -> {
                model(
                    "Harnex non installato",
                    "RedactGuard non trova il servizio Harnex configurato su questo dispositivo.",
                    StatusTone.ERROR,
                    HarnexConnectionPrimaryAction.RETRY,
                    "Riprova",
                    disconnectEnabled,
                    blockedReason,
                )
            }

            LocalAiRuntimeState.INCOMPATIBLE -> {
                model(
                    "Harnex non compatibile",
                    "La versione installata di Harnex non espone il contratto richiesto da questa versione di RedactGuard.",
                    StatusTone.ERROR,
                    HarnexConnectionPrimaryAction.OPEN_HARNEX,
                    "Apri Harnex",
                    disconnectEnabled,
                    blockedReason,
                )
            }

            LocalAiRuntimeState.DISCONNECTED -> {
                model(
                    "Connessione interrotta",
                    "La connessione è abilitata ma non è attiva. Puoi riprovare senza modificare la configurazione AI.",
                    StatusTone.REVIEW,
                    HarnexConnectionPrimaryAction.RETRY,
                    "Riprova connessione",
                    disconnectEnabled,
                    blockedReason,
                )
            }
        }
    }

    private fun model(
        label: String,
        description: String,
        tone: StatusTone,
        action: HarnexConnectionPrimaryAction,
        actionLabel: String?,
        disconnectEnabled: Boolean,
        blockedReason: String?,
    ): HarnexConnectionSettingsUiModel {
        return HarnexConnectionSettingsUiModel(
            statusLabel = label,
            statusDescription = description,
            tone = tone,
            primaryAction = action,
            primaryActionLabel = actionLabel,
            connectionEnabled = true,
            disconnectEnabled = disconnectEnabled,
            disconnectBlockedReason = blockedReason,
        )
    }
}
