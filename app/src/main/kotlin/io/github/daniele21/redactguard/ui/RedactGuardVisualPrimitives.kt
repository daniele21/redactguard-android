@file:Suppress("FunctionName", "ktlint:standard:function-naming")

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
    val borderColor =
        if (emphasized) {
            accent.copy(alpha = 0.58f)
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }

    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(if (emphasized) 1.5.dp else 1.dp, borderColor),
        shadowElevation = if (emphasized) 1.dp else 0.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(RedactGuardSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(RedactGuardSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReferenceIconTile(iconRes = iconRes, accent = accent)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xxs),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "›",
                style = MaterialTheme.typography.headlineSmall,
                color =
                    if (emphasized) {
                        accent
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
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
        modifier = modifier.size(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
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
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
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
                    modifier =
                        Modifier.padding(
                            horizontal = RedactGuardSpacing.xs,
                            vertical = RedactGuardSpacing.xxs,
                        ),
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
    val alpha = if (isSystemInDarkTheme()) 0.18f else 0.10f

    Surface(
        color = accent.copy(alpha = alpha),
        contentColor = accent,
        shape = MaterialTheme.shapes.small,
        modifier = modifier,
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = RedactGuardSpacing.xs,
                    vertical = RedactGuardSpacing.xxs,
                ),
            horizontalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = accent,
                shape = CircleShape,
                modifier = Modifier.size(7.dp),
            ) {}
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

internal enum class ReferencePhaseState {
    DONE,
    ACTIVE,
    PENDING,
}

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
            color = phaseColor(state, accent),
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
                    phaseMarker(state),
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
            fontWeight =
                if (state == ReferencePhaseState.ACTIVE) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Normal
                },
        )
    }
}

@Composable
private fun phaseColor(
    state: ReferencePhaseState,
    accent: Color,
): Color =
    when (state) {
        ReferencePhaseState.DONE -> referenceSuccessColor()
        ReferencePhaseState.ACTIVE -> accent
        ReferencePhaseState.PENDING -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

private fun phaseMarker(state: ReferencePhaseState): String =
    when (state) {
        ReferencePhaseState.DONE -> "✓"
        ReferencePhaseState.ACTIVE -> "•"
        ReferencePhaseState.PENDING -> ""
    }

@Composable
internal fun ReferenceSelectionBadge(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        color = if (selected) accent else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor =
            if (selected) {
                Color.White
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        shape = CircleShape,
        border =
            if (selected) {
                null
            } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            },
        modifier =
            modifier
                .size(24.dp)
                .semantics {
                    contentDescription = if (selected) "Selezionato" else "Non selezionato"
                },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                if (selected) "✓" else "",
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
internal fun piiAccent(key: String): Color {
    val dark = isSystemInDarkTheme()
    val normalized = key.lowercase()
    return when {
        isHealthKey(normalized) -> {
            if (dark) {
                RedactGuardBrandColors.piiHealthDark
            } else {
                RedactGuardBrandColors.piiHealthLight
            }
        }

        isFinancialKey(normalized) -> {
            if (dark) {
                RedactGuardBrandColors.piiFinancialDark
            } else {
                RedactGuardBrandColors.piiFinancialLight
            }
        }

        isLocationKey(normalized) -> {
            if (dark) {
                RedactGuardBrandColors.piiLocationDark
            } else {
                RedactGuardBrandColors.piiLocationLight
            }
        }

        isContactKey(normalized) -> {
            if (dark) {
                RedactGuardBrandColors.piiContactDark
            } else {
                RedactGuardBrandColors.piiContactLight
            }
        }

        isIdentityKey(normalized) -> {
            if (dark) {
                RedactGuardBrandColors.piiIdentityDark
            } else {
                RedactGuardBrandColors.piiIdentityLight
            }
        }

        normalized.contains("date") -> {
            if (dark) {
                RedactGuardBrandColors.piiFinancialDark
            } else {
                RedactGuardBrandColors.piiFinancialLight
            }
        }

        else -> {
            if (dark) {
                RedactGuardBrandColors.piiOtherDark
            } else {
                RedactGuardBrandColors.piiOtherLight
            }
        }
    }
}

private fun isHealthKey(key: String): Boolean =
    key.contains("health") ||
        key.contains("salute") ||
        key.contains("sanitar") ||
        key.contains("trattament") ||
        key.contains("lab") ||
        key.contains("measurement")

private fun isFinancialKey(key: String): Boolean =
    key.contains("iban") ||
        key.contains("account") ||
        key.contains("finanz")

private fun isLocationKey(key: String): Boolean =
    key.contains("postal") ||
        key.contains("address") ||
        key.contains("location") ||
        key.contains("luog") ||
        key.contains("indirizz")

private fun isContactKey(key: String): Boolean =
    key.contains("email") ||
        key.contains("telephone") ||
        key.contains("phone") ||
        key.contains("contact") ||
        key.contains("contatt") ||
        key.contains("url")

private fun isIdentityKey(key: String): Boolean =
    key.contains("name") ||
        key.contains("identity") ||
        key.contains("identit") ||
        key.contains("tax") ||
        key.contains("demographic")

@Composable
internal fun referenceSuccessColor(): Color =
    if (isSystemInDarkTheme()) {
        RedactGuardBrandColors.successDark
    } else {
        RedactGuardBrandColors.successLight
    }
