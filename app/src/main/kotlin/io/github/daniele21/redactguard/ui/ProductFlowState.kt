package io.github.daniele21.redactguard.ui

import io.github.daniele21.redactguard.domain.failure.FailureRecoveryAction
import io.github.daniele21.redactguard.domain.failure.ProductFailure
import io.github.daniele21.redactguard.domain.failure.ProductFailureKind as CanonicalFailureKind

internal enum class ProductStep {
    IMPORT,
    IMPORTING,
    DEFINITIONS,
    ANALYZING,
    REVIEW,
    NO_FINDINGS,
    EXPORTING,
    EXPORTED,
    ERROR,
}

internal enum class ProductRetryTarget {
    NONE,
    ANALYSIS,
    EXPORT,
}

internal data class ProductErrorTechnicalDetails(
    val code: String,
    val cause: String,
    val stage: String,
    val operationId: String? = null,
) {
    init {
        require(code.isNotBlank())
        require(cause.isNotBlank())
        require(stage.isNotBlank())
    }
}

internal data class ProductErrorModel(
    val title: String,
    val message: String,
    val retryTarget: ProductRetryTarget,
    val technicalDetails: ProductErrorTechnicalDetails? = null,
)

internal data class RedactGuardProductUiState(
    val step: ProductStep = ProductStep.IMPORT,
    val connection: ConnectionBadgeModel = ConnectionBadgeProjector.project(LocalAiConnectionStatus.UNAVAILABLE),
    val definitions: List<DefinitionChoice> = emptyList(),
    val reviewFinding: ReviewFindingModel? = null,
    val reviewPosition: Int = 0,
    val reviewTotal: Int = 0,
    val exportEnabled: Boolean = false,
    val error: ProductErrorModel? = null,
) {
    init {
        require(reviewPosition >= 0)
        require(reviewTotal >= 0)
        require(reviewTotal == 0 || reviewPosition < reviewTotal)
        if (step == ProductStep.REVIEW) requireNotNull(reviewFinding)
        if (step == ProductStep.ERROR) requireNotNull(error)
    }

    override fun toString(): String =
        "RedactGuardProductUiState(step=$step, connection=${connection.label}, definitionCount=${definitions.size}, " +
            "hasReviewFinding=${reviewFinding != null}, reviewPosition=$reviewPosition, reviewTotal=$reviewTotal, " +
            "exportEnabled=$exportEnabled, errorKind=${error?.retryTarget}, errorCode=${error?.technicalDetails?.code})"
}

/** Temporary compatibility surface while FD-2/FD-3/FD-4 are wired into the ViewModel. */
internal enum class ProductFailureKind {
    IMPORT_UNREADABLE,
    IMPORT_UNSUPPORTED,
    HOST_UNAVAILABLE,
    HARNESS_INCOMPATIBLE,
    ANALYSIS_FAILED,
    REVIEW_INVALID,
    EXPORT_FAILED,
}

internal object ProductFailureProjector {
    fun project(failure: ProductFailure): ProductErrorModel {
        val copy = copyFor(failure.kind)
        return ProductErrorModel(
            title = copy.title,
            message = copy.message,
            retryTarget = retryTargetFor(failure.kind.recoveryAction),
            technicalDetails =
                ProductErrorTechnicalDetails(
                    code = failure.code,
                    cause = failure.kind.name,
                    stage = failure.kind.stage.name,
                    operationId = failure.operationId,
                ),
        )
    }

