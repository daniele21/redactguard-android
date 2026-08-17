package io.github.daniele21.redactguard.domain.pii

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefinitionSelectionTest {
    @Test
    fun `built ins start unselected and toggle deterministically`() {
        val controller = DefinitionSelectionController()
        val email = PiiTypeId.parse("email")

        assertTrue(controller.state.selectedIds.isEmpty())
        controller.toggle(email)
        assertEquals(setOf(email), controller.state.selectedIds)
        controller.toggle(email)
        assertTrue(controller.state.selectedIds.isEmpty())
    }

    @Test
    fun `valid custom definition is added and selected`() {
        val controller = DefinitionSelectionController()

        val result =
            controller.addCustom(
                PiiDefinitionDraft(
                    label = "Matricola dipendente",
                    definition = "Identificativo interno assegnato a una persona dipendente.",
                    example = "ABC-123",
                ),
            )

        val created = result as PiiDefinitionCreationResult.Created
        assertTrue(created.definition.id in controller.state.selectedIds)
        assertEquals(PiiDefinitionSource.CUSTOM, created.definition.source)
        assertTrue(created.definition in controller.state.definitions)
    }

    @Test
    fun `invalid custom definition does not mutate selection`() {
        val controller = DefinitionSelectionController()
        val before = controller.state

        val result = controller.addCustom(PiiDefinitionDraft(label = "", definition = ""))

        assertTrue(result is PiiDefinitionCreationResult.Invalid)
        assertEquals(before, controller.state)
    }

    @Test
    fun `reset removes transient custom definitions and selections`() {
        val controller = DefinitionSelectionController()
        controller.toggle(PiiTypeId.parse("email"))
        controller.addCustom(PiiDefinitionDraft("Badge", "Identificativo personale del badge"))

        val reset = controller.reset()

        assertEquals(RedactGuardBuiltInPiiDefinitions.all, reset.definitions)
        assertTrue(reset.selectedIds.isEmpty())
    }
}
