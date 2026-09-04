package io.github.daniele21.redactguard.ui

import io.github.daniele21.redactguard.domain.analysis.LocalAiExecutionPhase
import io.github.daniele21.redactguard.domain.analysis.LocalAiExecutionState
import io.github.daniele21.redactguard.domain.analysis.LocalAiPreparationAction

internal enum class AnalysisVisualStage {
    PREPARING,
    SEARCHING,
    FAILED,
}

internal data class AnalysisProgressModel(
    val title: String,
    val message: String,
    val contentDescription: String,
    val visualStage: AnalysisVisualStage = AnalysisVisualStage.PREPARING,
)

internal object AnalysisProgressProjector {
    fun starting(): AnalysisProgressModel =
        AnalysisProgressModel(
            title = "Avvio dell’analisi locale",
            message = "Attivo la configurazione locale assegnata e verifico le risorse necessarie.",
            contentDescription = "Avvio dell’analisi locale in corso",
        )

    fun project(state: LocalAiExecutionState): AnalysisProgressModel =
        when (state.phase) {
            LocalAiExecutionPhase.ACTIVATED -> {
                AnalysisProgressModel(
                    title = "Preparazione AI locale",
                    message = "La configurazione è attiva. Preparo le risorse locali necessarie all’analisi.",
                    contentDescription = "Configurazione AI locale attiva, preparazione delle risorse in corso",
                )
            }

            LocalAiExecutionPhase.PREPARING -> {
                projectPreparation(state.preparationAction)
            }

            LocalAiExecutionPhase.READY -> {
                AnalysisProgressModel(
                    title = "AI locale pronta",
                    message = "Le risorse locali sono pronte. L’analisi dei dati sensibili sta per iniziare.",
                    contentDescription = "AI locale pronta per l’analisi",
                )
            }

            LocalAiExecutionPhase.GENERATING -> {
                AnalysisProgressModel(
                    title = "Ricerca dei dati sensibili",
                    message = "L’AI locale sta cercando le categorie selezionate nel documento.",
                    contentDescription = "Analisi locale dei dati sensibili in corso",
                    visualStage = AnalysisVisualStage.SEARCHING,
                )
            }

            LocalAiExecutionPhase.FAILED -> {
                AnalysisProgressModel(
                    title = "AI locale non disponibile",
                    message = "La preparazione o l’analisi locale non può proseguire. Nessun risultato parziale verrà mostrato.",
                    contentDescription = "Analisi locale interrotta per un problema dell’AI locale",
                    visualStage = AnalysisVisualStage.FAILED,
                )
            }
        }

    private fun projectPreparation(action: LocalAiPreparationAction): AnalysisProgressModel =
        when (action) {
            LocalAiPreparationAction.LOADING -> {
                AnalysisProgressModel(
                    title = "Preparazione AI locale",
                    message = "Carico le risorse locali richieste dalla configurazione assegnata.",
                    contentDescription = "Caricamento delle risorse AI locali in corso",
                )
            }

            LocalAiPreparationAction.REUSING -> {
                AnalysisProgressModel(
                    title = "AI locale già disponibile",
                    message = "Riutilizzo le risorse locali già pronte per questa analisi.",
                    contentDescription = "Riutilizzo delle risorse AI locali già pronte",
                )
            }

            LocalAiPreparationAction.SWITCHING -> {
                AnalysisProgressModel(
                    title = "Aggiornamento AI locale",
                    message = "Preparo la configurazione locale assegnata prima di iniziare l’analisi.",
                    contentDescription = "Aggiornamento della configurazione AI locale in corso",
                )
            }

            LocalAiPreparationAction.NONE -> {
                error("Preparing execution requires a source-backed preparation action")
            }
        }
}
