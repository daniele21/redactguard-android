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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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
                Modifier
                    .fillMaxSize()
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
    profiles: List<ProtectionProfileChoice> = emptyList(),
    presets: List<LocalAiPresetChoice> = emptyList(),
    presetSelectionNotice: String? = null,
    onToggle: (String) -> Unit,
    onProfileSelect: (String) -> Unit = {},
    onPresetSelect: (String) -> Unit = {},
    onAddCustom: () -> Unit,
    onAnalyze: () -> Unit,
) {
    val hasSelection = choices.any(DefinitionChoice::selected)
    val presetReady = presets.size <= 1 || presets.any(LocalAiPresetChoice::selected)
    RedactGuardScaffold(step = "Dati da proteggere", connection = connection) {
        Column(verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xxs)) {
            Text("Cosa vuoi proteggere?", style = MaterialTheme.typography.headlineMedium)
            Text("Parti da un profilo oppure personalizza le singole categorie.")
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.sm),
            modifier = Modifier.weight(1f),
        ) {
            if (profiles.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs)) {
                        Text("Profili rapidi", style = MaterialTheme.typography.titleMedium)
                        profiles.chunked(2).forEach { rowProfiles ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs),
                            ) {
                                rowProfiles.forEach { profile ->
                                    ProtectionProfileCard(
                                        profile = profile,
                                        onClick = { onProfileSelect(profile.id) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    Text("Personalizza categorie", style = MaterialTheme.typography.titleMedium)
                }
            }
            items(choices, key = DefinitionChoice::id) { choice ->
                FilterChip(
                    selected = choice.selected,
                    onClick = { onToggle(choice.id) },
                    label = { Text(choice.label) },
                )
            }
        }
        OutlinedButton(onClick = onAddCustom) { Text("Aggiungi categoria personalizzata") }
        if (presets.size > 1) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(RedactGuardSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs),
                ) {
                    Text("Modalità di analisi", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Scegli tra le opzioni rese disponibili dall’AI locale per questo utilizzo.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    presets.forEach { preset ->
                        FilterChip(
                            selected = preset.selected,
                            onClick = { onPresetSelect(preset.id) },
                            label = {
                                Column(verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xxs)) {
                                    Text(preset.label)
                                    preset.description?.let { description ->
                                        Text(description, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
        presetSelectionNotice?.let { notice ->
            Text(
                text = notice,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        when {
            !connection.analysisReady -> {
                Text(
                    "L’analisi sarà disponibile quando l’AI locale sarà collegata.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            !hasSelection -> {
                Text(
                    "Seleziona almeno una categoria per continuare.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            !presetReady -> {
                Text(
                    "Seleziona una modalità di analisi per continuare.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Button(
            onClick = onAnalyze,
            enabled = connection.analysisReady && hasSelection && presetReady,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Analizza in locale")
        }
    }
}

@Composable
private fun ProtectionProfileCard(
    profile: ProtectionProfileChoice,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        color =
            if (profile.selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        contentColor =
            if (profile.selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        shape = MaterialTheme.shapes.large,
        modifier =
            modifier.semantics {
                contentDescription =
                    if (profile.selected) {
                        "Profilo ${profile.label}, selezionato"
                    } else {
                        "Profilo ${profile.label}"
                    }
            },
    ) {
        Column(
            modifier = Modifier.padding(RedactGuardSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xxs),
        ) {
            Text(profile.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(profile.description, style = MaterialTheme.typography.bodySmall)
            if (profile.selected) {
                Text("Selezionato", style = MaterialTheme.typography.labelSmall)
            }
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
        Column(verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Revisione ${position + 1}/$total", style = MaterialTheme.typography.labelLarge)
                Text(
                    finding.categoryLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            LinearProgressIndicator(
                modifier =
                    Modifier.fillMaxWidth().semantics {
                        contentDescription = "Occorrenza ${position + 1} di $total"
                    },
            )
            Text("Decidi cosa oscurare", style = MaterialTheme.typography.headlineMedium)
        }

        ReviewContextCard(finding.context)
        SensitiveValueCard(finding = finding, onRevealToggle = onRevealToggle)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs),
        ) {
            Button(onClick = onRedact, modifier = Modifier.weight(1f)) { Text("Oscura") }
            OutlinedButton(onClick = onIgnore, modifier = Modifier.weight(1f)) { Text("Mantieni") }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onPrevious, enabled = position > 0) { Text("Precedente") }
            Text(
                reviewDecisionLabel(finding.decision),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            TextButton(onClick = onNext, enabled = position + 1 < total) { Text("Successiva") }
        }

        if (!exportEnabled) {
            Text(
                "Completa la decisione per tutte le occorrenze prima di esportare.",
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
private fun ReviewContextCard(context: ReviewContextModel) {
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
                        background = MaterialTheme.colorScheme.primaryContainer,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold,
                    ),
                ) {
                    append(context.focusPlaceholder)
                }
                append(context.maskedText.substring(focusEnd))
            }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Contesto", style = MaterialTheme.typography.titleMedium)
                Text("Pagina ${context.pageNumber}", style = MaterialTheme.typography.labelMedium)
            }
            Text(
                text = annotated,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Le altre occorrenze rilevate nel contesto restano mascherate.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SensitiveValueCard(
    finding: ReviewFindingModel,
    onRevealToggle: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(RedactGuardSpacing.md),
            verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs),
        ) {
            Text("Valore rilevato", style = MaterialTheme.typography.labelMedium)
            Text(
                finding.revealedValue ?: finding.placeholder,
                style = MaterialTheme.typography.titleLarge,
            )
            TextButton(onClick = onRevealToggle) {
                Text(if (finding.revealedValue == null) "Mostra valore" else "Nascondi valore")
            }
        }
    }
}

private fun reviewDecisionLabel(decision: ReviewDecision): String =
    when (decision) {
        ReviewDecision.PENDING -> "Decisione da prendere"
        ReviewDecision.REDACT -> "Verrà oscurata"
        ReviewDecision.IGNORE -> "Verrà mantenuta"
    }
