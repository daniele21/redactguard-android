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
import androidx.compose.ui.unit.dp

@Composable
internal fun ImportingScreen(connection: ConnectionBadgeModel) {
    RedactGuardScaffold(step = "Importazione", connection = connection) {
        Text("Preparazione del documento")
        Text("Il PDF viene letto localmente sul dispositivo.")
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
        Text("Puoi esportare il documento normalizzato oppure iniziare con un altro PDF.")
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
    onRetry: (() -> Unit)?,
    onNewDocument: () -> Unit,
) {
    RedactGuardScaffold(step = "Errore", connection = connection) {
        Text(title)
        Text(message)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            onRetry?.let { retry -> Button(onClick = retry) { Text("Riprova") } }
            OutlinedButton(onClick = onNewDocument) { Text("Nuovo documento") }
        }
    }
}

internal data class CustomPiiInput(
    val label: String,
    val definition: String,
    val example: String?,
)

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
