@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package io.github.daniele21.redactguard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * RedactGuard brand palette shared semantically with the desktop product. Keep raw brand values here
 * rather than scattering them across product surfaces.
 */
internal object RedactGuardBrandColors {
    val primaryLight = Color(0xFF004AC6)
    val onPrimaryLight = Color(0xFFFFFFFF)
    val primaryContainerLight = Color(0xFFE5F0FF)
    val onPrimaryContainerLight = Color(0xFF00308A)

    val surfaceLight = Color(0xFFF9F9FF)
    val surfaceLowestLight = Color(0xFFFFFFFF)
    val surfaceLowLight = Color(0xFFF0F3FF)
    val surfaceContainerLight = Color(0xFFE7EEFE)
    val surfaceHighLight = Color(0xFFE2E8F8)
    val onSurfaceLight = Color(0xFF151C27)
    val onSurfaceVariantLight = Color(0xFF434655)
    val outlineLight = Color(0xFF737686)
    val outlineVariantLight = Color(0xFFC3C6D7)

    val primaryDark = Color(0xFFADC6FF)
    val onPrimaryDark = Color(0xFF002E7A)
    val primaryContainerDark = Color(0xFF1E40AF)
    val onPrimaryContainerDark = Color(0xFFD9E2FF)
    val surfaceDark = Color(0xFF0F172A)
    val surfaceLowestDark = Color(0xFF020617)
    val surfaceLowDark = Color(0xFF1E293B)
    val surfaceHighDark = Color(0xFF334155)
    val onSurfaceDark = Color(0xFFF1F5F9)
    val onSurfaceVariantDark = Color(0xFFCBD5E1)
    val outlineDark = Color(0xFF64748B)
    val outlineVariantDark = Color(0xFF334155)

    val successLight = Color(0xFF059669)
    val successDark = Color(0xFF34D399)
    val warningLight = Color(0xFFD97706)
    val warningDark = Color(0xFFFBBF24)
    val errorLight = Color(0xFFBA1A1A)
    val errorDark = Color(0xFFFFB4AB)

    val piiIdentityLight = Color(0xFF3B82F6)
    val piiContactLight = Color(0xFF8B5CF6)
    val piiLocationLight = Color(0xFF06B6D4)
    val piiDateLight = Color(0xFFF59E0B)
    val piiFinancialLight = Color(0xFF059669)
    val piiHealthLight = Color(0xFFEF4444)
    val piiLabLight = Color(0xFFF97316)
    val piiMeasurementLight = Color(0xFF14B8A6)
    val piiLifestyleLight = Color(0xFF0EA5E9)
    val piiSecretLight = Color(0xFF6B7280)
    val piiCustomLight = Color(0xFFEC4899)

    val piiIdentityDark = Color(0xFF60A5FA)
    val piiContactDark = Color(0xFFA78BFA)
    val piiLocationDark = Color(0xFF22D3EE)
    val piiDateDark = Color(0xFFFBBF24)
    val piiFinancialDark = Color(0xFF34D399)
    val piiHealthDark = Color(0xFFF87171)
    val piiLabDark = Color(0xFFFB923C)
    val piiMeasurementDark = Color(0xFF2DD4BF)
    val piiLifestyleDark = Color(0xFF38BDF8)
    val piiSecretDark = Color(0xFF94A3B8)
    val piiCustomDark = Color(0xFFF472B6)
}

