package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.contracts.ConsumerErrorCode
import io.github.daniele21.localllm.contracts.ConsumerFailure
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeDiagnostic
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeFailureCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ConsumerFailureMappingTest {
    @Test
    fun `every consumer error preserves enum identity without preserving free-form message`() {
        listOf(true, false).forEach { connected ->
            ConsumerErrorCode.entries.forEach { code ->
                val secretMessage = "raw failure detail alice@example.test ${code.name}"
                val failure = ConsumerFailure(code, secretMessage)

                val mapped = failure.toAnalysisRuntimeException("consumer.generate") { connected }

                assertEquals(expectedRuntimeFailure(code, connected), mapped.code)
                assertEquals(
                    AnalysisRuntimeDiagnostic(
                        step = "consumer.generate",
                        type = "Consumer:${code.name}",
                    ),
                    mapped.diagnostic,
                )
                assertFalse(mapped.message.orEmpty().contains(secretMessage))
                assertFalse(mapped.toString().contains("alice@example.test"))
            }
        }
    }

    @Test
    fun `typed consumer diagnostic accepts only the caller-owned safe step`() {
        val failure = ConsumerFailure(ConsumerErrorCode.RUNTIME_FAILURE, "untrusted runtime detail")

        val mapped = failure.toAnalysisRuntimeException("consumer.create-session") { true }

        assertEquals(
            AnalysisRuntimeDiagnostic(
                step = "consumer.create-session",
                type = "Consumer:RUNTIME_FAILURE",
            ),
            mapped.diagnostic,
        )
    }

    private fun expectedRuntimeFailure(
        code: ConsumerErrorCode,
        connected: Boolean,
    ): AnalysisRuntimeFailureCode =
        when (code) {
            ConsumerErrorCode.MODEL_UNAVAILABLE -> {
                AnalysisRuntimeFailureCode.MODEL_UNAVAILABLE
            }

            ConsumerErrorCode.CANCELLED -> {
                AnalysisRuntimeFailureCode.CANCELLED
            }

            ConsumerErrorCode.RUNTIME_FAILURE,
            ConsumerErrorCode.PREPARE_FAILED,
            ConsumerErrorCode.SESSION_NOT_FOUND,
            -> {
                if (connected) AnalysisRuntimeFailureCode.GENERATION_FAILED else AnalysisRuntimeFailureCode.DISCONNECTED
            }

            ConsumerErrorCode.CAPABILITY_INCOMPATIBLE -> {
                if (connected) AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE else AnalysisRuntimeFailureCode.DISCONNECTED
            }

            else -> {
                AnalysisRuntimeFailureCode.CAPABILITY_INCOMPATIBLE
            }
        }
}
