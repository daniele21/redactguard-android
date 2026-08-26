package io.github.daniele21.redactguard.ui

import io.github.daniele21.redactguard.domain.pii.RedactGuardPiiProfiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionProfileProjectionTest {
    @Test
    fun `exact general profile is projected as selected`() {
        val general = requireNotNull(RedactGuardPiiProfiles.all.firstOrNull { it.label == "Generale" })
        val definitions =
            RedactGuardPiiProfiles.all
                .flatMap { it.typeIds }
                .distinct()
                .map { id -> DefinitionChoice(id.value, id.value, selected = id in general.typeIds) }

        val projected = ProtectionProfileProjector.project(definitions)

        assertEquals("GENERAL", projected.single(ProtectionProfileChoice::selected).id)
    }

    @Test
    fun `profile selection returns only categories whose state must change`() {
        val general = requireNotNull(RedactGuardPiiProfiles.byId.values.firstOrNull { it.label == "Generale" })
        val definitions =
            general.typeIds.mapIndexed { index, id ->
                DefinitionChoice(id.value, id.value, selected = index != 0)
            }

        val toggles = ProtectionProfileSelection.togglesFor("GENERAL", definitions)

        assertEquals(listOf(general.typeIds.first().value), toggles)
    }

    @Test
    fun `unknown profile is a no-op`() {
        val toggles =
            ProtectionProfileSelection.togglesFor(
                profileId = "UNKNOWN",
                definitions = listOf(DefinitionChoice("email", "Email", selected = true)),
            )

        assertTrue(toggles.isEmpty())
    }
}
