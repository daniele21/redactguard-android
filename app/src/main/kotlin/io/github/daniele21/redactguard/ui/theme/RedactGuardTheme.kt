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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * RedactGuard Android visual-reference palette. Keep raw brand/category values here so product
 * surfaces consume semantic owners instead of scattering reference colors through Compose code.
 */
internal object RedactGuardBrandColors {
    val primaryLight = Color(0xFF004AC6)
    val onPrimaryLight = Color(0xFFFFFFFF)
    val primaryContainerLight = Color(0xFFEAF2FF)
    val onPrimaryContainerLight = Color(0xFF00308A)
    val accentLight = Color(0xFF00B894)

    val surfaceLight = Color(0xFFF7F9FC)
    val surfaceLowestLight = Color(0xFFFFFFFF)
    val surfaceLowLight = Color(0xFFF3F6FA)
    val surfaceContainerLight = Color(0xFFEDF2F7)
    val surfaceHighLight = Color(0xFFE6EDF6)
    val onSurfaceLight = Color(0xFF0D1B2A)
    val onSurfaceVariantLight = Color(0xFF475569)
    val outlineLight = Color(0xFF64748B)
    val outlineVariantLight = Color(0xFFD8E0EA)

    val primaryDark = Color(0xFFADC6FF)
    val onPrimaryDark = Color(0xFF002E7A)
    val primaryContainerDark = Color(0xFF173B7A)
    val onPrimaryContainerDark = Color(0xFFD9E2FF)
    val accentDark = Color(0xFF34D399)
    val surfaceDark = Color(0xFF0F172A)
    val surfaceLowestDark = Color(0xFF020617)
    val surfaceLowDark = Color(0xFF172033)
    val surfaceHighDark = Color(0xFF29364A)
    val onSurfaceDark = Color(0xFFF1F5F9)
    val onSurfaceVariantDark = Color(0xFFCBD5E1)
    val outlineDark = Color(0xFF94A3B8)
    val outlineVariantDark = Color(0xFF334155)

    val successLight = Color(0xFF00A884)
    val successDark = Color(0xFF34D399)
    val warningLight = Color(0xFFD97706)
    val warningDark = Color(0xFFFBBF24)
    val errorLight = Color(0xFFBA1A1A)
    val errorDark = Color(0xFFFFB4AB)

    val piiIdentityLight = Color(0xFF2563EB)
    val piiContactLight = Color(0xFF00B894)
    val piiHealthLight = Color(0xFFE53935)
    val piiFinancialLight = Color(0xFFF59E0B)
    val piiLocationLight = Color(0xFF8B5CF6)
    val piiOtherLight = Color(0xFF64748B)

    val piiIdentityDark = Color(0xFF60A5FA)
    val piiContactDark = Color(0xFF34D399)
    val piiHealthDark = Color(0xFFF87171)
    val piiFinancialDark = Color(0xFFFBBF24)
    val piiLocationDark = Color(0xFFA78BFA)
    val piiOtherDark = Color(0xFF94A3B8)

    val piiDateLight = piiFinancialLight
    val piiLabLight = piiHealthLight
    val piiMeasurementLight = piiHealthLight
    val piiLifestyleLight = piiOtherLight
    val piiSecretLight = piiOtherLight
    val piiCustomLight = piiOtherLight
    val piiDateDark = piiFinancialDark
    val piiLabDark = piiHealthDark
    val piiMeasurementDark = piiHealthDark
    val piiLifestyleDark = piiOtherDark
    val piiSecretDark = piiOtherDark
    val piiCustomDark = piiOtherDark
}

private val LightColorScheme =
    lightColorScheme(
        primary = RedactGuardBrandColors.primaryLight,
        onPrimary = RedactGuardBrandColors.onPrimaryLight,
        primaryContainer = RedactGuardBrandColors.primaryContainerLight,
        onPrimaryContainer = RedactGuardBrandColors.onPrimaryContainerLight,
        secondary = RedactGuardBrandColors.accentLight,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFDDF8F1),
        onSecondaryContainer = Color(0xFF005B49),
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
        secondary = RedactGuardBrandColors.accentDark,
        onSecondary = Color(0xFF00382D),
        secondaryContainer = Color(0xFF064E3B),
        onSecondaryContainer = Color(0xFFA7F3D0),
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

private val brandFamily = FontFamily.SansSerif

private val RedactGuardTypography =
    Typography(
        displayLarge = brandTextStyle(28, 36, FontWeight.SemiBold),
        headlineLarge = brandTextStyle(22, 30, FontWeight.SemiBold),
        headlineMedium = brandTextStyle(20, 28, FontWeight.SemiBold),
        headlineSmall = brandTextStyle(18, 24, FontWeight.SemiBold),
        titleLarge = brandTextStyle(18, 26, FontWeight.SemiBold),
        titleMedium = brandTextStyle(16, 24, FontWeight.Medium),
        titleSmall = brandTextStyle(14, 20, FontWeight.Medium),
        bodyLarge = brandTextStyle(14, 20),
        bodyMedium = brandTextStyle(14, 20),
        bodySmall = brandTextStyle(12, 16),
        labelLarge = brandTextStyle(14, 20, FontWeight.Medium),
        labelMedium = brandTextStyle(12, 16, FontWeight.Medium),
        labelSmall = brandTextStyle(11, 16, FontWeight.Medium),
    )

private fun brandTextStyle(
    sizeSp: Int,
    lineHeightSp: Int,
    weight: FontWeight = FontWeight.Normal,
): TextStyle =
    TextStyle(
        fontFamily = brandFamily,
        fontWeight = weight,
        fontSize = sizeSp.sp,
        lineHeight = lineHeightSp.sp,
    )

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
