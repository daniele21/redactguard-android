package io.github.daniele21.redactguard.ui.theme

import androidx.compose.ui.unit.dp

internal object RedactGuardSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
}

internal object RedactGuardMotion {
    const val INSTANT_MILLIS = 0
    const val FAST_MILLIS = 120
    const val STANDARD_MILLIS = 200
    const val LARGE_MILLIS = 300
}

internal object RedactGuardDimensions {
    val compactContentMaxWidth = 640.dp
    val expandedContentMaxWidth = 960.dp
    val minimumTouchTarget = 48.dp
}
