package io.github.daniele21.redactguard.ui

/** Product-owned projection of the external local-AI dependency; no Binder type leaks into UI. */
internal enum class LocalAiConnectionStatus {
    /** Binder transport is connected but Host configuration has not yet been proven usable. */
    CONNECTED,
    CONFIGURING,
    READY,
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
                    false,
                    "La connessione con Local AI Harness è attiva. Sto ancora verificando l’assegnazione e la modalità disponibili per questa analisi.",
                )
            }

            LocalAiConnectionStatus.CONFIGURING -> {
                ConnectionBadgeModel(
                    "Configurazione AI locale",
                    StatusTone.NEUTRAL,
                    false,
                    "Sto verificando l’uso consentito e le modalità pubblicate da Local AI Harness. Nessun modello viene caricato durante questa verifica.",
                )
            }

            LocalAiConnectionStatus.READY -> {
                ConnectionBadgeModel(
                    "AI locale pronta",
                    StatusTone.READY,
                    true,
                    "La connessione e la configurazione per l’analisi sono disponibili. Il modello necessario verrà preparato automaticamente quando avvii l’analisi.",
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
                    "Configurazione AI non disponibile",
                    StatusTone.ERROR,
                    false,
                    "Local AI Harness è raggiungibile, ma non espone una configurazione compatibile per questa analisi. Verifica l’assegnazione dell’app e riprova.",
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

/** Product-owned shortcut for choosing a coherent group of PII definitions. */
internal data class ProtectionProfileChoice(
    val id: String,
    val label: String,
    val description: String,
    val selected: Boolean,
) {
    init {
        require(id.isNotBlank())
        require(label.isNotBlank())
        require(description.isNotBlank())
    }

    override fun toString(): String =
        "ProtectionProfileChoice(id=$id, label=<product-copy>, description=<product-copy>, selected=$selected)"
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

    override fun toString(): String = "LocalAiPresetChoice(id=$id, label=<consumer-safe>, description=<consumer-safe>, selected=$selected)"
}

internal data class LocalAiPresetUiState(
    /** All human-readable Host modes, including a single auto-selected option. */
    val choices: List<LocalAiPresetChoice> = emptyList(),
    val replacementNotice: String? = null,
) {
    val selectedChoice: LocalAiPresetChoice?
        get() = choices.singleOrNull(LocalAiPresetChoice::selected)

    val showSelector: Boolean
        get() = choices.size > 1

    override fun toString(): String =
        "LocalAiPresetUiState(choiceCount=${choices.size}, selectedCount=${choices.count(
            LocalAiPresetChoice::selected,
        )}, hasReplacementNotice=${replacementNotice != null})"
}

/**
 * Context shown around one finding. `maskedText` never contains a known review surface: every known
 * occurrence intersecting the window is replaced with its deterministic placeholder.
 */
internal data class ReviewContextModel(
    val maskedText: String,
    val focusPlaceholder: String,
    val pageNumber: Int,
) {
    init {
        require(maskedText.isNotBlank())
        require(focusPlaceholder.isNotBlank())
        require(focusPlaceholder in maskedText)
        require(pageNumber > 0)
    }

    override fun toString(): String =
        "ReviewContextModel(maskedText=<redacted>, focusPlaceholder=$focusPlaceholder, pageNumber=$pageNumber)"
}

internal data class ReviewFindingModel(
    val id: String,
    val categoryLabel: String,
    val placeholder: String,
    val context: ReviewContextModel,
    val revealedValue: String? = null,
    val decision: ReviewDecision = ReviewDecision.PENDING,
) {
    init {
        require(id.isNotBlank())
        require(categoryLabel.isNotBlank())
        require(placeholder.isNotBlank())
        require(context.focusPlaceholder == placeholder)
    }

    override fun toString(): String =
        "ReviewFindingModel(id=$id, categoryLabel=<redacted>, placeholder=$placeholder, context=<redacted>, " +
            "revealedValue=<redacted>, decision=$decision)"
}

internal enum class ReviewDecision { PENDING, REDACT, IGNORE }
