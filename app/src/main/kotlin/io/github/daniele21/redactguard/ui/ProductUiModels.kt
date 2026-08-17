package io.github.daniele21.redactguard.ui

/** Product-owned projection of the external Harness connection; no Binder type leaks into UI. */
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
            LocalAiConnectionStatus.CONNECTED ->
                ConnectionBadgeModel("Harness connesso", StatusTone.READY, true)

            LocalAiConnectionStatus.CONNECTING ->
                ConnectionBadgeModel(
                    "Connessione a Harness",
                    StatusTone.NEUTRAL,
                    false,
                    "Sto verificando disponibilità, compatibilità e autorizzazione di Harness.",
                )

            LocalAiConnectionStatus.PERMISSION_DENIED ->
                ConnectionBadgeModel(
                    "Accesso a Harness negato",
                    StatusTone.ERROR,
                    false,
                    "Harness ha rifiutato RedactGuard. Aggiorna Harness e riprova; il documento non viene inviato né perso.",
                )

            LocalAiConnectionStatus.INCOMPATIBLE ->
                ConnectionBadgeModel(
                    "Harness incompatibile",
                    StatusTone.ERROR,
                    false,
                    "La versione installata di Harness non supporta il protocollo richiesto da RedactGuard. Aggiorna Harness e riprova.",
                )

            LocalAiConnectionStatus.HOST_NOT_INSTALLED ->
                ConnectionBadgeModel(
                    "Harness non installato",
                    StatusTone.ERROR,
                    false,
                    "Installa Harness sul dispositivo, quindi torna in RedactGuard: la connessione verrà ritentata automaticamente.",
                )

            LocalAiConnectionStatus.DISCONNECTED ->
                ConnectionBadgeModel(
                    "Harness disconnesso",
                    StatusTone.REVIEW,
                    false,
                    "La connessione locale con Harness è stata interrotta. Verifica Harness e torna nell’app per riprovare.",
                )

            LocalAiConnectionStatus.UNAVAILABLE ->
                ConnectionBadgeModel(
                    "Harness non disponibile",
                    StatusTone.NEUTRAL,
                    false,
                    "Harness non è raggiungibile in questo momento. L’app resta utilizzabile e puoi riprovare senza riavviarla.",
                )
        }
}

internal data class DefinitionChoice(
    val id: String,
    val label: String,
    val selected: Boolean,
) {
    override fun toString(): String = "DefinitionChoice(id=$id, label=<redacted>, selected=$selected)"
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
