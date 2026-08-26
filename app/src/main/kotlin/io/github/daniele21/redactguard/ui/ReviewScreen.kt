@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package io.github.daniele21.redactguard.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.daniele21.redactguard.ui.theme.RedactGuardSpacing

@Composable
internal fun ReviewScreen(
    connection: ConnectionBadgeModel,
    finding: ReviewFindingModel,
    position: Int,
    total: Int,
    onRevealToggle: () -> Unit,
    onRedact: () -> Unit,
    onIgnore: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onExport: () -> Unit,
    exportEnabled: Boolean,
    windowClass: ProductWindowClass = ProductWindowClass.COMPACT,
) {
    val useCompactComposition =
        windowClass == ProductWindowClass.COMPACT || LocalDensity.current.fontScale >= 1.5f

    RedactGuardScaffold(step = "Revisione", connection = connection) {
        ReviewHeader(
            finding = finding,
            position = position,
            total = total,
        )

        if (useCompactComposition) {
            CompactReviewContent(
                finding = finding,
                position = position,
                total = total,
                onRevealToggle = onRevealToggle,
                onRedact = onRedact,
                onIgnore = onIgnore,
                onPrevious = onPrevious,
                onNext = onNext,
                onExport = onExport,
                exportEnabled = exportEnabled,
            )
        } else {
            WideReviewContent(
                finding = finding,
                position = position,
                total = total,
                onRevealToggle = onRevealToggle,
                onRedact = onRedact,
                onIgnore = onIgnore,
                onPrevious = onPrevious,
                onNext = onNext,
                onExport = onExport,
                exportEnabled = exportEnabled,
                windowClass = windowClass,
            )
        }
    }
}

@Composable
private fun ColumnScope.CompactReviewContent(
    finding: ReviewFindingModel,
    position: Int,
    total: Int,
    onRevealToggle: () -> Unit,
    onRedact: () -> Unit,
    onIgnore: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onExport: () -> Unit,
    exportEnabled: Boolean,
) {
    Column(
        modifier =
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.md),
    ) {
        ReviewContextCard(finding = finding)
        ReviewDecisionPanel(
            finding = finding,
            position = position,
            total = total,
            onRevealToggle = onRevealToggle,
            onRedact = onRedact,
            onIgnore = onIgnore,
            onPrevious = onPrevious,
            onNext = onNext,
            onExport = onExport,
            exportEnabled = exportEnabled,
        )
    }
}

@Composable
private fun ColumnScope.WideReviewContent(
    finding: ReviewFindingModel,
    position: Int,
    total: Int,
    onRevealToggle: () -> Unit,
    onRedact: () -> Unit,
    onIgnore: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onExport: () -> Unit,
    exportEnabled: Boolean,
    windowClass: ProductWindowClass,
) {
    val contextWeight =
        if (windowClass == ProductWindowClass.EXPANDED) {
            1.35f
        } else {
            1.1f
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .weight(1f),
        horizontalArrangement = Arrangement.spacedBy(RedactGuardSpacing.md),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .weight(contextWeight)
                    .verticalScroll(rememberScrollState())
                    .semantics {
                        paneTitle = "Contesto del documento"
                    },
        ) {
            ReviewContextCard(finding = finding)
        }
        ReviewDecisionPanel(
            finding = finding,
            position = position,
            total = total,
            onRevealToggle = onRevealToggle,
            onRedact = onRedact,
            onIgnore = onIgnore,
            onPrevious = onPrevious,
            onNext = onNext,
            onExport = onExport,
            exportEnabled = exportEnabled,
            modifier =
                Modifier
                    .fillMaxHeight()
                    .weight(0.9f)
                    .verticalScroll(rememberScrollState())
                    .semantics {
                        paneTitle = "Decisione sulla rilevazione"
                    },
        )
    }
}

