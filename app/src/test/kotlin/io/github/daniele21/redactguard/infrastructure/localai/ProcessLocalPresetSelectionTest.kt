package io.github.daniele21.redactguard.infrastructure.localai

import io.github.daniele21.localllm.contracts.ConsumerPublishedPreset
import io.github.daniele21.localllm.contracts.InferencePresetId
import io.github.daniele21.localllm.contracts.InferencePresetRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessLocalPresetSelectionTest {
    @Test
    fun `single published preset is selected even when host does not mark it default`() {
        val selection = ProcessLocalPresetSelection()

        val resolved = selection.resolve(listOf(published(DEFAULT_PRESET, isDefault = false)), null)

        assertEquals(DEFAULT_PRESET, resolved)
        assertEquals(DEFAULT_PRESET, selection.state.value.selectedPreset)
        assertEquals(1, selection.state.value.options.size)
        assertFalse(selection.state.value.staleSelectionReplaced)
    }

    @Test
    fun `in memory selection is retained while exact preset reference remains advertised`() {
        val selection = ProcessLocalPresetSelection()
        val published =
            listOf(
                published(DEFAULT_PRESET, isDefault = true),
                published(QUALITY_PRESET, isDefault = false),
            )
        selection.resolve(published, null)
        assertTrue(selection.select(QUALITY_PRESET))

        val resolved = selection.resolve(published, null)

        assertEquals(QUALITY_PRESET, resolved)
        assertEquals(QUALITY_PRESET, selection.state.value.selectedPreset)
        assertFalse(selection.state.value.staleSelectionReplaced)
    }

    @Test
    fun `withdrawn in memory selection refreshes to current host default`() {
        val selection = ProcessLocalPresetSelection()
        val initial =
            listOf(
                published(DEFAULT_PRESET, isDefault = true),
                published(QUALITY_PRESET, isDefault = false),
            )
        selection.resolve(initial, null)
        assertTrue(selection.select(QUALITY_PRESET))

        val resolved = selection.resolve(listOf(published(DEFAULT_PRESET, isDefault = true)), null)

        assertEquals(DEFAULT_PRESET, resolved)
        assertEquals(DEFAULT_PRESET, selection.state.value.selectedPreset)
        assertTrue(selection.state.value.staleSelectionReplaced)
    }

    @Test
    fun `explicit non advertised preset remains fail closed`() {
        val selection = ProcessLocalPresetSelection()

        val resolved = selection.resolve(listOf(published(DEFAULT_PRESET, isDefault = true)), QUALITY_PRESET)

        assertNull(resolved)
        assertNull(selection.state.value.selectedPreset)
    }

    private fun published(
        preset: InferencePresetRef,
        isDefault: Boolean,
    ): ConsumerPublishedPreset =
        ConsumerPublishedPreset(
            preset = preset,
            displayName = "Synthetic preset",
            description = "Synthetic consumer-safe preset",
            isDefault = isDefault,
        )

    private companion object {
        val DEFAULT_PRESET = InferencePresetRef(InferencePresetId("balanced"), 1)
        val QUALITY_PRESET = InferencePresetRef(InferencePresetId("quality"), 2)
    }
}
