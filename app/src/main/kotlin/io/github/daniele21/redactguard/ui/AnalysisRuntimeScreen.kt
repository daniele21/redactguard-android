@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package io.github.daniele21.redactguard.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/** Source-backed analysis surface. No percentage is shown because Harness does not publish one. */
@Composable
internal fun AnalysisScreen(
    connection: ConnectionBadgeModel,
    progress: AnalysisProgressModel,
    onCancel: () -> Unit,
) {
    val searching = progress.visualStage == AnalysisVisualStage.SEARCHING
    val preparationState =
        if (searching || progress.visualStage == AnalysisVisualStage.FAILED) {
            ReferencePhaseState.DONE
        } else {
            ReferencePhaseState.ACTIVE
        }
    val searchState = if (searching) ReferencePhaseState.ACTIVE else ReferencePhaseState.PENDING

    RedactGuardScaffold(step = "Analisi", connection = connection) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs),
        ) {
            Image(
                painter = painterResource(R.drawable.rg_analysis_shield),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(140.dp),
            )
            Text(
                "Analisi in corso",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Il tuo documento viene analizzato completamente in locale.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                progress.title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Text(
                progress.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LinearProgressIndicator(
            modifier =
                Modifier.fillMaxWidth().semantics {
                    contentDescription = progress.contentDescription
                },
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.sm),
        ) {
            ReferencePhaseRow("Documento preparato", preparationState)
            ReferencePhaseRow("Ricerca dati sensibili", searchState)
            ReferencePhaseRow("Validazione risultati", ReferencePhaseState.PENDING)
        }

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Annulla analisi")
        }
    }
}
