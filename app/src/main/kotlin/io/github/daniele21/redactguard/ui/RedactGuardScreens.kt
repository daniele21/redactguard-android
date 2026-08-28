@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package io.github.daniele21.redactguard.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.daniele21.redactguard.R
import io.github.daniele21.redactguard.ui.theme.RedactGuardSpacing

@Composable
internal fun RedactGuardScaffold(
    step: String,
    connection: ConnectionBadgeModel,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(
                        horizontal = RedactGuardSpacing.md,
                        vertical = RedactGuardSpacing.xs,
                    ),
            verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.sm),
        ) {
            ProductTopBar(step = step)
            if (!connection.analysisReady) {
                connection.explanation?.let { explanation ->
                    ConnectionExplanation(model = connection, explanation = explanation)
                }
            }
            content()
        }
    }
}

@Composable
private fun ProductTopBar(step: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = RedactGuardSpacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.redactguard_mark),
            contentDescription = "RedactGuard",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(28.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                "RedactGuard",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (step != "Documento") {
                Text(
                    step,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun ConnectionBadge(model: ConnectionBadgeModel) {
    val ready = model.analysisReady
    val displayLabel = if (ready) "AI locale pronta" else model.label
    val containerColor =
        if (ready) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
        } else {
            connectionContainerColor(model.tone)
        }
    val contentColor =
        if (ready) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            connectionContentColor(model.tone)
        }
    val borderColor =
        if (ready) {
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)
        } else {
            connectionContentColor(model.tone).copy(alpha = 0.16f)
        }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, borderColor),
        modifier =
            Modifier.semantics {
                contentDescription = "Stato AI locale: ${model.label}"
            },
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
                color =
                    if (ready) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        connectionContentColor(model.tone)
                    },
                shape = CircleShape,
                modifier = Modifier.size(7.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {}
            }
            Text(
                displayLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ConnectionExplanation(
    model: ConnectionBadgeModel,
    explanation: String,
) {
    Surface(
        color = connectionContainerColor(model.tone),
        contentColor = connectionContentColor(model.tone),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, connectionContentColor(model.tone).copy(alpha = 0.16f)),
        modifier =
            Modifier.fillMaxWidth().semantics {
                contentDescription = "Dettaglio stato AI locale: $explanation"
            },
    ) {
        Text(
            explanation,
            style = MaterialTheme.typography.bodySmall,
            modifier =
                Modifier.padding(
                    horizontal = RedactGuardSpacing.sm,
                    vertical = RedactGuardSpacing.xs,
                ),
        )
    }
}

@Composable
private fun connectionContainerColor(tone: StatusTone): Color =
    when (tone) {
        StatusTone.READY -> MaterialTheme.colorScheme.primaryContainer
        StatusTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceContainer
        StatusTone.REVIEW -> MaterialTheme.colorScheme.secondaryContainer
        StatusTone.ERROR -> MaterialTheme.colorScheme.errorContainer
    }

@Composable
private fun connectionContentColor(tone: StatusTone): Color =
    when (tone) {
        StatusTone.READY -> MaterialTheme.colorScheme.onPrimaryContainer
        StatusTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
        StatusTone.REVIEW -> MaterialTheme.colorScheme.onSecondaryContainer
        StatusTone.ERROR -> MaterialTheme.colorScheme.onErrorContainer
    }

@Composable
internal fun ProductPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 0.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(RedactGuardSpacing.md),
            verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.sm),
            content = content,
        )
    }
}
