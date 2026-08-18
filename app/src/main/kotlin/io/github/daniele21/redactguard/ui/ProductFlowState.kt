package io.github.daniele21.redactguard.ui

import io.github.daniele21.redactguard.domain.failure.FailureRecoveryAction
import io.github.daniele21.redactguard.domain.failure.ProductFailure
import io.github.daniele21.redactguard.domain.failure.ProductFailureKind

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
    val technicalDetails: ProductErrorTechnicalDetails,
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

    private fun copyFor(kind: ProductFailureKind): ErrorCopy =
        when (kind) {
            ProductFailureKind.SOURCE_NOT_FOUND -> {
                ErrorCopy(
                    "PDF non più disponibile",
                    "Il file selezionato non è più accessibile. Seleziona di nuovo il PDF.",
                )
            }

            ProductFailureKind.SOURCE_UNREADABLE -> {
                ErrorCopy(
                    "Impossibile leggere il PDF",
                    "RedactGuard non può accedere al file selezionato. Controlla l’accesso e riprova.",
                )
            }

            ProductFailureKind.ENCRYPTED_PDF -> {
                ErrorCopy(
                    "PDF protetto da password",
                    "RedactGuard non può analizzare PDF cifrati. Rimuovi la protezione e riprova.",
                )
            }

            ProductFailureKind.MALFORMED_PDF -> {
                ErrorCopy(
                    "PDF non valido",
                    "La struttura del PDF è danneggiata o non valida. Usa una copia valida del documento.",
                )
            }

            ProductFailureKind.PARSER_FAILED -> {
                ErrorCopy(
                    "Impossibile elaborare il PDF",
                    "Il parser PDF ha incontrato un errore inatteso. Prova a importare di nuovo il documento.",
                )
            }

            ProductFailureKind.LIMIT_EXCEEDED -> {
                ErrorCopy(
                    "PDF oltre i limiti supportati",
                    "Il documento supera un limite di elaborazione locale. Usa un PDF più piccolo.",
                )
            }

            ProductFailureKind.EMPTY_PDF -> {
                ErrorCopy(
                    "PDF vuoto",
                    "Il documento non contiene pagine utilizzabili. Seleziona un altro PDF.",
                )
            }

            ProductFailureKind.IMAGE_ONLY_PDF -> {
                ErrorCopy(
                    "PDF senza testo estraibile",
                    "Questo PDF non contiene testo che RedactGuard riesce ad analizzare. Potrebbe essere composto da immagini o scansioni. L’OCR non è attualmente supportato.",
                )
            }

            ProductFailureKind.HOST_NOT_INSTALLED -> {
                ErrorCopy(
                    "Harness non installato",
                    "Installa Local AI Harness sul dispositivo prima di avviare l’analisi.",
                )
            }

            ProductFailureKind.HOST_UNAVAILABLE -> {
                ErrorCopy(
                    "Harness non disponibile",
                    "Apri Harness e rendi disponibile il modello PII, quindi riprova l’analisi.",
                )
            }

            ProductFailureKind.PERMISSION_DENIED -> {
                ErrorCopy(
                    "Accesso a Harness negato",
                    "Harness ha rifiutato RedactGuard. Aggiorna Harness e verifica l’autorizzazione dell’app.",
                )
            }

            ProductFailureKind.CAPABILITY_INCOMPATIBLE -> {
                ErrorCopy(
                    "Harness incompatibile",
                    "La versione di Harness non supporta il contratto richiesto da RedactGuard. Aggiorna Harness.",
                )
            }

            ProductFailureKind.PLAN_REJECTED -> {
                ErrorCopy(
                    "Documento non analizzabile con questi limiti",
                    "Il piano di analisi è stato rifiutato prima dell’inferenza. Usa un documento supportato e riprova.",
                )
            }

            ProductFailureKind.INVALID_STRUCTURED_RESULT -> {
                ErrorCopy(
                    "Risposta AI non valida",
                    "Harness ha restituito un risultato strutturato non valido. Nessun risultato parziale è stato conservato.",
                )
            }

            ProductFailureKind.INVALID_FINDINGS -> {
                ErrorCopy(
                    "Risultati AI non validi",
                    "I risultati ricevuti non superano i controlli di integrità. Nessun risultato parziale è stato conservato.",
                )
            }

            ProductFailureKind.CHUNK_FAILED -> {
                ErrorCopy(
                    "Analisi non completata",
                    "Una parte del documento non è stata analizzata correttamente. Nessun risultato parziale è stato conservato.",
                )
            }

            ProductFailureKind.DISCONNECTED -> {
                ErrorCopy(
                    "Connessione con Harness interrotta",
                    "Harness si è disconnesso durante l’operazione. Riconnettilo e riprova.",
                )
            }

            ProductFailureKind.CANCELLED -> {
                ErrorCopy(
                    "Analisi annullata",
                    "L’analisi è stata annullata e nessun risultato parziale è stato conservato.",
                )
            }

            ProductFailureKind.RUNTIME_CLEANUP_FAILED -> {
                ErrorCopy(
                    "Analisi non finalizzata in sicurezza",
                    "RedactGuard non è riuscito a chiudere correttamente l’operazione locale. Riconnetti Harness e riprova.",
                )
            }

            ProductFailureKind.REVIEW_PENDING_DECISION -> {
                ErrorCopy(
                    "Revisione incompleta",
                    "Decidi se oscurare o ignorare tutte le occorrenze prima di esportare.",
                )
            }

            ProductFailureKind.REVIEW_UNKNOWN_SEGMENT,
            ProductFailureKind.REVIEW_MISSING_DEFINITION,
            ProductFailureKind.REVIEW_SOURCE_MISMATCH,
            ProductFailureKind.REVIEW_DUPLICATE_OCCURRENCE,
            ProductFailureKind.REVIEW_OVERLAP_CONFLICT,
            ProductFailureKind.REVIEW_DUPLICATE_DEFINITION,
            ProductFailureKind.REVIEW_UNKNOWN_REVEAL_OCCURRENCE,
            -> {
                ErrorCopy(
                    "Revisione non valida",
                    "Le occorrenze non possono essere esportate in modo sicuro. Avvia una nuova analisi.",
                )
            }

            ProductFailureKind.DESTINATION_UNWRITABLE -> {
                ErrorCopy(
                    "Destinazione non scrivibile",
                    "RedactGuard non può scrivere nella destinazione scelta. Selezionane un’altra.",
                )
            }

            ProductFailureKind.SOURCE_MISMATCH -> {
                ErrorCopy(
                    "Documento cambiato",
                    "Il contenuto da esportare non corrisponde più al documento analizzato. Avvia una nuova analisi.",
                )
            }

            ProductFailureKind.OUTPUT_LIMIT_EXCEEDED -> {
                ErrorCopy(
                    "PDF protetto troppo grande",
                    "L’esportazione supera il limite locale previsto. Inizia con un documento più piccolo.",
                )
            }

            ProductFailureKind.WRITER_FAILED -> {
                ErrorCopy(
                    "Esportazione non riuscita",
                    "Il PDF protetto non è stato creato correttamente. Puoi riprovare l’esportazione.",
                )
            }

            ProductFailureKind.UNKNOWN_INTERNAL -> {
                ErrorCopy(
                    "Errore inatteso",
                    "RedactGuard ha interrotto l’operazione in sicurezza. Inizia con un nuovo documento.",
                )
            }
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
