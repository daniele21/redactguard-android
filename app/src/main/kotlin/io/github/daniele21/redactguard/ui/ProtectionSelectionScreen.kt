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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.daniele21.redactguard.ui.theme.RedactGuardSpacing

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
                "Scegli un preset consigliato oppure personalizza le categorie.",
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
                        ReferenceSectionHeader("Preset consigliati")
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
                                if (rowProfiles.size == 1) {
                                    Box(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                item { ReferenceSectionHeader("Categorie selezionate") }
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
                        "Mostriamo solo le opzioni consumer-safe pubblicate dall’AI locale.",
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
    val accent = profileAccent(profile.id)
    val selectedBorder = if (profile.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    Surface(
        onClick = onClick,
        color =
            if (profile.selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            },
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(if (profile.selected) 1.5.dp else 1.dp, selectedBorder),
        shadowElevation = if (profile.selected) 1.dp else 0.dp,
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
            verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = accent.copy(alpha = 0.12f),
                    contentColor = accent,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.size(34.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            profile.label.take(1).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                ReferenceSelectionBadge(selected = profile.selected)
            }
            Text(profile.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                profile.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
            )
            if (profile.selected) {
                Text(
                    "Profilo attivo",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun profileAccent(id: String): Color =
    when (id.uppercase()) {
        "HEALTHCARE" -> piiAccent("health")
        "FINANCIAL" -> piiAccent("financial")
        "LEGAL" -> piiAccent("other")
        else -> piiAccent("identity")
    }

@Composable
private fun CategoryChoiceRow(
    choice: DefinitionChoice,
    onClick: () -> Unit,
) {
    val accent = piiAccent(choice.id)
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
        border =
            BorderStroke(
                if (choice.selected) 1.25.dp else 1.dp,
                if (choice.selected) accent.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant,
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = RedactGuardSpacing.sm, vertical = RedactGuardSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(RedactGuardSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = accent.copy(alpha = 0.12f),
                contentColor = accent,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(34.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(color = accent, shape = CircleShape, modifier = Modifier.size(9.dp)) {}
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(choice.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    if (choice.selected) "Inclusa ✓" else "Esclusa",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (choice.selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = choice.selected,
                onCheckedChange = null,
                modifier =
                    Modifier.semantics {
                        contentDescription =
                            if (choice.selected) {
                                "${choice.label}, inclusa"
                            } else {
                                "${choice.label}, esclusa"
                            }
                    },
            )
        }
    }
}
