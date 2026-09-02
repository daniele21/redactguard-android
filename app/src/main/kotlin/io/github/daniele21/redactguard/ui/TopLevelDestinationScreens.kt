@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package io.github.daniele21.redactguard.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.daniele21.redactguard.R
import io.github.daniele21.redactguard.ui.theme.RedactGuardSpacing

@Composable
internal fun LocalAiSetupScreen(
    model: LocalAiSetupUiModel,
    onRefresh: () -> Unit,
) {
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    var showTechnical by rememberSaveable { mutableStateOf(false) }
    DestinationSurface(
        title = "AI locale",
        subtitle = "Controlla se la configurazione locale è utilizzabile senza attivare o preparare il modello.",
    ) {
        LocalAiStatusCard(model)
        OutlinedButton(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(model.refreshLabel)
        }
        model.replacementNotice?.let(::LocalAiSetupNotice)

        ReferenceSectionHeader("Configurazione")
        ProductPanel {
            SetupDetailList(model.contextualDetails)
        }

        TextButton(
            onClick = { showAdvanced = !showAdvanced },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (showAdvanced) "Nascondi configurazione avanzata" else "Mostra configurazione avanzata")
        }
        if (showAdvanced) {
            ProductPanel {
                SetupDetailList(model.advancedDetails)
            }
        }

        if (model.technicalDetails.isNotEmpty()) {
            TextButton(
                onClick = { showTechnical = !showTechnical },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (showTechnical) "Nascondi dettagli tecnici" else "Mostra dettagli tecnici")
            }
            if (showTechnical) {
                ProductPanel {
                    SetupDetailList(model.technicalDetails)
                }
            }
        }

        Text(
            "Modello e configurazione restano gestiti dal servizio AI locale. Aprire o aggiornare questa sezione non carica il modello e non mantiene risorse AI in memoria.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun RedactGuardSettingsScreen() {
    DestinationSurface(
        title = "Impostazioni",
        subtitle = "Preferenze e confini propri di RedactGuard.",
    ) {
        ReferenceSectionHeader("Privacy")
        ProductPanel {
            Text(
                "Dati sensibili in memoria",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Testo del documento, risultati, valori rivelati e decisioni di review non vengono salvati in modo durevole per la navigazione.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ReferenceSectionHeader("AI locale")
        ProductPanel {
            Text(
                "Gestione separata",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Modelli e preset si gestiscono nel servizio AI locale. RedactGuard mostra solo le informazioni necessarie al proprio flusso di protezione.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "Non ci sono ancora altre preferenze di prodotto configurabili.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DestinationSurface(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RedactGuardSpacing.md, vertical = RedactGuardSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.md),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun LocalAiStatusCard(model: LocalAiSetupUiModel) {
    val colors = statusColors(model.tone)
    Surface(
        color = colors.first,
        contentColor = colors.second,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, colors.second.copy(alpha = 0.16f)),
        modifier =
            Modifier.fillMaxWidth().semantics {
                contentDescription = "Stato AI locale: ${model.statusLabel}"
            },
    ) {
        Row(
            modifier = Modifier.padding(RedactGuardSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(RedactGuardSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_rg_ai_local),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xxs),
            ) {
                Text(
                    model.statusLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    model.statusDescription,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun LocalAiSetupNotice(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(RedactGuardSpacing.md),
        )
    }
}

@Composable
private fun SetupDetailList(details: List<LocalAiSetupDetail>) {
    Column(verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.sm)) {
        details.forEach { detail -> SetupDetailRow(detail.label, detail.value) }
    }
}

@Composable
private fun SetupDetailRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RedactGuardSpacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(112.dp),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun statusColors(tone: StatusTone): Pair<Color, Color> =
    when (tone) {
        StatusTone.READY -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        StatusTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceContainer to MaterialTheme.colorScheme.onSurfaceVariant
        StatusTone.REVIEW -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        StatusTone.ERROR -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