private val LightColorScheme =
    lightColorScheme(
        primary = RedactGuardBrandColors.primaryLight,
        onPrimary = RedactGuardBrandColors.onPrimaryLight,
        primaryContainer = RedactGuardBrandColors.primaryContainerLight,
        onPrimaryContainer = RedactGuardBrandColors.onPrimaryContainerLight,
        secondary = Color(0xFF475569),
        onSecondary = Color.White,
        secondaryContainer = RedactGuardBrandColors.surfaceContainerLight,
        onSecondaryContainer = RedactGuardBrandColors.onSurfaceLight,
        background = RedactGuardBrandColors.surfaceLight,
        onBackground = RedactGuardBrandColors.onSurfaceLight,
        surface = RedactGuardBrandColors.surfaceLight,
        onSurface = RedactGuardBrandColors.onSurfaceLight,
        surfaceVariant = RedactGuardBrandColors.surfaceLowLight,
        onSurfaceVariant = RedactGuardBrandColors.onSurfaceVariantLight,
        surfaceContainerLowest = RedactGuardBrandColors.surfaceLowestLight,
        surfaceContainerLow = RedactGuardBrandColors.surfaceLowLight,
        surfaceContainer = RedactGuardBrandColors.surfaceContainerLight,
        surfaceContainerHigh = RedactGuardBrandColors.surfaceHighLight,
        outline = RedactGuardBrandColors.outlineLight,
        outlineVariant = RedactGuardBrandColors.outlineVariantLight,
        error = RedactGuardBrandColors.errorLight,
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = RedactGuardBrandColors.primaryDark,
        onPrimary = RedactGuardBrandColors.onPrimaryDark,
        primaryContainer = RedactGuardBrandColors.primaryContainerDark,
        onPrimaryContainer = RedactGuardBrandColors.onPrimaryContainerDark,
        secondary = Color(0xFFCBD5E1),
        onSecondary = Color(0xFF1E293B),
        secondaryContainer = RedactGuardBrandColors.surfaceLowDark,
        onSecondaryContainer = RedactGuardBrandColors.onSurfaceDark,
        background = RedactGuardBrandColors.surfaceLowestDark,
        onBackground = RedactGuardBrandColors.onSurfaceDark,
        surface = RedactGuardBrandColors.surfaceDark,
        onSurface = RedactGuardBrandColors.onSurfaceDark,
        surfaceVariant = RedactGuardBrandColors.surfaceLowDark,
        onSurfaceVariant = RedactGuardBrandColors.onSurfaceVariantDark,
        surfaceContainerLowest = RedactGuardBrandColors.surfaceLowestDark,
        surfaceContainerLow = RedactGuardBrandColors.surfaceDark,
        surfaceContainer = RedactGuardBrandColors.surfaceLowDark,
        surfaceContainerHigh = RedactGuardBrandColors.surfaceHighDark,
        outline = RedactGuardBrandColors.outlineDark,
        outlineVariant = RedactGuardBrandColors.outlineVariantDark,
        error = RedactGuardBrandColors.errorDark,
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
    )

private val RedactGuardTypography =
    Typography().let { base ->
        val brandFamily = FontFamily.SansSerif
        base.copy(
            displayLarge = base.displayLarge.copy(fontFamily = brandFamily, fontWeight = FontWeight.SemiBold),
            headlineLarge = base.headlineLarge.copy(fontFamily = brandFamily, fontWeight = FontWeight.SemiBold),
            headlineMedium = base.headlineMedium.copy(fontFamily = brandFamily, fontWeight = FontWeight.SemiBold),
            headlineSmall = base.headlineSmall.copy(fontFamily = brandFamily, fontWeight = FontWeight.SemiBold),
            titleLarge = base.titleLarge.copy(fontFamily = brandFamily, fontWeight = FontWeight.SemiBold),
            titleMedium = base.titleMedium.copy(fontFamily = brandFamily, fontWeight = FontWeight.Medium),
            titleSmall = base.titleSmall.copy(fontFamily = brandFamily, fontWeight = FontWeight.Medium),
            bodyLarge = base.bodyLarge.copy(fontFamily = brandFamily),
            bodyMedium = base.bodyMedium.copy(fontFamily = brandFamily),
            bodySmall = base.bodySmall.copy(fontFamily = brandFamily),
            labelLarge = base.labelLarge.copy(fontFamily = brandFamily, fontWeight = FontWeight.Medium),
            labelMedium = base.labelMedium.copy(fontFamily = brandFamily, fontWeight = FontWeight.Medium),
            labelSmall = base.labelSmall.copy(fontFamily = brandFamily, fontWeight = FontWeight.Medium),
        )
    }

private val RedactGuardShapes =
    Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(24.dp),
    )

internal object RedactGuardSemanticColors {
    val successLight = RedactGuardBrandColors.successLight
    val successDark = RedactGuardBrandColors.successDark
    val warningLight = RedactGuardBrandColors.warningLight
    val warningDark = RedactGuardBrandColors.warningDark
    val focusLight = RedactGuardBrandColors.primaryLight
    val focusDark = RedactGuardBrandColors.primaryDark
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
