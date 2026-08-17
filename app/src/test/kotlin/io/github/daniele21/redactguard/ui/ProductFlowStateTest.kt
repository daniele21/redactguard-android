package io.github.daniele21.redactguard.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductFlowStateTest {
    @Test
    fun `aggregate UI state diagnostics never expose custom labels or revealed values`() {
        val sensitiveLabel = "Cliente Mario Rossi"
        val sensitiveValue = "mario.rossi@example.test"
        val state =
            RedactGuardProductUiState(
                step = ProductStep.REVIEW,
                connection = ConnectionBadgeProjector.project(LocalAiConnectionStatus.CONNECTED),
                definitions = listOf(DefinitionChoice("custom-1", sensitiveLabel, selected = true)),
                reviewFinding =
                    ReviewFindingModel(
                        id = "email:p0001-b0001:0-24",
                        categoryLabel = sensitiveLabel,
                        placeholder = "[EMAIL_1]",
                        revealedValue = sensitiveValue,
                    ),
                reviewPosition = 0,
                reviewTotal = 1,
            )

        val diagnostics = state.toString()

        assertFalse(diagnostics.contains(sensitiveLabel))
        assertFalse(diagnostics.contains(sensitiveValue))
        assertTrue(diagnostics.contains("definitionCount=1"))
        assertTrue(diagnostics.contains("hasReviewFinding=true"))
    }

    @Test
    fun `custom PII input diagnostics redact every user field`() {
        val input = CustomPiiInput("Badge Mario Rossi", "Codice personale 123", "MR-123")

        val diagnostics = input.toString()

        assertFalse(diagnostics.contains("Mario Rossi"))
        assertFalse(diagnostics.contains("Codice personale"))
        assertFalse(diagnostics.contains("MR-123"))
    }

    @Test
    fun `definition and review models redact sensitive labels from diagnostics`() {
        val definition = DefinitionChoice("custom-1", "Cliente Segreto", selected = true)
        val finding =
            ReviewFindingModel(
                id = "custom-1:p0001-b0001:0-7",
                categoryLabel = "Cliente Segreto",
                placeholder = "[CLIENTE_1]",
                revealedValue = "segreto",
            )

        assertFalse(definition.toString().contains("Cliente Segreto"))
        assertFalse(finding.toString().contains("Cliente Segreto"))
        assertFalse(finding.toString().contains("segreto"))
    }
}
