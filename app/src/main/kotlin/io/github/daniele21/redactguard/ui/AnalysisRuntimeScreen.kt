@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package io.github.daniele21.redactguard.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics

/** Source-backed analysis surface. No percentage is shown because Harness does not publish one. */
@Composable
internal fun AnalysisScreen(
    connection: ConnectionBadgeModel,
    progress: AnalysisProgressModel,
    onCancel: () -> Unit,
) {
    RedactGuardScaffold(step = "Analisi", connection = connection) {
        Text(
            progress.title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Text(progress.message)
        LinearProgressIndicator(
            modifier =
                Modifier.fillMaxWidth().semantics {
                    contentDescription = progress.contentDescription
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
