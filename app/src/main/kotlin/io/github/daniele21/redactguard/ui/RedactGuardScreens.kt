@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package io.github.daniele21.redactguard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
                Modifier.fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = RedactGuardSpacing.lg, vertical = RedactGuardSpacing.md),
            verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xxs)) {
                    Text("RedactGuard", style = MaterialTheme.typography.titleLarge)
                    Text(
                        step,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ConnectionBadge(connection)
            }
            connection.explanation?.let { explanation ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = explanation,
                        style = MaterialTheme.typography.bodySmall,
                        modifier =
                            Modifier.padding(RedactGuardSpacing.sm).semantics {
                                contentDescription = "Dettaglio stato AI locale: $explanation"
                            },
                    )
                }
            }
            content()
        }
    }
}

@Composable
internal fun ConnectionBadge(model: ConnectionBadgeModel) {
    val containerColor = connectionContainerColor(model.tone)
    val contentColor = connectionContentColor(model.tone)
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.semantics { contentDescription = "Stato AI locale: ${model.label}" },
    ) {
        Text(
            model.label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = RedactGuardSpacing.sm, vertical = RedactGuardSpacing.xs),
        )
    }
}

@Composable
private fun connectionContainerColor(tone: StatusTone): Color =
    when (tone) {
        StatusTone.READY -> MaterialTheme.colorScheme.primaryContainer
        StatusTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
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
internal fun ImportScreen(
    connection: ConnectionBadgeModel,
    onImportPdf: () -> Unit,
    onPasteText: () -> Unit,
) {
    RedactGuardScaffold(step = "Documento", connection = connection) {
        Column(verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.sm)) {
            Text("Proteggi un documento", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Il contenuto resta sul dispositivo. Importa un PDF con testo estraibile oppure incolla il testo.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = onImportPdf, modifier = Modifier.fillMaxWidth()) { Text("Importa PDF") }
            OutlinedButton(onClick = onPasteText, modifier = Modifier.fillMaxWidth()) { Text("Incolla testo") }
            Text(
                "Le scansioni e i PDF composti solo da immagini non sono ancora supportati.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun DefinitionSelectionScreen(
    connection: ConnectionBadgeModel,
    choices: List<DefinitionChoice>,
    onToggle: (String) -> Unit,
    onAddCustom: () -> Unit,
    onAnalyze: () -> Unit,
) {
    val hasSelection = choices.any(DefinitionChoice::selected)
    RedactGuardScaffold(step = "Dati da proteggere", connection = connection) {
        Column(verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xxs)) {
            Text("Cosa vuoi proteggere?", style = MaterialTheme.typography.headlineMedium)
            Text("Seleziona almeno una categoria. Potrai decidere cosa oscurare durante la revisione.")
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs),
            modifier = Modifier.weight(1f),
        ) {
            items(choices, key = DefinitionChoice::id) { choice ->
                FilterChip(
                    selected = choice.selected,
                    onClick = { onToggle(choice.id) },
                    label = { Text(choice.label) },
                )
            }
        }
        OutlinedButton(onClick = onAddCustom) { Text("Aggiungi categoria personalizzata") }
        if (!connection.analysisReady) {
            Text(
                "L’analisi sarà disponibile quando l’AI locale sarà pronta.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (!hasSelection) {
            Text(
                "Seleziona almeno una categoria per continuare.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(
            onClick = onAnalyze,
            enabled = connection.analysisReady && hasSelection,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Analizza in locale")
        }
    }
}

@Composable
internal fun AnalysisScreen(
    connection: ConnectionBadgeModel,
    onCancel: () -> Unit,
) {
    RedactGuardScaffold(step = "Analisi", connection = connection) {
        Text("Ricerca dei dati sensibili", style = MaterialTheme.typography.headlineMedium)
        Text("Il documento è stato preparato. L’AI locale sta cercando le categorie selezionate.")
        LinearProgressIndicator(
            modifier =
                Modifier.fillMaxWidth().semantics {
                    contentDescription = "Analisi locale dei dati sensibili in corso"
                },
        )
        Text(
            "La revisione si aprirà solo quando l’analisi sarà completata e validata. Nessun risultato parziale viene mostrato.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onCancel) { Text("Annulla analisi") }
    }
}

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
) {
    RedactGuardScaffold(step = "Revisione", connection = connection) {
        Column(verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xxs)) {
            Text("Decidi cosa oscurare", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Occorrenza ${position + 1} di $total · ${finding.categoryLabel}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(RedactGuardSpacing.md),
                verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.sm),
            ) {
                Text(reviewDecisionLabel(finding.decision), style = MaterialTheme.typography.labelMedium)
                Text(
                    finding.revealedValue ?: finding.placeholder,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                OutlinedButton(onClick = onRevealToggle) {
                    Text(if (finding.revealedValue == null) "Mostra valore" else "Nascondi valore")
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs)) {
            Button(onClick = onRedact) { Text("Oscura") }
            OutlinedButton(onClick = onIgnore) { Text("Ignora") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs)) {
            OutlinedButton(onClick = onPrevious, enabled = position > 0) { Text("Precedente") }
            OutlinedButton(onClick = onNext, enabled = position + 1 < total) { Text("Successiva") }
        }
        if (!exportEnabled) {
            Text(
                "Completa la decisione per tutte le occorrenze prima di esportare.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(onClick = onExport, enabled = exportEnabled, modifier = Modifier.fillMaxWidth()) {
            Text("Esporta PDF protetto")
        }
    }
}

private fun reviewDecisionLabel(decision: ReviewDecision): String =
    when (decision) {
        ReviewDecision.PENDING -> "Decisione da prendere"
        ReviewDecision.REDACT -> "Verrà oscurata"
        ReviewDecision.IGNORE -> "Verrà mantenuta"
    }
