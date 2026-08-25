package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeDiagnostic
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeException
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeFailureCode

/** Converts unexpected SDK/platform runtime failures into a privacy-safe app-owned identity. */
internal inline fun <T> localAiBoundary(
    step: String,
    block: () -> T,
): T =
    try {
        block()
    } catch (failure: AnalysisRuntimeException) {
        throw failure
    } catch (failure: RuntimeException) {
        throw AnalysisRuntimeException(
            code = AnalysisRuntimeFailureCode.INTERNAL_FAILURE,
            diagnostic = AnalysisRuntimeDiagnostic(step = step, type = safeRuntimeFailureType(failure)),
        )
    }

internal fun unexpectedLocalAiFailure(
    step: String,
    failure: RuntimeException,
): AnalysisRuntimeException =
    if (failure is AnalysisRuntimeException) {
        failure
    } else {
        AnalysisRuntimeException(
            code = AnalysisRuntimeFailureCode.INTERNAL_FAILURE,
            diagnostic = AnalysisRuntimeDiagnostic(step = step, type = safeRuntimeFailureType(failure)),
        )
    }

private fun safeRuntimeFailureType(failure: RuntimeException): String =
    when (failure) {
        is SecurityException -> "SecurityException"
        is IllegalStateException -> "IllegalStateException"
        is IllegalArgumentException -> "IllegalArgumentException"
        is UnsupportedOperationException -> "UnsupportedOperationException"
        else -> "RuntimeException"
    }