    fun project(kind: ProductFailureKind): ProductErrorModel =
        when (kind) {
            ProductFailureKind.IMPORT_UNREADABLE -> {
                ProductErrorModel(
                    title = "Impossibile leggere il PDF",
                    message = "Controlla l’accesso al file e prova a importarlo di nuovo.",
                    retryTarget = ProductRetryTarget.NONE,
                )
            }

            ProductFailureKind.IMPORT_UNSUPPORTED -> {
                ProductErrorModel(
                    title = "PDF non analizzabile",
                    message = "Il file è cifrato, non valido, troppo grande o non contiene testo utilizzabile.",
                    retryTarget = ProductRetryTarget.NONE,
                )
            }

            ProductFailureKind.HOST_UNAVAILABLE -> {
                ProductErrorModel(
                    title = "Harness non disponibile",
                    message = "Apri Harness e rendi disponibile il modello PII prima di riprovare.",
                    retryTarget = ProductRetryTarget.ANALYSIS,
                )
            }

            ProductFailureKind.HARNESS_INCOMPATIBLE -> {
                ProductErrorModel(
                    title = "Harness incompatibile",
                    message = "Il contratto Local AI disponibile non soddisfa i requisiti di RedactGuard.",
                    retryTarget = ProductRetryTarget.ANALYSIS,
                )
            }

            ProductFailureKind.ANALYSIS_FAILED -> {
                ProductErrorModel(
                    title = "Analisi non completata",
                    message = "Nessun risultato parziale è stato conservato. Puoi riprovare l’analisi.",
                    retryTarget = ProductRetryTarget.ANALYSIS,
                )
            }

            ProductFailureKind.REVIEW_INVALID -> {
                ProductErrorModel(
                    title = "Revisione non valida",
                    message = "Le occorrenze non possono essere esportate in modo sicuro. Avvia una nuova analisi.",
                    retryTarget = ProductRetryTarget.ANALYSIS,
                )
            }

            ProductFailureKind.EXPORT_FAILED -> {
                ProductErrorModel(
                    title = "Esportazione non riuscita",
                    message = "Il PDF protetto non è stato creato correttamente. Scegli una nuova destinazione.",
                    retryTarget = ProductRetryTarget.EXPORT,
                )
            }
        }

