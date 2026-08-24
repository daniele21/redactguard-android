package io.github.daniele21.redactguard.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveProductSurfaceTest {
    @Test
    fun `window classification follows compact medium expanded boundaries`() {
        assertEquals(ProductWindowClass.COMPACT, classifyProductWindow(599))
        assertEquals(ProductWindowClass.MEDIUM, classifyProductWindow(600))
        assertEquals(ProductWindowClass.MEDIUM, classifyProductWindow(839))
        assertEquals(ProductWindowClass.EXPANDED, classifyProductWindow(840))
    }
}
