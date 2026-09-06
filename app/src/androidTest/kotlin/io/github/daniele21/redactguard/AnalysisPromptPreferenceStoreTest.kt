package io.github.daniele21.redactguard

import androidx.test.platform.app.InstrumentationRegistry
import io.github.daniele21.redactguard.domain.analysis.AnalysisPromptPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AnalysisPromptPreferenceStoreTest {
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun resetBefore() {
        AnalysisPromptPreferenceStore(context).reset()
    }

    @After
    fun resetAfter() {
        AnalysisPromptPreferenceStore(context).reset()
    }

    @Test
    fun customPromptPersistsAcrossStoreInstancesAndDefaultClearsCustomization() {
        val first = AnalysisPromptPreferenceStore(context)
        val saved = requireNotNull(first.write("Extract only explicit occurrences."))
        assertTrue(saved.isCustom)

        val restored = AnalysisPromptPreferenceStore(context).read()
        assertTrue(restored.isCustom)
        assertEquals("Extract only explicit occurrences.", restored.prompt)

        val defaulted = requireNotNull(AnalysisPromptPreferenceStore(context).write(AnalysisPromptPolicy.defaultEditablePrompt))
        assertFalse(defaulted.isCustom)
        assertEquals(AnalysisPromptPolicy.defaultEditablePrompt, AnalysisPromptPreferenceStore(context).read().prompt)
    }

    @Test
    fun invalidPromptIsNeverPersisted() {
        val store = AnalysisPromptPreferenceStore(context)
        assertNull(store.write(" \n "))
        assertFalse(store.read().isCustom)
    }
}
