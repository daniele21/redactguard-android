package io.github.daniele21.redactguard.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.daniele21.redactguard.ui.theme.RedactGuardBrandColors
import io.github.daniele21.redactguard.ui.theme.RedactGuardSpacing

@Composable
internal fun ReferenceActionCard(
    title: String,
    subtitle: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border =
            BorderStroke(
                if (emphasized) 1.5.dp else 1.dp,
                if (emphasized) accent.copy(alpha = 0.68f) else MaterialTheme.colorScheme.outlineVariant,
            ),
        shadowElevation = if (emphasized) 2.dp else 0.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(RedactGuardSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(RedactGuardSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReferenceIconTile(iconRes = iconRes, accent = accent)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xxs),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "›",
                style = MaterialTheme.typography.headlineSmall,
                color = if (emphasized) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun ReferenceIconTile(
    @DrawableRes iconRes: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = accent,
        contentColor = Color.White,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.size(48.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
internal fun ReferenceSectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (actionLabel != null && onAction != null) {
            Surface(
                onClick = onAction,
                color = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = RedactGuardSpacing.xs, vertical = RedactGuardSpacing.xxs),
                )
            }
        }
    }
}

@Composable
internal fun ReferenceSemanticTag(
    label: String,
    semanticKey: String = label,
    modifier: Modifier = Modifier,
) {
    val accent = piiAccent(semanticKey)
    Surface(
        color = accent.copy(alpha = if (isSystemInDarkTheme()) 0.18f else 0.10f),
        contentColor = accent,
        shape = MaterialTheme.shapes.small,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = RedactGuardSpacing.xs, vertical = RedactGuardSpacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(color = accent, shape = CircleShape, modifier = Modifier.size(7.dp)) {}
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

internal enum class ReferencePhaseState { DONE, ACTIVE, PENDING }

@Composable
internal fun ReferencePhaseRow(
    label: String,
    state: ReferencePhaseState,
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        horizontalArrangement = Arrangement.spacedBy(RedactGuardSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Surface(
            color =
                when (state) {
                    ReferencePhaseState.DONE -> referenceSuccessColor()
                    ReferencePhaseState.ACTIVE -> accent
                    ReferencePhaseState.PENDING -> MaterialTheme.colorScheme.surfaceContainerHigh
                },
            contentColor = Color.White,
            shape = CircleShape,
            border =
                if (state == ReferencePhaseState.PENDING) {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                } else {
                    null
                },
            modifier = Modifier.size(22.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    when (state) {
                        ReferencePhaseState.DONE -> "✓"
                        ReferencePhaseState.ACTIVE -> "•"
                        ReferencePhaseState.PENDING -> ""
                    },
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color =
                if (state == ReferencePhaseState.PENDING) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            fontWeight = if (state == ReferencePhaseState.ACTIVE) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
internal fun ReferenceSelectionBadge(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        color = if (selected) accent else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = CircleShape,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier =
            modifier.size(24.dp).semantics {
                contentDescription = if (selected) "Selezionato" else "Non selezionato"
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(if (selected) "✓" else "", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
internal fun piiAccent(key: String): Color {
    val dark = isSystemInDarkTheme()
    val normalized = key.lowercase()
    return when {
        normalized.contains("health") || normalized.contains("salute") || normalized.contains("sanitar") ||
            normalized.contains("trattament") || normalized.contains("lab") || normalized.contains("measurement") ->
            if (dark) RedactGuardBrandColors.piiHealthDark else RedactGuardBrandColors.piiHealthLight

        normalized.contains("iban") || normalized.contains("account") || normalized.contains("finanz") ->
            if (dark) RedactGuardBrandColors.piiFinancialDark else RedactGuardBrandColors.piiFinancialLight

        normalized.contains("postal") || normalized.contains("address") || normalized.contains("location") ||
            normalized.contains("luog") || normalized.contains("indirizz") ->
            if (dark) RedactGuardBrandColors.piiLocationDark else RedactGuardBrandColors.piiLocationLight

        normalized.contains("email") || normalized.contains("telephone") || normalized.contains("phone") ||
            normalized.contains("contact") || normalized.contains("contatt") || normalized.contains("url") ->
            if (dark) RedactGuardBrandColors.piiContactDark else RedactGuardBrandColors.piiContactLight

        normalized.contains("name") || normalized.contains("identity") || normalized.contains("identit") ||
            normalized.contains("tax") || normalized.contains("demographic") ->
            if (dark) RedactGuardBrandColors.piiIdentityDark else RedactGuardBrandColors.piiIdentityLight

        normalized.contains("date") ->
            if (dark) RedactGuardBrandColors.piiFinancialDark else RedactGuardBrandColors.piiFinancialLight

        else -> if (dark) RedactGuardBrandColors.piiOtherDark else RedactGuardBrandColors.piiOtherLight
    }
}

@Composable
internal fun referenceSuccessColor(): Color =
    if (isSystemInDarkTheme()) RedactGuardBrandColors.successDark else RedactGuardBrandColors.successLight
