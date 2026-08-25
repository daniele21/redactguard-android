package io.github.daniele21.redactguard.ui

/** Product-owned projection of the external local-AI dependency; no Binder type leaks into UI. */
internal enum class LocalAiConnectionStatus {
    CONNECTED,
    CONNECTING,
    PERMISSION_DENIED,
    INCOMPATIBLE,
    HOST_NOT_INSTALLED,
    DISCONNECTED,
    UNAVAILABLE,
}

internal enum class StatusTone { READY, NEUTRAL, REVIEW, ERROR }

internal data class ConnectionBadgeModel(
    val label: String,
    val tone: StatusTone,
    val analysisReady: Boolean,
    val explanation: String? = null,
)

internal object ConnectionBadgeProjector {
    fun project(status: LocalAiConnectionStatus): ConnectionBadgeModel =
        when (status) {
            LocalAiConnectionStatus.CONNECTED -> {
                ConnectionBadgeModel(
                    "AI locale collegata",
                    StatusTone.NEUTRAL,
                    true,
                    "La connessione locale è disponibile. Assegnazione, preset e compatibilità vengono verificati quando avvii l’analisi.",
                )
            }

            LocalAiConnectionStatus.CONNECTING -> {
                ConnectionBadgeModel(
                    "Connessione all’AI locale",
                    StatusTone.NEUTRAL,
                    false,
                    "Sto verificando che il servizio AI locale sia disponibile e compatibile.",
                )
            }

            LocalAiConnectionStatus.PERMISSION_DENIED -> {
                ConnectionBadgeModel(
                    "AI locale non autorizzata",
                    StatusTone.ERROR,
                    false,
                    "RedactGuard non è autorizzato a usare il servizio AI locale. Apri Local AI Harness, verifica l’autorizzazione e riprova.",
                )
            }

            LocalAiConnectionStatus.INCOMPATIBLE -> {
                ConnectionBadgeModel(
                    "AI locale da aggiornare",
                    StatusTone.ERROR,
                    false,
                    "La versione installata del servizio AI locale non è compatibile. Aggiorna Local AI Harness e riprova.",
                )
            }

            LocalAiConnectionStatus.HOST_NOT_INSTALLED -> {
                ConnectionBadgeModel(
                    "AI locale non installata",
                    StatusTone.ERROR,
                    false,
                    "Installa Local AI Harness sul dispositivo, quindi torna in RedactGuard: la connessione verrà ritentata automaticamente.",
                )
            }

            LocalAiConnectionStatus.DISCONNECTED -> {
                ConnectionBadgeModel(
                    "AI locale disconnessa",
                    StatusTone.REVIEW,
                    false,
                    "La connessione al servizio AI locale si è interrotta. Riapri Local AI Harness e torna in RedactGuard per riprovare.",
                )
            }

            LocalAiConnectionStatus.UNAVAILABLE -> {
                ConnectionBadgeModel(
                    "AI locale non disponibile",
                    StatusTone.NEUTRAL,
                    false,
                    "Il servizio AI locale non è raggiungibile in questo momento. Il documento resta sul dispositivo e puoi riprovare senza riavviare RedactGuard.",
                )
            }
        }
}

internal data class DefinitionChoice(
    val id: String,
    val label: String,
    val selected: Boolean,
) {
    override fun toString(): String = "DefinitionChoice(id=$id, label=<redacted>, selected=$selected)"
}

/** Consumer-safe Host option. The id is process-local and does not expose runtime/model identity. */
internal data class LocalAiPresetChoice(
    val id: String,
    val label: String,
    val description: String? = null,
    val selected: Boolean,
) {
    init {
        require(id.isNotBlank())
        require(label.isNotBlank())
    }

    override fun toString(): String =
        "LocalAiPresetChoice(id=$id, label=<consumer-safe>, description=<consumer-safe>, selected=$selected)"
}

internal data class LocalAiPresetUiState(
    val choices: List<LocalAiPresetChoice> = emptyList(),
    val replacementNotice: String? = null,
) {
    override fun toString(): String =
        "LocalAiPresetUiState(choiceCount=${choices.size}, selectedCount=${choices.count(LocalAiPresetChoice::selected)}, hasReplacementNotice=${replacementNotice != null})"
}

internal data class ReviewFindingModel(
    val id: String,
    val categoryLabel: String,
    val placeholder: String,
    val revealedValue: String? = null,
    val decision: ReviewDecision = ReviewDecision.PENDING,
) {
    init {
        require(id.isNotBlank())
        require(categoryLabel.isNotBlank())
        require(placeholder.isNotBlank())
    }

    override fun toString(): String =
        "ReviewFindingModel(id=$id, categoryLabel=<redacted>, placeholder=$placeholder, revealedValue=<redacted>, decision=$decision)"
}

internal enum class ReviewDecision { PENDING, REDACT, IGNORE }
