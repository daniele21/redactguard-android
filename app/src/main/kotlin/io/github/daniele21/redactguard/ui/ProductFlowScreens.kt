@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package io.github.daniele21.redactguard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal fun ImportingScreen(connection: ConnectionBadgeModel) {
    RedactGuardScaffold(step = "Importazione", connection = connection) {
        Text("Preparazione del documento")
        Text("Il contenuto viene preparato localmente sul dispositivo.")
    }
}

@Composable
internal fun NoFindingsScreen(
    connection: ConnectionBadgeModel,
    onExport: () -> Unit,
    onNewDocument: () -> Unit,
) {
    RedactGuardScaffold(step = "Revisione", connection = connection) {
        Text("Nessuna occorrenza rilevata")
        Text("Puoi esportare il documento normalizzato oppure iniziare con un altro documento.")
        Button(onClick = onExport) { Text("Esporta PDF") }
        OutlinedButton(onClick = onNewDocument) { Text("Nuovo documento") }
    }
}

@Composable
internal fun ExportingScreen(connection: ConnectionBadgeModel) {
    RedactGuardScaffold(step = "Esportazione", connection = connection) {
        Text("Creazione del PDF protetto")
        Text("Il file viene generato localmente nella destinazione scelta.")
    }
}

@Composable
internal fun ExportSuccessScreen(
    connection: ConnectionBadgeModel,
    onNewDocument: () -> Unit,
) {
    RedactGuardScaffold(step = "Completato", connection = connection) {
        Text("PDF protetto creato")
        Text("Riapri il file dalla destinazione scelta per verificarne il contenuto.")
        Button(onClick = onNewDocument) { Text("Nuovo documento") }
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
        Text(
            text = title,
            modifier = Modifier.semantics { contentDescription = "Errore: $title" },
        )
        Text(message)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            onRetry?.let { retry -> Button(onClick = retry) { Text("Riprova") } }
            OutlinedButton(onClick = onNewDocument) { Text("Nuovo documento") }
        }
        technicalDetails?.let { details ->
            TextButton(
                onClick = { detailsVisible = !detailsVisible },
                modifier =
                    Modifier.semantics {
                        contentDescription =
                            if (detailsVisible) {
                                "Nascondi dettagli tecnici dell’errore"
                            } else {
                                "Mostra dettagli tecnici dell’errore"
                            }
                    },
            ) {
                Text(if (detailsVisible) "Nascondi dettagli tecnici" else "Dettagli tecnici")
            }
            if (detailsVisible) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.semantics { contentDescription = technicalFailureDescription(details) },
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
}

private fun technicalFailureDescription(details: ProductErrorTechnicalDetails): String =
    buildString {
        append("Dettagli tecnici errore. Codice ${details.code}. Causa ${details.cause}. Fase ${details.stage}.")
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Il testo resta sul dispositivo e segue la stessa analisi dei PDF con testo estraibile.")
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
            Button(
                onClick = { onSubmit(text) },
                enabled = text.isNotBlank(),
            ) {
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    Text("Controlla i campi o il limite di PII personalizzati.")
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