    private fun copyFor(kind: CanonicalFailureKind): ErrorCopy =
        when (kind) {
            CanonicalFailureKind.SOURCE_NOT_FOUND ->
                ErrorCopy("PDF non più disponibile", "Il file selezionato non è più accessibile. Seleziona di nuovo il PDF.")

            CanonicalFailureKind.SOURCE_UNREADABLE ->
                ErrorCopy("Impossibile leggere il PDF", "RedactGuard non può accedere al file selezionato. Controlla l’accesso e riprova.")

            CanonicalFailureKind.ENCRYPTED_PDF ->
                ErrorCopy("PDF protetto da password", "RedactGuard non può analizzare PDF cifrati. Rimuovi la protezione e riprova.")

            CanonicalFailureKind.MALFORMED_PDF ->
                ErrorCopy("PDF non valido", "La struttura del PDF è danneggiata o non valida. Usa una copia valida del documento.")

            CanonicalFailureKind.PARSER_FAILED ->
                ErrorCopy("Impossibile elaborare il PDF", "Il parser PDF ha incontrato un errore inatteso. Prova a importare di nuovo il documento.")

            CanonicalFailureKind.LIMIT_EXCEEDED ->
                ErrorCopy("PDF oltre i limiti supportati", "Il documento supera un limite di elaborazione locale. Usa un PDF più piccolo.")

            CanonicalFailureKind.EMPTY_PDF ->
                ErrorCopy("PDF vuoto", "Il documento non contiene pagine utilizzabili. Seleziona un altro PDF.")

            CanonicalFailureKind.IMAGE_ONLY_PDF ->
                ErrorCopy(
                    "PDF senza testo estraibile",
                    "Questo PDF non contiene testo che RedactGuard riesce ad analizzare. Potrebbe essere composto da immagini o scansioni. L’OCR non è attualmente supportato.",
                )

            CanonicalFailureKind.HOST_NOT_INSTALLED ->
                ErrorCopy("Harness non installato", "Installa Local AI Harness sul dispositivo prima di avviare l’analisi.")

            CanonicalFailureKind.HOST_UNAVAILABLE ->
                ErrorCopy("Harness non disponibile", "Apri Harness e rendi disponibile il modello PII, quindi riprova l’analisi.")

            CanonicalFailureKind.PERMISSION_DENIED ->
                ErrorCopy("Accesso a Harness negato", "Harness ha rifiutato RedactGuard. Aggiorna Harness e verifica l’autorizzazione dell’app.")

            CanonicalFailureKind.CAPABILITY_INCOMPATIBLE ->
                ErrorCopy("Harness incompatibile", "La versione di Harness non supporta il contratto richiesto da RedactGuard. Aggiorna Harness.")

            CanonicalFailureKind.PLAN_REJECTED ->
                ErrorCopy("Documento non analizzabile con questi limiti", "Il piano di analisi è stato rifiutato prima dell’inferenza. Usa un documento supportato e riprova.")

            CanonicalFailureKind.INVALID_STRUCTURED_RESULT ->
                ErrorCopy("Risposta AI non valida", "Harness ha restituito un risultato strutturato non valido. Nessun risultato parziale è stato conservato.")

            CanonicalFailureKind.INVALID_FINDINGS ->
                ErrorCopy("Risultati AI non validi", "I risultati ricevuti non superano i controlli di integrità. Nessun risultato parziale è stato conservato.")

            CanonicalFailureKind.CHUNK_FAILED ->
                ErrorCopy("Analisi non completata", "Una parte del documento non è stata analizzata correttamente. Nessun risultato parziale è stato conservato.")

            CanonicalFailureKind.DISCONNECTED ->
                ErrorCopy("Connessione con Harness interrotta", "Harness si è disconnesso durante l’operazione. Riconnettilo e riprova.")

            CanonicalFailureKind.CANCELLED ->
                ErrorCopy("Analisi annullata", "L’analisi è stata annullata e nessun risultato parziale è stato conservato.")

            CanonicalFailureKind.REVIEW_PENDING_DECISION ->
                ErrorCopy("Revisione incompleta", "Decidi se oscurare o ignorare tutte le occorrenze prima di esportare.")

            CanonicalFailureKind.REVIEW_UNKNOWN_SEGMENT,
            CanonicalFailureKind.REVIEW_MISSING_DEFINITION,
            CanonicalFailureKind.REVIEW_SOURCE_MISMATCH,
            CanonicalFailureKind.REVIEW_DUPLICATE_OCCURRENCE,
            CanonicalFailureKind.REVIEW_OVERLAP_CONFLICT,
            -> ErrorCopy("Revisione non valida", "Le occorrenze non possono essere esportate in modo sicuro. Avvia una nuova analisi.")

            CanonicalFailureKind.DESTINATION_UNWRITABLE ->
                ErrorCopy("Destinazione non scrivibile", "RedactGuard non può scrivere nella destinazione scelta. Selezionane un’altra.")

            CanonicalFailureKind.SOURCE_MISMATCH ->
                ErrorCopy("Documento cambiato", "Il contenuto da esportare non corrisponde più al documento analizzato. Avvia una nuova analisi.")

            CanonicalFailureKind.OUTPUT_LIMIT_EXCEEDED ->
                ErrorCopy("PDF protetto troppo grande", "L’esportazione supera il limite locale previsto. Inizia con un documento più piccolo.")

            CanonicalFailureKind.WRITER_FAILED ->
                ErrorCopy("Esportazione non riuscita", "Il PDF protetto non è stato creato correttamente. Puoi riprovare l’esportazione.")

            CanonicalFailureKind.UNKNOWN_INTERNAL ->
                ErrorCopy("Errore inatteso", "RedactGuard ha interrotto l’operazione in sicurezza. Inizia con un nuovo documento.")
        }

    private fun retryTargetFor(action: FailureRecoveryAction): ProductRetryTarget =
        when (action) {
            FailureRecoveryAction.OPEN_HARNESS,
            FailureRecoveryAction.RECONNECT_HARNESS,
            FailureRecoveryAction.RETRY_ANALYSIS,
            -> ProductRetryTarget.ANALYSIS

            FailureRecoveryAction.SELECT_EXPORT_DESTINATION,
            FailureRecoveryAction.RETRY_EXPORT,
            -> ProductRetryTarget.EXPORT

            FailureRecoveryAction.RESELECT_DOCUMENT,
            FailureRecoveryAction.REMOVE_PDF_PROTECTION,
            FailureRecoveryAction.USE_VALID_PDF,
            FailureRecoveryAction.USE_TEXT_PDF,
            FailureRecoveryAction.USE_SMALLER_DOCUMENT,
            FailureRecoveryAction.INSTALL_HARNESS,
            FailureRecoveryAction.UPDATE_HARNESS,
            FailureRecoveryAction.COMPLETE_REVIEW,
            FailureRecoveryAction.START_NEW_DOCUMENT,
            FailureRecoveryAction.NONE,
            -> ProductRetryTarget.NONE
        }

    private data class ErrorCopy(
        val title: String,
        val message: String,
    )
}
