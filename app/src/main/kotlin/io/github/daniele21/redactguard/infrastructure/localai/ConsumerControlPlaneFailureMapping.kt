package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.contracts.ConsumerControlPlaneErrorCode
import io.github.daniele21.localllm.contracts.ConsumerControlPlaneFailure
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeFailureCode

internal fun ConsumerControlPlaneFailure.toAnalysisFailureCode(transportConnected: () -> Boolean): AnalysisRuntimeFailureCode =
    when (code) {
        ConsumerControlPlaneErrorCode.TRANSPORT_FAILURE -> {
            AnalysisRuntimeFailureCode.DISCONNECTED
        }

        ConsumerControlPlaneErrorCode.MODEL_UNAVAILABLE,
        ConsumerControlPlaneErrorCode.CONFIGURATION_REQUIRED,
        ConsumerControlPlaneErrorCode.MODEL_CONFLICT,
        ConsumerControlPlaneErrorCode.ACTIVATION_ALREADY_ACTIVE,
        -> {
            AnalysisRuntimeFailureCode.HOST_UNAVAILABLE
        }

        ConsumerControlPlaneErrorCode.RUNTIME_FAILURE -> {
            if (transportConnected()) AnalysisRuntimeFailureCode.GENERATION_FAILED else AnalysisRuntimeFailureCode.DISCONNECTED
        }

        else -> {
            AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE
        }
    }
