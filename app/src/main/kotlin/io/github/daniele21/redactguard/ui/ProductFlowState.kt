package io.github.daniele21.redactguard.ui

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

internal data class ProductErrorModel(
    val title: String,
    val message: String,
    val retryTarget: ProductRetryTarget,
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
}

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
    fun project(kind: ProductFailureKind): ProductErrorModel =
        when (kind) {
            ProductFailureKind.IMPORT_UNREADABLE ->
                ProductErrorModel(
                    title = "Impossibile leggere il PDF",
                    message = "Controlla l’accesso al file e prova a importarlo di nuovo.",
                    retryTarget = ProductRetryTarget.NONE,
                )

            ProductFailureKind.IMPORT_UNSUPPORTED ->
                ProductErrorModel(
                    title = "PDF non analizzabile",
                    message = "Il file è cifrato, non valido, troppo grande o non contiene testo utilizzabile.",
                    retryTarget = ProductRetryTarget.NONE,
                )

            ProductFailureKind.HOST_UNAVAILABLE ->
                ProductErrorModel(
                    title = "Harness non disponibile",
                    message = "Apri Harness e rendi disponibile il modello PII prima di riprovare.",
                    retryTarget = ProductRetryTarget.ANALYSIS,
                )

            ProductFailureKind.HARNESS_INCOMPATIBLE ->
                ProductErrorModel(
                    title = "Harness incompatibile",
                    message = "Il contratto Local AI disponibile non soddisfa i requisiti di RedactGuard.",
                    retryTarget = ProductRetryTarget.ANALYSIS,
                )

            ProductFailureKind.ANALYSIS_FAILED ->
                ProductErrorModel(
                    title = "Analisi non completata",
                    message = "Nessun risultato parziale è stato conservato. Puoi riprovare l’analisi.",
                    retryTarget = ProductRetryTarget.ANALYSIS,
                )

            ProductFailureKind.REVIEW_INVALID ->
                ProductErrorModel(
                    title = "Revisione non valida",
                    message = "Le occorrenze non possono essere esportate in modo sicuro. Avvia una nuova analisi.",
                    retryTarget = ProductRetryTarget.ANALYSIS,
                )

            ProductFailureKind.EXPORT_FAILED ->
                ProductErrorModel(
                    title = "Esportazione non riuscita",
                    message = "Il PDF protetto non è stato creato correttamente. Scegli una nuova destinazione.",
                    retryTarget = ProductRetryTarget.EXPORT,
                )
        }
}
