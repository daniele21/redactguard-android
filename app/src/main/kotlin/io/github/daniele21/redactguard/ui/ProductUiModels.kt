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
)

internal object ConnectionBadgeProjector {
    fun project(status: LocalAiConnectionStatus): ConnectionBadgeModel =
        when (status) {
            LocalAiConnectionStatus.CONNECTED -> ConnectionBadgeModel("Harness connesso", StatusTone.READY, true)
            LocalAiConnectionStatus.CONNECTING -> ConnectionBadgeModel("Connessione a Harness", StatusTone.NEUTRAL, false)
            LocalAiConnectionStatus.PERMISSION_DENIED -> ConnectionBadgeModel("Accesso a Harness negato", StatusTone.ERROR, false)
            LocalAiConnectionStatus.INCOMPATIBLE -> ConnectionBadgeModel("Harness incompatibile", StatusTone.ERROR, false)
            LocalAiConnectionStatus.HOST_NOT_INSTALLED -> ConnectionBadgeModel("Harness non disponibile", StatusTone.ERROR, false)
            LocalAiConnectionStatus.DISCONNECTED -> ConnectionBadgeModel("Harness disconnesso", StatusTone.REVIEW, false)
            LocalAiConnectionStatus.UNAVAILABLE -> ConnectionBadgeModel("Harness non disponibile", StatusTone.NEUTRAL, false)
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
