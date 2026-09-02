package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.contracts.ConsumerControlPlaneErrorCode
import io.github.daniele21.localllm.contracts.ConsumerControlPlaneFailure
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeDiagnostic
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeException
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeFailureCode

internal fun ConsumerControlPlaneFailure.toAnalysisFailureCode(transportConnected: () -> Boolean): AnalysisRuntimeFailureCode =
    when (code) {
        ConsumerControlPlaneErrorCode.TRANSPORT_FAILURE -> {
            AnalysisRuntimeFailureCode.DISCONNECTED
        }

        ConsumerControlPlaneErrorCode.CONFIGURATION_REQUIRED,
        ConsumerControlPlaneErrorCode.USE_CASE_NOT_ASSIGNED,
        ConsumerControlPlaneErrorCode.PRESET_NOT_EXPOSED,
        ConsumerControlPlaneErrorCode.STALE_REVISION,
        -> {
            AnalysisRuntimeFailureCode.CONFIGURATION_REQUIRED
        }

        ConsumerControlPlaneErrorCode.MODEL_UNAVAILABLE,
        ConsumerControlPlaneErrorCode.MODEL_CONFLICT,
        -> {
            AnalysisRuntimeFailureCode.MODEL_UNAVAILABLE
        }

        ConsumerControlPlaneErrorCode.INVALID_REQUEST -> {
            AnalysisRuntimeFailureCode.INVALID_REQUEST
        }

        ConsumerControlPlaneErrorCode.ACTIVATION_ALREADY_ACTIVE -> {
            AnalysisRuntimeFailureCode.HOST_UNAVAILABLE
        }

        ConsumerControlPlaneErrorCode.RUNTIME_FAILURE -> {
            if (transportConnected()) AnalysisRuntimeFailureCode.GENERATION_FAILED else AnalysisRuntimeFailureCode.DISCONNECTED
        }

        ConsumerControlPlaneErrorCode.FEATURE_UNAVAILABLE -> {
            AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE
        }
    }

internal fun ConsumerControlPlaneFailure.toAnalysisRuntimeException(
    step: String,
    transportConnected: () -> Boolean,
): AnalysisRuntimeException =
    AnalysisRuntimeException(
        code = toAnalysisFailureCode(transportConnected),
        diagnostic =
            AnalysisRuntimeDiagnostic(
                step = step,
                type = "ControlPlane:${code.name}",
            ),
    )
