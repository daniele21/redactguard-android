@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package io.github.daniele21.redactguard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import io.github.daniele21.redactguard.ui.theme.RedactGuardDimensions
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
    AdaptiveProductSurfaceForWindow { content() }
}

@Composable
internal fun AdaptiveProductSurfaceForWindow(content: @Composable (ProductWindowClass) -> Unit) {
    BoxWithConstraints(
        modifier =
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).semantics {
                paneTitle = "RedactGuard"
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        val windowClass = classifyProductWindow(maxWidth.value.roundToInt())
        val contentWidth =
            when (windowClass) {
                ProductWindowClass.COMPACT -> minOf(maxWidth, RedactGuardDimensions.compactContentMaxWidth)
                ProductWindowClass.MEDIUM -> minOf(maxWidth, RedactGuardDimensions.mediumContentMaxWidth)
                ProductWindowClass.EXPANDED -> minOf(maxWidth, RedactGuardDimensions.expandedContentMaxWidth)
            }

        Box(
            modifier = Modifier.fillMaxHeight().width(contentWidth),
            contentAlignment = Alignment.TopCenter,
        ) {
            content(windowClass)
        }
    }
}
