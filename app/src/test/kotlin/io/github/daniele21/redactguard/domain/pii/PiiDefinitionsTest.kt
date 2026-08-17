package io.github.daniele21.redactguard.domain.pii

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PiiDefinitionsTest {
    @Test
    fun `built in v1 contract preserves exact category ids`() {
        assertEquals(1, RedactGuardBuiltInPiiDefinitions.VERSION)
        assertEquals(
            listOf("full-name", "email", "telephone", "postal-address", "italian-tax-code", "iban"),
            RedactGuardBuiltInPiiDefinitions.all.map { it.id.value },
        )
        assertTrue(PiiDefinitionSet.create(RedactGuardBuiltInPiiDefinitions.all).isSuccess)
    }

    @Test
    fun `custom definitions are bounded content private and deterministic`() {
        val draft = PiiDefinitionDraft(" Alias prova ", " Dato definito dall'utente ", " Ada Esempio ")
        val created = PiiDefinitionFactory.createCustom(draft, RedactGuardBuiltInPiiDefinitions.all)
        assertTrue(created is PiiDefinitionCreationResult.Created)
        val definition = (created as PiiDefinitionCreationResult.Created).definition
        assertEquals("custom-1", definition.id.value)
        assertEquals("Alias prova", definition.label)
        assertFalse(definition.toString().contains("Ada Esempio"))
        assertFalse(draft.toString().contains("Alias prova"))
    }

    @Test
    fun `unsupported control characters fail custom validation`() {
        val validation =
            PiiDefinitionFactory.validateCustomDraft(
                PiiDefinitionDraft("Alias\u0000", "Definition"),
                RedactGuardBuiltInPiiDefinitions.all,
            )
        assertTrue(PiiDefinitionIssue.UNSUPPORTED_CONTROL_CHARACTER in validation.issues)
    }
}
