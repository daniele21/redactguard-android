package io.github.daniele21.redactguard.domain.analysis

@JvmInline
internal value class RedactionJobId(
    val value: String,
) {
    init {
        require(SAFE_ID.matches(value)) { "Redaction job ID must be a privacy-safe identifier" }
    }

    private companion object {
        val SAFE_ID = Regex("^[A-Za-z0-9._:-]{1,96}$")
    }
}

internal enum class RedactionJobState {
    IMPORTED,
    PREPROCESSING,
    READY_FOR_ANALYSIS,
    ANALYZING,
    POSTPROCESSING,
    READY_FOR_REVIEW,
    EXPORTING,
    DONE,
    CANCEL_REQUESTED,
    CANCELLED,
    RECOVERING,
    FAILED_RECOVERABLE,
    FAILED_FINAL,
}

/** Transport is deliberately independent from product-job state. */
internal enum class AnalysisTransportState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
}

internal enum class RecoveryCapability {
    RECOVERABLE_TRANSPARENTLY,
    RECOVERABLE_WITH_PROCESS_STATE,
    REQUIRES_SOURCE_REOPEN,
    NOT_RECOVERABLE,
}

/**
 * Privacy-safe product-job metadata. It must never contain document text, findings, prompts,
 * generated output, private paths or raw Binder payloads.
 */
internal data class RedactionJobSnapshot(
    val jobId: RedactionJobId,
    val state: RedactionJobState,
    val revision: Long,
    val attempt: Int,
    val recoveryCapability: RecoveryCapability,
    val cancelRequested: Boolean = false,
) {
    init {
        require(revision >= 0) { "Redaction job revision must be non-negative" }
        require(attempt >= 1) { "Redaction job attempt must be positive" }
        require(!cancelRequested || state in CANCELLATION_STATES) {
            "Cancellation intent must use a cancellation state"
        }
    }

    private companion object {
        val CANCELLATION_STATES = setOf(RedactionJobState.CANCEL_REQUESTED, RedactionJobState.CANCELLED)
    }
}

internal data class RedactionJobTransition(
    val state: RedactionJobState,
    val revision: Long,
    val attempt: Int,
    val recoveryCapability: RecoveryCapability,
    val cancelRequested: Boolean = false,
)

internal object RedactionJobLifecycle {
    fun apply(
        current: RedactionJobSnapshot,
        transition: RedactionJobTransition,
    ): RedactionJobSnapshot {
        if (transition.revision <= current.revision) return current
        require(transition.attempt >= current.attempt) { "Redaction job attempt cannot move backwards" }

        if (transition.attempt == current.attempt) {
            require(transition.state in allowedNextStates(current.state)) {
                "Illegal redaction job transition ${current.state} -> ${transition.state}"
            }
        } else {
            require(transition.attempt == current.attempt + 1) { "Redaction job attempt must advance one step at a time" }
            require(transition.state == RedactionJobState.RECOVERING) {
                "A new redaction job attempt must begin in RECOVERING"
            }
        }

        return RedactionJobSnapshot(
            jobId = current.jobId,
            state = transition.state,
            revision = transition.revision,
            attempt = transition.attempt,
            recoveryCapability = transition.recoveryCapability,
            cancelRequested = transition.cancelRequested,
        )
    }

    private fun allowedNextStates(state: RedactionJobState): Set<RedactionJobState> =
        when (state) {
            RedactionJobState.IMPORTED -> {
                setOf(RedactionJobState.PREPROCESSING, RedactionJobState.READY_FOR_ANALYSIS, RedactionJobState.FAILED_FINAL)
            }

            RedactionJobState.PREPROCESSING -> {
                setOf(RedactionJobState.READY_FOR_ANALYSIS, RedactionJobState.FAILED_RECOVERABLE, RedactionJobState.FAILED_FINAL)
            }

            RedactionJobState.READY_FOR_ANALYSIS -> {
                setOf(RedactionJobState.ANALYZING, RedactionJobState.CANCEL_REQUESTED, RedactionJobState.FAILED_FINAL)
            }

            RedactionJobState.ANALYZING -> {
                setOf(
                    RedactionJobState.POSTPROCESSING,
                    RedactionJobState.CANCEL_REQUESTED,
                    RedactionJobState.RECOVERING,
                    RedactionJobState.FAILED_RECOVERABLE,
                    RedactionJobState.FAILED_FINAL,
                )
            }

            RedactionJobState.POSTPROCESSING -> {
                setOf(
                    RedactionJobState.READY_FOR_REVIEW,
                    RedactionJobState.CANCEL_REQUESTED,
                    RedactionJobState.FAILED_RECOVERABLE,
                    RedactionJobState.FAILED_FINAL,
                )
            }

            RedactionJobState.READY_FOR_REVIEW -> {
                setOf(RedactionJobState.EXPORTING, RedactionJobState.DONE)
            }

            RedactionJobState.EXPORTING -> {
                setOf(RedactionJobState.DONE, RedactionJobState.FAILED_RECOVERABLE, RedactionJobState.FAILED_FINAL)
            }

            RedactionJobState.CANCEL_REQUESTED -> {
                setOf(RedactionJobState.CANCELLED, RedactionJobState.DONE, RedactionJobState.FAILED_FINAL)
            }

            RedactionJobState.RECOVERING -> {
                setOf(
                    RedactionJobState.ANALYZING,
                    RedactionJobState.READY_FOR_ANALYSIS,
                    RedactionJobState.CANCEL_REQUESTED,
                    RedactionJobState.FAILED_RECOVERABLE,
                    RedactionJobState.FAILED_FINAL,
                )
            }

            RedactionJobState.FAILED_RECOVERABLE -> {
                setOf(RedactionJobState.RECOVERING, RedactionJobState.CANCEL_REQUESTED, RedactionJobState.FAILED_FINAL)
            }

            RedactionJobState.DONE,
            RedactionJobState.CANCELLED,
            RedactionJobState.FAILED_FINAL,
            -> {
                emptySet()
            }
        }
}

internal data class RedactionJobObservation(
    val job: RedactionJobSnapshot,
    val transport: AnalysisTransportState,
) {
    fun withTransport(state: AnalysisTransportState): RedactionJobObservation = copy(transport = state)

    fun withJobTransition(transition: RedactionJobTransition): RedactionJobObservation =
        copy(job = RedactionJobLifecycle.apply(job, transition))
}
