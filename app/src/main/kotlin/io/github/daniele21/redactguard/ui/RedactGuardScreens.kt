@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package io.github.daniele21.redactguard.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.daniele21.redactguard.R
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
                    .padding(horizontal = RedactGuardSpacing.md, vertical = RedactGuardSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.md),
        ) {
            ProductTopBar(step = step, connection = connection)
            connection.explanation?.let { explanation ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = explanation,
                        style = MaterialTheme.typography.bodySmall,
                        modifier =
                            Modifier.padding(horizontal = RedactGuardSpacing.sm, vertical = RedactGuardSpacing.xs).semantics {
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
private fun ProductTopBar(
    step: String,
    connection: ConnectionBadgeModel,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(RedactGuardSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(RedactGuardSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Image(
                    painter = painterResource(R.drawable.redactguard_mark),
                    contentDescription = "RedactGuard",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.padding(RedactGuardSpacing.xxs).size(36.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xxs),
            ) {
                Text("RedactGuard", style = MaterialTheme.typography.titleLarge)
                Text(
                    step.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ConnectionBadge(connection)
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
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.18f)),
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
        StatusTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceContainer
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
private fun ProductPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(RedactGuardSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.md),
            content = content,
        )
    }
}

@Composable
internal fun ImportScreen(
    connection: ConnectionBadgeModel,
    onImportPdf: () -> Unit,
    onPasteText: () -> Unit,
) {
    RedactGuardScaffold(step = "Documento", connection = connection) {
        ProductPanel {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.large,
            ) {
                Text(
                    "PROTEZIONE LOCALE",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = RedactGuardSpacing.sm, vertical = RedactGuardSpacing.xs),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs)) {
                Text("Proteggi un documento", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Trova i dati sensibili, rivedili uno a uno e crea una copia protetta senza inviare il contenuto al cloud.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(RedactGuardSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs),
                ) {
                    Text("Solo sul dispositivo", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Il documento resta locale durante importazione, analisi e revisione.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Button(onClick = onImportPdf, modifier = Modifier.fillMaxWidth()) { Text("Importa PDF") }
            OutlinedButton(onClick = onPasteText, modifier = Modifier.fillMaxWidth()) { Text("Incolla testo") }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                "Sono supportati PDF con testo estraibile. Scansioni e PDF composti solo da immagini richiedono OCR e non vengono elaborati implicitamente.",
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
            Text(
                "Scegli un profilo come base. Puoi poi rifinire le singole categorie.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.md),
            modifier = Modifier.weight(1f),
        ) {
            if (profiles.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.sm)) {
                        Text("Profili rapidi", style = MaterialTheme.typography.titleMedium)
                        profiles.chunked(2).forEach { rowProfiles ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(RedactGuardSpacing.sm),
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
                CategoryChoiceRow(choice = choice, onClick = { onToggle(choice.id) })
            }
        }
        OutlinedButton(onClick = onAddCustom, modifier = Modifier.fillMaxWidth()) {
            Text("Aggiungi categoria personalizzata")
        }
        if (presets.size > 1) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
    val selectedBorder = if (profile.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Surface(
        onClick = onClick,
        color =
            if (profile.selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            },
        contentColor =
            if (profile.selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(if (profile.selected) 2.dp else 1.dp, selectedBorder),
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
            modifier = Modifier.padding(RedactGuardSpacing.md),
            verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(profile.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (profile.selected) {
                    Text("✓", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            Text(
                profile.description,
                style = MaterialTheme.typography.bodySmall,
                color = if (profile.selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (profile.selected) {
                Text("Profilo attivo", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CategoryChoiceRow(
    choice: DefinitionChoice,
    onClick: () -> Unit,
) {
    val borderColor = if (choice.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Surface(
        onClick = onClick,
        color = if (choice.selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(if (choice.selected) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = RedactGuardSpacing.md, vertical = RedactGuardSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(RedactGuardSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(choice.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                if (choice.selected) "Inclusa ✓" else "Esclusa",
                style = MaterialTheme.typography.labelMedium,
                color = if (choice.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun AnalysisScreen(
    connection: ConnectionBadgeModel,
    onCancel: () -> Unit,
) {
    RedactGuardScaffold(step = "Analisi", connection = connection) {
        ProductPanel {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(RedactGuardSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.sm),
                ) {
                    Text("Analisi locale in corso", style = MaterialTheme.typography.labelLarge)
                    LinearProgressIndicator(
                        modifier =
                            Modifier.fillMaxWidth().semantics {
                                contentDescription = "Analisi locale dei dati sensibili in corso"
                            },
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs)) {
                Text("Ricerca dei dati sensibili", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "L’AI locale sta cercando solo le categorie che hai scelto.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "La revisione si aprirà solo quando l’analisi sarà completata e validata. Nessun risultato parziale viene mostrato.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Annulla analisi") }
        }
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
    windowClass: ProductWindowClass = ProductWindowClass.COMPACT,
) {
    RedactGuardScaffold(step = "Revisione", connection = connection) {
        ReviewHeader(finding = finding, position = position, total = total)
        if (windowClass == ProductWindowClass.COMPACT) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.md),
            ) {
                ReviewContextCard(finding.context)
                ReviewDecisionPanel(
                    finding = finding,
                    position = position,
                    total = total,
                    onRevealToggle = onRevealToggle,
                    onRedact = onRedact,
                    onIgnore = onIgnore,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onExport = onExport,
                    exportEnabled = exportEnabled,
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(RedactGuardSpacing.md),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .weight(if (windowClass == ProductWindowClass.EXPANDED) 1.25f else 1f)
                            .verticalScroll(rememberScrollState())
                            .semantics { paneTitle = "Contesto del documento" },
                ) {
                    ReviewContextCard(finding.context)
                }
                ReviewDecisionPanel(
                    finding = finding,
                    position = position,
                    total = total,
                    onRevealToggle = onRevealToggle,
                    onRedact = onRedact,
                    onIgnore = onIgnore,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onExport = onExport,
                    exportEnabled = exportEnabled,
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .semantics { paneTitle = "Decisione sulla rilevazione" },
                )
            }
        }
    }
}

@Composable
private fun ReviewHeader(
    finding: ReviewFindingModel,
    position: Int,
    total: Int,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(RedactGuardSpacing.md),
            verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Revisione ${position + 1}/$total", style = MaterialTheme.typography.labelLarge)
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(
                        finding.categoryLabel,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = RedactGuardSpacing.sm, vertical = RedactGuardSpacing.xs),
                    )
                }
            }
            LinearProgressIndicator(
                progress = { (position + 1).toFloat() / total.coerceAtLeast(1).toFloat() },
                modifier =
                    Modifier.fillMaxWidth().semantics {
                        contentDescription = "Occorrenza ${position + 1} di $total"
                    },
            )
            Text("Decidi cosa oscurare", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun ReviewDecisionPanel(
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.md),
    ) {
        SensitiveValueCard(finding = finding, onRevealToggle = onRevealToggle)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RedactGuardSpacing.sm),
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
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                Text("Contesto", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("Pagina ${context.pageNumber}", style = MaterialTheme.typography.labelMedium)
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = annotated,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(RedactGuardSpacing.md),
                )
            }
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
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(RedactGuardSpacing.md),
            verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs),
        ) {
            Text("Valore rilevato", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