@Composable
private fun ReviewHeader(
    finding: ReviewFindingModel,
    position: Int,
    total: Int,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xxs),
            ) {
                Text(
                    "Revisione",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    "Revisione ${position + 1}/$total",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ReferenceSemanticTag(
                label = finding.categoryLabel,
                semanticKey = finding.categoryLabel,
            )
        }
        LinearProgressIndicator(
            progress = {
                (position + 1).toFloat() / total.coerceAtLeast(1).toFloat()
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription =
                            "Occorrenza ${position + 1} di $total"
                    },
        )
    }
}

@Composable
private fun ReviewContextCard(finding: ReviewFindingModel) {
    val context = finding.context
    val accent = piiAccent(finding.categoryLabel)
    val focusStart = context.maskedText.indexOf(context.focusPlaceholder)
    val annotated =
        buildAnnotatedString {
            if (focusStart < 0) {
                append(context.maskedText)
            } else {
                val focusEnd = focusStart + context.focusPlaceholder.length
                append(context.maskedText.substring(0, focusStart))
                withStyle(
                    SpanStyle(
                        background = accent.copy(alpha = 0.18f),
                        color = accent,
                        fontWeight = FontWeight.SemiBold,
                    ),
                ) {
                    append(context.focusPlaceholder)
                }
                append(context.maskedText.substring(focusEnd))
            }
        }

    ProductPanel {
        Column(
            verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xxs),
        ) {
            Text(
                "Possibile dato sensibile",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                "Contesto nel documento",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Contesto",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "Pagina ${context.pageNumber}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = annotated,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(RedactGuardSpacing.md),
            )
        }
        Text(
            "Le altre occorrenze restano mascherate: il contesto aiuta a " +
                "decidere senza esporre più dati del necessario.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReviewDecisionPanel(
    finding: ReviewFindingModel,
    position: Int,
    total: Int,
    onRevealToggle: () -> Unit,
    onRedact: () -> Unit,
    onIgnore: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onExport: () -> Unit,
    exportEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.md),
    ) {
        SensitiveValueCard(
            finding = finding,
            onRevealToggle = onRevealToggle,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs),
        ) {
            Text(
                "Azione",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onRedact,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Oscura")
            }
            OutlinedButton(
                onClick = onIgnore,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Mantieni")
            }
        }

        Text(
            reviewDecisionLabel(finding.decision),
            style = MaterialTheme.typography.labelMedium,
            color = decisionColor(finding.decision),
            modifier =
                Modifier.semantics {
                    liveRegion = LiveRegionMode.Polite
                },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(
                onClick = onPrevious,
                enabled = position > 0,
            ) {
                Text("← Precedente")
            }
            TextButton(
                onClick = onNext,
                enabled = position + 1 < total,
            ) {
                Text("Successiva →")
            }
        }

        if (!exportEnabled) {
            Text(
                "Completa la decisione per tutte le occorrenze prima di esportare.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(
            onClick = onExport,
            enabled = exportEnabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Esporta PDF protetto")
        }
    }
}

@Composable
private fun SensitiveValueCard(
    finding: ReviewFindingModel,
    onRevealToggle: () -> Unit,
) {
    val accent = piiAccent(finding.categoryLabel)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(RedactGuardSpacing.md),
            verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs),
        ) {
            ReferenceSemanticTag(
                label = finding.categoryLabel,
                semanticKey = finding.categoryLabel,
            )
            Text(
                "Valore rilevato",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                finding.revealedValue ?: finding.placeholder,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = onRevealToggle) {
                Text(
                    if (finding.revealedValue == null) {
                        "Mostra valore"
                    } else {
                        "Nascondi valore"
                    },
                )
            }
        }
    }
}

@Composable
private fun decisionColor(decision: ReviewDecision): Color =
    when (decision) {
        ReviewDecision.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
        ReviewDecision.REDACT -> referenceSuccessColor()
        ReviewDecision.IGNORE -> MaterialTheme.colorScheme.onSurfaceVariant
    }

private fun reviewDecisionLabel(decision: ReviewDecision): String =
    when (decision) {
        ReviewDecision.PENDING -> "Decisione da prendere"
        ReviewDecision.REDACT -> "Verrà oscurata"
        ReviewDecision.IGNORE -> "Verrà mantenuta"
    }
