@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package io.github.daniele21.redactguard.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.daniele21.redactguard.R
import io.github.daniele21.redactguard.ui.theme.RedactGuardSpacing

@Composable
internal fun ImportingScreen(connection: ConnectionBadgeModel) {
    RedactGuardScaffold(step = "Importazione", connection = connection) {
        ProcessingStateCard(
            eyebrow = "DOCUMENTO",
            title = "Preparazione del documento",
            message = "Il contenuto viene preparato localmente sul dispositivo.",
            description = "Preparazione locale del documento in corso",
        )
    }
}

@Composable
internal fun NoFindingsScreen(
    connection: ConnectionBadgeModel,
    onExport: () -> Unit,
    onNewDocument: () -> Unit,
) {
    RedactGuardScaffold(step = "Revisione", connection = connection) {
        OutcomeMessage(
            title = "Nessuna occorrenza rilevata",
            message = "Non ci sono rilevazioni da rivedere. Puoi esportare il documento oppure iniziare con un altro.",
            contentDescription = "Analisi completata senza occorrenze rilevate",
        )
        Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Text("Esporta PDF") }
        OutlinedButton(onClick = onNewDocument, modifier = Modifier.fillMaxWidth()) { Text("Nuovo documento") }
    }
}

@Composable
internal fun ExportingScreen(connection: ConnectionBadgeModel) {
    RedactGuardScaffold(step = "Esportazione", connection = connection) {
        ProcessingStateCard(
            eyebrow = "PROTEZIONE",
            title = "Creazione del PDF protetto",
            message = "Il file viene generato localmente nella destinazione scelta.",
            description = "Creazione locale del PDF protetto in corso",
        )
    }
}

@Composable
internal fun ExportSuccessScreen(
    connection: ConnectionBadgeModel,
    onNewDocument: () -> Unit,
    summary: ProductDocumentSummary? = null,
) {
    RedactGuardScaffold(step = "Completato", connection = connection) {
        Column(
            modifier =
                Modifier.fillMaxWidth().semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = "PDF protetto creato con successo"
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs),
        ) {
            Image(
                painter = painterResource(R.drawable.rg_success_badge),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(150.dp),
            )
            Text(
                "Documento protetto",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Hai completato la revisione e il PDF protetto è stato creato localmente.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        summary?.takeIf { it.totalFindings > 0 }?.let { OutcomeSummaryCards(it) }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(RedactGuardSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(RedactGuardSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.size(42.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_rg_document),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(23.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("PDF protetto salvato", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Disponibile nella destinazione scelta durante l’esportazione.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Button(onClick = onNewDocument, modifier = Modifier.fillMaxWidth()) {
            Text("Nuovo documento")
        }
        Text(
            "RedactGuard non conserva una copia persistente del contenuto di revisione.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OutcomeSummaryCards(summary: ProductDocumentSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs),
    ) {
        OutcomeMetricCard("Totale occorrenze", summary.totalFindings, Modifier.weight(1f))
        OutcomeMetricCard("Oscurate", summary.redactedCount, Modifier.weight(1f))
        OutcomeMetricCard("Mantenute", summary.keptCount, Modifier.weight(1f))
    }
}

@Composable
private fun OutcomeMetricCard(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(RedactGuardSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xxs),
        ) {
            Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun ProductErrorScreen(
    connection: ConnectionBadgeModel,
    title: String,
    message: String,
    technicalDetails: ProductErrorTechnicalDetails? = null,
    onRetry: (() -> Unit)?,
    onNewDocument: () -> Unit,
) {
    var detailsVisible by remember(technicalDetails?.code) { mutableStateOf(false) }

    RedactGuardScaffold(step = "Errore", connection = connection) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.28f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(RedactGuardSpacing.md),
                verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.sm),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_rg_other),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier =
                        Modifier.semantics {
                            liveRegion = LiveRegionMode.Assertive
                            contentDescription = "Errore: $title"
                        },
                )
                Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        onRetry?.let { retry ->
            Button(onClick = retry, modifier = Modifier.fillMaxWidth()) { Text("Riprova") }
        }
        OutlinedButton(onClick = onNewDocument, modifier = Modifier.fillMaxWidth()) {
            Text("Nuovo documento")
        }

        technicalDetails?.let { details ->
            TechnicalDetailsDisclosure(
                details = details,
                visible = detailsVisible,
                onToggle = { detailsVisible = !detailsVisible },
            )
        }
    }
}

@Composable
private fun TechnicalDetailsDisclosure(
    details: ProductErrorTechnicalDetails,
    visible: Boolean,
    onToggle: () -> Unit,
) {
    TextButton(
        onClick = onToggle,
        modifier =
            Modifier.semantics {
                contentDescription =
                    if (visible) "Nascondi dettagli tecnici dell’errore" else "Mostra dettagli tecnici dell’errore"
            },
    ) {
        Text(if (visible) "Nascondi dettagli tecnici" else "Dettagli tecnici")
    }
    if (visible) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xxs),
                modifier =
                    Modifier.padding(RedactGuardSpacing.sm).semantics {
                        contentDescription = technicalFailureDescription(details)
                    },
            ) {
                Text("Codice: ${details.code}")
                Text("Causa: ${details.cause}")
                Text("Fase: ${details.stage}")
                details.lowLevelStep?.let { Text("Step: $it") }
                details.lowLevelType?.let { Text("Errore parser: $it") }
                details.operationId?.let { Text("Operazione: $it") }
            }
        }
    }
}

