@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package io.github.daniele21.redactguard.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
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
import io.github.daniele21.redactguard.R
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
    summary: ProductDocumentSummary? = null,
) {
    val useCompactComposition =
        windowClass == ProductWindowClass.COMPACT || LocalDensity.current.fontScale >= 1.5f

    RedactGuardScaffold(step = "Revisione", connection = connection) {
        ReviewHeader(position = position, total = total)

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
                summary = summary,
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
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs),
    ) {
        ReferenceSemanticTag(label = finding.categoryLabel, semanticKey = finding.categoryLabel)
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
    summary: ProductDocumentSummary?,
    onRevealToggle: () -> Unit,
    onRedact: () -> Unit,
    onIgnore: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onExport: () -> Unit,
    exportEnabled: Boolean,
    windowClass: ProductWindowClass,
) {
    Row(
        modifier = Modifier.fillMaxWidth().weight(1f),
        horizontalArrangement = Arrangement.spacedBy(RedactGuardSpacing.sm),
    ) {
        if (windowClass == ProductWindowClass.EXPANDED && summary != null) {
            ReviewSummaryRail(
                summary = summary,
                position = position,
                total = total,
                currentPage = finding.context.pageNumber,
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .weight(0.72f)
                        .verticalScroll(rememberScrollState())
                        .semantics { paneTitle = "Riepilogo documento" },
            )
        }

        Column(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .weight(if (windowClass == ProductWindowClass.EXPANDED) 1.25f else 1.1f)
                    .verticalScroll(rememberScrollState())
                    .semantics { paneTitle = "Contesto del documento" },
            verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs),
        ) {
            ReferenceSemanticTag(label = finding.categoryLabel, semanticKey = finding.categoryLabel)
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
                    .semantics { paneTitle = "Decisione sulla rilevazione" },
        )
    }
}

@Composable
private fun ReviewHeader(
    position: Int,
    total: Int,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xxs),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Revisione", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            "${position + 1} di $total",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { (position + 1).toFloat() / total.coerceAtLeast(1).toFloat() },
            modifier =
                Modifier.fillMaxWidth().semantics {
                    contentDescription = "Occorrenza ${position + 1} di $total"
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

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(RedactGuardSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xxs)) {
                Text("Possibile dato sensibile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Contesto nel documento",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Pagina ${context.pageNumber}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                    modifier = Modifier.padding(RedactGuardSpacing.sm),
                )
            }
        }
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
        verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs),
    ) {
        SensitiveValueCard(finding = finding, onRevealToggle = onRevealToggle)

        Text(
            "Azione",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onRedact, modifier = Modifier.fillMaxWidth()) {
            Text("Oscura (consigliato)")
        }
        OutlinedButton(onClick = onIgnore, modifier = Modifier.fillMaxWidth()) {
            Text("Mantieni")
        }

        Text(
            reviewDecisionLabel(finding.decision),
            style = MaterialTheme.typography.labelMedium,
            color = decisionColor(finding.decision),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onPrevious, enabled = position > 0) { Text("← Precedente") }
            TextButton(onClick = onNext, enabled = position + 1 < total) { Text("Successiva →") }
        }

        if (!exportEnabled) {
            Text(
                "Completa tutte le decisioni per esportare.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(onClick = onExport, enabled = exportEnabled, modifier = Modifier.fillMaxWidth()) {
            Text("Esporta PDF protetto")
        }
    }
}

@Composable
private fun SensitiveValueCard(
    finding: ReviewFindingModel,
    onRevealToggle: () -> Unit,
) {
    val revealed = finding.revealedValue != null
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier.padding(
                    start = RedactGuardSpacing.sm,
                    top = RedactGuardSpacing.xs,
                    end = RedactGuardSpacing.xxs,
                    bottom = RedactGuardSpacing.xs,
                ),
            horizontalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xxs),
            ) {
                Text(
                    "Valore rilevato",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    finding.revealedValue ?: "••••••••••••",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier =
                        Modifier.semantics {
                            contentDescription =
                                if (revealed) "Valore sensibile mostrato" else "Valore sensibile nascosto"
                        },
                )
            }
            IconButton(
                onClick = onRevealToggle,
                modifier =
                    Modifier.semantics {
                        contentDescription =
                            if (revealed) "Nascondi valore sensibile" else "Mostra valore sensibile"
                    },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_rg_visibility),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReviewSummaryRail(
    summary: ProductDocumentSummary,
    position: Int,
    total: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(RedactGuardSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.sm),
        ) {
            Text("Documento", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(summary.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2)
            Text(
                "Pagina $currentPage di ${summary.pageCount}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text("Revisione", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${position + 1} di $total", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

            summary.categoryCounts.forEach { category ->
                SummaryCategoryRow(category)
            }
        }
    }
}

@Composable
private fun SummaryCategoryRow(category: ProductCategorySummary) {
    val spec = reviewFamilySpec(category.family)
    val accent = reviewFamilyAccent(category.family)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(spec.iconRes),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.padding(2.dp),
        )
        Text(spec.label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(category.count.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

private data class ReviewFamilySpec(
    val label: String,
    @DrawableRes val iconRes: Int,
)

private fun reviewFamilySpec(family: PiiVisualFamily): ReviewFamilySpec =
    when (family) {
        PiiVisualFamily.IDENTITY -> ReviewFamilySpec("Identità", R.drawable.ic_rg_identity)
        PiiVisualFamily.CONTACT -> ReviewFamilySpec("Contatti", R.drawable.ic_rg_contact)
        PiiVisualFamily.HEALTH -> ReviewFamilySpec("Salute", R.drawable.ic_rg_health)
        PiiVisualFamily.FINANCIAL -> ReviewFamilySpec("Finanziarie", R.drawable.ic_rg_financial)
        PiiVisualFamily.LOCATION -> ReviewFamilySpec("Luoghi", R.drawable.ic_rg_location)
        PiiVisualFamily.OTHER -> ReviewFamilySpec("Altro", R.drawable.ic_rg_other)
    }

@Composable
private fun reviewFamilyAccent(family: PiiVisualFamily): Color =
    when (family) {
        PiiVisualFamily.IDENTITY -> piiAccent("identity")
        PiiVisualFamily.CONTACT -> piiAccent("contact")
        PiiVisualFamily.HEALTH -> piiAccent("health")
        PiiVisualFamily.FINANCIAL -> piiAccent("financial")
        PiiVisualFamily.LOCATION -> piiAccent("location")
        PiiVisualFamily.OTHER -> piiAccent("other")
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
