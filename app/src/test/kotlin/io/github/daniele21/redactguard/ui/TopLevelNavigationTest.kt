package io.github.daniele21.redactguard.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class TopLevelNavigationTest {
    @Test
    fun `compact windows use bottom navigation`() {
        assertEquals(
            TopLevelNavigationMode.BOTTOM_BAR,
            topLevelNavigationMode(ProductWindowClass.COMPACT),
        )
    }

    @Test
    fun `medium and expanded windows use navigation rail`() {
        assertEquals(
            TopLevelNavigationMode.RAIL,
            topLevelNavigationMode(ProductWindowClass.MEDIUM),
        )
        assertEquals(
            TopLevelNavigationMode.RAIL,
            topLevelNavigationMode(ProductWindowClass.EXPANDED),
        )
    }

    @Test
    fun `top level destination inventory stays bounded to product contract`() {
        assertEquals(
            listOf("Analizza", "AI locale", "Impostazioni"),
            RedactGuardTopLevelDestination.entries.map(RedactGuardTopLevelDestination::label),
        )
    }
}
