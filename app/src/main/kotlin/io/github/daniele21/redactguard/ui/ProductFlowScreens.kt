@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package io.github.daniele21.redactguard.ui

import androidx.compose.foundation.BorderStroke
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
        OutcomeCard(
            eyebrow = "ANALISI COMPLETATA",
            title = "Nessuna occorrenza rilevata",
            message =
                "Non ci sono rilevazioni da rivedere. Puoi esportare il " +
                    "documento normalizzato oppure iniziare con un altro documento.",
            contentDescription = "Analisi completata senza occorrenze rilevate",
        )
        Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
            Text("Esporta PDF")
        }
        OutlinedButton(onClick = onNewDocument, modifier = Modifier.fillMaxWidth()) {
            Text("Nuovo documento")
        }
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
) {
    RedactGuardScaffold(step = "Completato", connection = connection) {
        OutcomeCard(
            eyebrow = "PROTEZIONE COMPLETATA",
            title = "Documento protetto",
            message =
                "PDF protetto creato. Riaprilo per verificare il contenuto " +
                    "prima di condividerlo.",
            contentDescription = "PDF protetto creato con successo",
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(RedactGuardSpacing.md),
                verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs),
            ) {
                Text("Prossimo passo", style = MaterialTheme.typography.labelMedium)
                Text(
                    "Verifica il PDF esportato. RedactGuard non mantiene una " +
                        "copia persistente del contenuto di revisione.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Button(onClick = onNewDocument, modifier = Modifier.fillMaxWidth()) {
            Text("Proteggi un altro documento")
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
    var detailsVisible by
        remember(technicalDetails?.code) {
            mutableStateOf(false)
        }

    RedactGuardScaffold(step = "Errore", connection = connection) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = MaterialTheme.shapes.extraLarge,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.30f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(RedactGuardSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.sm),
            ) {
                Text("AZIONE RICHIESTA", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier =
                        Modifier.semantics {
                            liveRegion = LiveRegionMode.Assertive
                            contentDescription = "Errore: $title"
                        },
                )
                Text(message, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RedactGuardSpacing.sm),
        ) {
            onRetry?.let { retry ->
                Button(onClick = retry, modifier = Modifier.weight(1f)) {
                    Text("Riprova")
                }
            }
            OutlinedButton(onClick = onNewDocument, modifier = Modifier.weight(1f)) {
                Text("Nuovo documento")
            }
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
                    if (visible) {
                        "Nascondi dettagli tecnici dell’errore"
                    } else {
                        "Mostra dettagli tecnici dell’errore"
                    }
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
                details.lowLevelStep?.let { step -> Text("Step: $step") }
                details.lowLevelType?.let { type -> Text("Errore parser: $type") }
                details.operationId?.let { operationId -> Text("Operazione: $operationId") }
            }
        }
    }
}

@Composable
private fun OutcomeCard(
    eyebrow: String,
    title: String,
    message: String,
    contentDescription: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.24f)),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier.padding(RedactGuardSpacing.lg).semantics {
                    liveRegion = LiveRegionMode.Polite
                    this.contentDescription = contentDescription
                },
            verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondary,
                contentColor = Color.White,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.size(92.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_rg_check_circle),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(54.dp),
                    )
                }
            }
            Text(
                eyebrow,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                    modifier =
                        Modifier.padding(
                            horizontal = RedactGuardSpacing.sm,
                            vertical = RedactGuardSpacing.xs,
                        ),
                )
            }
            CircularProgressIndicator()
            Column(
                verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private fun technicalFailureDescription(details: ProductErrorTechnicalDetails): String =
    buildString {
        append(
            "Dettagli tecnici errore. Codice ${details.code}. " +
                "Causa ${details.cause}. Fase ${details.stage}.",
        )
        details.lowLevelStep?.let { step -> append(" Step $step.") }
        details.lowLevelType?.let { type -> append(" Errore parser $type.") }
        details.operationId?.let { operationId -> append(" Operazione $operationId.") }
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
                Text(
                    "Il testo resta sul dispositivo e segue la stessa analisi " +
                        "dei PDF con testo estraibile.",
                )
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
            Button(onClick = { onSubmit(text) }, enabled = text.isNotBlank()) {
                Text("Usa questo testo")
            }
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
                    Text(
                        "Controlla i campi o il limite di PII personalizzati.",
                        color = MaterialTheme.colorScheme.error,
                    )
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
            ) {
                Text("Aggiungi")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}
