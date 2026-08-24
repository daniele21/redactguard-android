@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package io.github.daniele21.redactguard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val LightColorScheme =
    lightColorScheme(
        primary = Color(0xFF166A5A),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD2F4E9),
        onPrimaryContainer = Color(0xFF002019),
        secondary = Color(0xFF50655F),
        onSecondary = Color.White,
        surface = Color(0xFFF7F9F8),
        onSurface = Color(0xFF16201D),
        surfaceVariant = Color(0xFFE4EAE7),
        onSurfaceVariant = Color(0xFF56625E),
        outline = Color(0xFFCBD5D1),
        outlineVariant = Color(0xFFE0E7E4),
        error = Color(0xFFB3261E),
        onError = Color.White,
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = Color(0xFF78D5C0),
        onPrimary = Color(0xFF00382D),
        primaryContainer = Color(0xFF005143),
        onPrimaryContainer = Color(0xFFD2F4E9),
        secondary = Color(0xFFB6CCC5),
        onSecondary = Color(0xFF213630),
        surface = Color(0xFF0F1513),
        onSurface = Color(0xFFE6EFEB),
        surfaceVariant = Color(0xFF25302C),
        onSurfaceVariant = Color(0xFFAEBAB5),
        outline = Color(0xFF3D4A45),
        outlineVariant = Color(0xFF29332F),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
    )

private val RedactGuardTypography =
    Typography().let { base ->
        base.copy(
            headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
            titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium),
            labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Medium),
        )
    }

private val RedactGuardShapes =
    Shapes(
        extraSmall =
            androidx.compose.foundation.shape
                .RoundedCornerShape(6.dp),
        small =
            androidx.compose.foundation.shape
                .RoundedCornerShape(8.dp),
        medium =
            androidx.compose.foundation.shape
                .RoundedCornerShape(12.dp),
        large =
            androidx.compose.foundation.shape
                .RoundedCornerShape(16.dp),
        extraLarge =
            androidx.compose.foundation.shape
                .RoundedCornerShape(24.dp),
    )

internal object RedactGuardSemanticColors {
    val successLight = Color(0xFF1B6E50)
    val successDark = Color(0xFF70D3A7)
    val warningLight = Color(0xFF8A5A00)
    val warningDark = Color(0xFFF5C15C)
    val focusLight = Color(0xFF005AC1)
    val focusDark = Color(0xFF9FC9FF)
}

@Composable
internal fun RedactGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = RedactGuardTypography,
        shapes = RedactGuardShapes,
        content = content,
    )
}
