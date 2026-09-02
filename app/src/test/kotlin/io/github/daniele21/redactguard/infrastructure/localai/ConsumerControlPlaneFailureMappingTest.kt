package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.contracts.ConsumerControlPlaneErrorCode
import io.github.daniele21.localllm.contracts.ConsumerControlPlaneFailure
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeDiagnostic
import io.github.daniele21.redactguard.domain.analysis.AnalysisRuntimeFailureCode
import org.junit.Assert.assertEquals
import org.junit.Test

class ConsumerControlPlaneFailureMappingTest {
    @Test
    fun `consumer registration failures require configuration without becoming incompatibility`() {
        listOf(
            ConsumerControlPlaneErrorCode.UNKNOWN_APPLICATION,
            ConsumerControlPlaneErrorCode.APPLICATION_NOT_AUTHORIZED,
        ).forEach { code ->
            val failure = ConsumerControlPlaneFailure(code, "untrusted host detail")

            val mapped = failure.toAnalysisRuntimeException("control-plane.setup-resolution") { true }

            assertEquals(AnalysisRuntimeFailureCode.CONFIGURATION_REQUIRED, mapped.code)
            assertEquals(
                AnalysisRuntimeDiagnostic(
                    step = "control-plane.setup-resolution",
                    type = "ControlPlane:${code.name}",
                ),
                mapped.diagnostic,
            )
        }
    }
}