@Composable
private fun OutcomeMessage(
    title: String,
    message: String,
    contentDescription: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.24f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier.padding(RedactGuardSpacing.md).semantics {
                    liveRegion = LiveRegionMode.Polite
                    this.contentDescription = contentDescription
                },
            verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.rg_success_badge),
                contentDescription = null,
                modifier = Modifier.size(100.dp),
            )
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProcessingStateCard(
    eyebrow: String,
    title: String,
    message: String,
    description: String,
) {
    ProductPanel {
        Column(
            modifier =
                Modifier.fillMaxWidth().semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = description
                },
            verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    eyebrow,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = RedactGuardSpacing.sm, vertical = RedactGuardSpacing.xs),
                )
            }
            CircularProgressIndicator()
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun technicalFailureDescription(details: ProductErrorTechnicalDetails): String =
    buildString {
        append("Dettagli tecnici errore. Codice ${details.code}. Causa ${details.cause}. Fase ${details.stage}.")
        details.lowLevelStep?.let { append(" Step $it.") }
        details.lowLevelType?.let { append(" Errore parser $it.") }
        details.operationId?.let { append(" Operazione $it.") }
    }

@Composable
internal fun PasteTextDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Incolla testo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.sm)) {
                Text("Il testo resta sul dispositivo e segue la stessa analisi dei PDF con testo estraibile.")
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Testo da analizzare") },
                    minLines = 8,
                    maxLines = 16,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(text) }, enabled = text.isNotBlank()) { Text("Usa questo testo") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}

internal data class CustomPiiInput(
    val label: String,
    val definition: String,
    val example: String?,
) {
    override fun toString(): String = "CustomPiiInput(label=<redacted>, definition=<redacted>, example=<redacted>)"
}

@Composable
internal fun CustomPiiDialog(
    onDismiss: () -> Unit,
    onSubmit: (CustomPiiInput) -> Boolean,
) {
    var label by remember { mutableStateOf("") }
    var definition by remember { mutableStateOf("") }
    var example by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PII personalizzato") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = {
                        label = it
                        invalid = false
                    },
                    label = { Text("Nome") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = definition,
                    onValueChange = {
                        definition = it
                        invalid = false
                    },
                    label = { Text("Definizione") },
                    minLines = 3,
                )
                OutlinedTextField(
                    value = example,
                    onValueChange = {
                        example = it
                        invalid = false
                    },
                    label = { Text("Esempio facoltativo") },
                )
                if (invalid) {
                    Text("Controlla i campi o il limite di PII personalizzati.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val accepted =
                        onSubmit(
                            CustomPiiInput(
                                label = label,
                                definition = definition,
                                example = example.takeIf(String::isNotBlank),
                            ),
                        )
                    invalid = !accepted
                    if (accepted) onDismiss()
                },
            ) { Text("Aggiungi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}
