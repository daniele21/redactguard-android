package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.contracts.ConsumerErrorCode
import io.github.daniele21.localllm.contracts.ConsumerFailure
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeDiagnostic
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeException
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeFailureCode

internal fun ConsumerFailure.toAnalysisFailureCode(transportConnected: () -> Boolean): AnalysisRuntimeFailureCode =
    when (code) {
        ConsumerErrorCode.MODEL_UNAVAILABLE -> {
            AnalysisRuntimeFailureCode.HOST_UNAVAILABLE
        }

        ConsumerErrorCode.CANCELLED -> {
            AnalysisRuntimeFailureCode.CANCELLED
        }

        ConsumerErrorCode.RUNTIME_FAILURE,
        ConsumerErrorCode.PREPARE_FAILED,
        ConsumerErrorCode.SESSION_NOT_FOUND,
        -> {
            if (transportConnected()) AnalysisRuntimeFailureCode.GENERATION_FAILED else AnalysisRuntimeFailureCode.DISCONNECTED
        }

        ConsumerErrorCode.CAPABILITY_INCOMPATIBLE -> {
            if (transportConnected()) AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE else AnalysisRuntimeFailureCode.DISCONNECTED
        }

        else -> {
            AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE
        }
    }

internal fun ConsumerFailure.toAnalysisRuntimeException(
    step: String,
    transportConnected: () -> Boolean,
): AnalysisRuntimeException =
    AnalysisRuntimeException(
        code = toAnalysisFailureCode(transportConnected),
        diagnostic =
            AnalysisRuntimeDiagnostic(
                step = step,
                type = "Consumer:${code.name}",
            ),
    )
