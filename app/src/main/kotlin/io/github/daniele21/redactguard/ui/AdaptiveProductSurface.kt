package io.github.daniele21.redactguard.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

internal enum class ProductWindowClass {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

internal fun classifyProductWindow(widthDp: Int): ProductWindowClass =
    when {
        widthDp < 600 -> ProductWindowClass.COMPACT
        widthDp < 840 -> ProductWindowClass.MEDIUM
        else -> ProductWindowClass.EXPANDED
    }

@Composable
internal fun AdaptiveProductSurface(content: @Composable () -> Unit) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().semantics { paneTitle = "RedactGuard" },
        contentAlignment = Alignment.TopCenter,
    ) {
        val windowClass = classifyProductWindow(maxWidth.value.roundToInt())
        val contentWidth =
            when (windowClass) {
                ProductWindowClass.COMPACT -> maxWidth
                ProductWindowClass.MEDIUM -> minOf(maxWidth, 720.dp)
                ProductWindowClass.EXPANDED -> minOf(maxWidth, 840.dp)
            }

        Box(
            modifier = Modifier.fillMaxHeight().width(contentWidth),
            contentAlignment = Alignment.TopCenter,
        ) {
            content()
        }
    }
}
