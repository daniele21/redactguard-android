package io.github.daniele21.redactguard.domain.analysis

internal enum class LocalAiExecutionPhase {
    ACTIVATED,
    PREPARING,
    READY,
    GENERATING,
    FAILED,
}

internal enum class LocalAiPreparationAction {
    NONE,
    LOADING,
    REUSING,
    SWITCHING,
}

/** Product-owned runtime lifecycle projection. It intentionally carries no model/runtime identity. */
internal data class LocalAiExecutionState(
    val phase: LocalAiExecutionPhase,
    val preparationAction: LocalAiPreparationAction = LocalAiPreparationAction.NONE,
    val failureCode: AnalysisRuntimeFailureCode? = null,
    val retryable: Boolean = false,
) {
    init {
        require((phase == LocalAiExecutionPhase.PREPARING) == (preparationAction != LocalAiPreparationAction.NONE)) {
            "Preparation action is present only while preparing"
        }
        require((phase == LocalAiExecutionPhase.FAILED) == (failureCode != null)) {
            "Failure code is present only for failed execution"
        }
        require(!retryable || phase == LocalAiExecutionPhase.FAILED) {
            "Retryable applies only to failed execution"
        }
    }
}
