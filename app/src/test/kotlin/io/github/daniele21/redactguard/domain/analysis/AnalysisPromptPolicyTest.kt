package io.github.daniele21.redactguard.domain.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisPromptPolicyTest {
    @Test
    fun `default prompt explicitly prevents observed missing placeholder and renamed type failures`() {
        val prompt = AnalysisPromptPolicy.defaultEditablePrompt

        assertEquals(3, AnalysisProtocol.PROMPT_VERSION)
        assertTrue(prompt.contains("selectedTypeIds"))
        assertTrue(prompt.contains("full-name"))
        assertTrue(prompt.contains("never return \"name\""))
        assertTrue(prompt.contains("omit it completely", ignoreCase = true))
        assertTrue(prompt.contains("[Missing]"))
        assertTrue(prompt.contains("{\"schemaVersion\":1,\"findings\":[]}"))
    }

    @Test
    fun `protected integrity contract is always appended after editable guidance`() {
        val effective = AnalysisPromptPolicy.effectiveInstruction("Find only explicit occurrences.")

        assertTrue(effective.startsWith("Find only explicit occurrences."))
        assertTrue(effective.endsWith(AnalysisPromptPolicy.protectedRules))
        assertTrue(effective.contains("surface must be a non-empty exact substring"))
        assertTrue(effective.contains("placeholders and sentinel values are forbidden"))
    }

    @Test
    fun `editable prompt validation normalizes line endings and rejects unsafe values`() {
        val normalized = AnalysisPromptPolicy.validate("  first\r\nsecond  ")
        assertTrue(normalized.isValid)
        assertEquals("first\nsecond", normalized.normalized)

        val blank = AnalysisPromptPolicy.validate(" \n ")
        assertTrue(AnalysisPromptIssue.EMPTY in blank.issues)

        val tooLong = AnalysisPromptPolicy.validate("x".repeat(AnalysisPromptPolicy.MAX_EDITABLE_CHARACTERS + 1))
        assertTrue(AnalysisPromptIssue.TOO_LONG in tooLong.issues)

        val control = AnalysisPromptPolicy.validate("valid\u0001invalid")
        assertTrue(AnalysisPromptIssue.UNSUPPORTED_CONTROL_CHARACTER in control.issues)
        assertFalse(control.isValid)
    }

    @Test
    fun `validation diagnostics never expose prompt contents`() {
        val sensitivePrompt = "private-guidance-7f4c"
        val validation = AnalysisPromptPolicy.validate(sensitivePrompt)

        assertFalse(validation.toString().contains(sensitivePrompt))
        assertTrue(validation.toString().contains("normalized=<redacted>"))
    }
}
