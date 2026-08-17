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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal fun RedactGuardScaffold(
    step: String,
    connection: ConnectionBadgeModel,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("RedactGuard", style = MaterialTheme.typography.titleLarge)
                    Text(step, style = MaterialTheme.typography.labelMedium)
                }
                ConnectionBadge(connection)
            }
            connection.explanation?.let { explanation ->
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.semantics { contentDescription = "Dettaglio stato Local AI: $explanation" },
                )
            }
            content()
        }
    }
}

@Composable
internal fun ConnectionBadge(model: ConnectionBadgeModel) {
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.semantics { contentDescription = "Stato Local AI: ${model.label}" },
    ) {
        Text(model.label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
    }
}

@Composable
internal fun ImportScreen(
    connection: ConnectionBadgeModel,
    onImportPdf: () -> Unit,
) {
    RedactGuardScaffold(step = "Importazione", connection = connection) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Proteggi un documento", style = MaterialTheme.typography.headlineMedium)
            Text("Il documento resta sul dispositivo. Seleziona un PDF da analizzare e proteggere.")
            Button(onClick = onImportPdf) { Text("Importa PDF") }
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
    RedactGuardScaffold(step = "Dati da proteggere", connection = connection) {
        Text("Scegli cosa rilevare", style = MaterialTheme.typography.headlineMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(choices, key = DefinitionChoice::id) { choice ->
                FilterChip(
                    selected = choice.selected,
                    onClick = { onToggle(choice.id) },
                    label = { Text(choice.label) },
                )
            }
        }
        OutlinedButton(onClick = onAddCustom) { Text("Aggiungi PII personalizzato") }
        if (!connection.analysisReady) {
            Text("L’analisi sarà disponibile quando Harness sarà connesso.")
        }
        Button(
            onClick = onAnalyze,
            enabled = connection.analysisReady && choices.any(DefinitionChoice::selected),
        ) {
            Text("Analizza documento")
        }
    }
}

@Composable
internal fun AnalysisScreen(
    connection: ConnectionBadgeModel,
    onCancel: () -> Unit,
) {
    RedactGuardScaffold(step = "Analisi", connection = connection) {
        Text("Analisi locale in corso", style = MaterialTheme.typography.headlineMedium)
        Text("✓ Testo estratto")
        Text("● PII in analisi")
        Text("○ Revisione pronta")
        OutlinedButton(onClick = onCancel) { Text("Annulla") }
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
        Text("Verifica le occorrenze", style = MaterialTheme.typography.headlineMedium)
        Text("${position + 1} di $total · ${finding.categoryLabel}")
        Text(finding.revealedValue ?: finding.placeholder)
        OutlinedButton(onClick = onRevealToggle) {
            Text(if (finding.revealedValue == null) "Mostra valore" else "Nascondi valore")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRedact) { Text("Oscura") }
            OutlinedButton(onClick = onIgnore) { Text("Ignora") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPrevious, enabled = position > 0) { Text("Precedente") }
            OutlinedButton(onClick = onNext, enabled = position + 1 < total) { Text("Successiva") }
        }
        Button(onClick = onExport, enabled = exportEnabled) { Text("Esporta PDF") }
    }
}
