@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package io.github.daniele21.redactguard.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalDensity
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
    val familyGroups = choices.groupBy { choice -> PiiVisualFamilyProjector.projectTypeId(choice.id) }
    var showAdvanced by rememberSaveable { mutableStateOf(false) }

    RedactGuardScaffold(step = "Dati da proteggere", connection = connection) {
        Column(verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xxs)) {
            Text("Cosa vuoi proteggere?", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Scegli le categorie da proteggere o usa un preset.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.sm),
            modifier = Modifier.weight(1f),
        ) {
            if (profiles.isNotEmpty()) {
                item { ProfileGrid(profiles = profiles, onProfileSelect = onProfileSelect) }
            }

            item { ReferenceSectionHeader("Categorie selezionate") }

            items(
                items = PiiVisualFamily.entries.filter(familyGroups::containsKey),
                key = PiiVisualFamily::name,
            ) { family ->
                val definitions = familyGroups.getValue(family)
                FamilyChoiceRow(
                    family = family,
                    choices = definitions,
                    onClick = {
                        val targetSelected = !definitions.all(DefinitionChoice::selected)
                        definitions
                            .filter { it.selected != targetSelected }
                            .forEach { choice -> onToggle(choice.id) }
                    },
                )
            }

            item {
                TextButton(onClick = { showAdvanced = !showAdvanced }) {
                    Text(if (showAdvanced) "Nascondi personalizzazione" else "Personalizza categorie e analisi")
                }
            }

            if (showAdvanced) {
                item {
                    OutlinedButton(onClick = onAddCustom, modifier = Modifier.fillMaxWidth()) {
                        Text("Aggiungi categoria personalizzata")
                    }
                }
                if (presets.size > 1) {
                    item { PresetSelector(presets = presets, onPresetSelect = onPresetSelect) }
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

        SelectionReadinessMessage(
            connection = connection,
            hasSelection = hasSelection,
            presetReady = presetReady,
        )

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
private fun ProfileGrid(
    profiles: List<ProtectionProfileChoice>,
    onProfileSelect: (String) -> Unit,
) {
    val fontScale = LocalDensity.current.fontScale
    Column(verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs)) {
        ReferenceSectionHeader("Preset consigliati")
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val useTwoColumns = maxWidth >= 360.dp && fontScale < 1.3f
            Column(
                verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (useTwoColumns) {
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
                            if (rowProfiles.size == 1) Box(modifier = Modifier.weight(1f))
                        }
                    }
                } else {
                    profiles.forEach { profile ->
                        ProtectionProfileCard(
                            profile = profile,
                            onClick = { onProfileSelect(profile.id) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FamilyChoiceRow(
    family: PiiVisualFamily,
    choices: List<DefinitionChoice>,
    onClick: () -> Unit,
) {
    val selectedCount = choices.count(DefinitionChoice::selected)
    val allSelected = selectedCount == choices.size
    val partial = selectedCount in 1 until choices.size
    val spec = familySpec(family)
    val accent = familyAccent(family)

    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier =
            Modifier.fillMaxWidth().semantics {
                contentDescription =
                    "${spec.label}, ${when {
                        allSelected -> "inclusa"
                        partial -> "parzialmente inclusa"
                        else -> "esclusa"
                    }}"
            },
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = RedactGuardSpacing.sm,
                    vertical = RedactGuardSpacing.xs,
                ),
            horizontalArrangement = Arrangement.spacedBy(RedactGuardSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = accent.copy(alpha = 0.12f),
                contentColor = accent,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(spec.iconRes),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(21.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(spec.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    spec.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                if (partial) {
                    Text(
                        "$selectedCount di ${choices.size} incluse",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                    )
                }
            }
            Switch(
                checked = allSelected,
                onCheckedChange = null,
            )
        }
    }
}

@Composable
private fun ProtectionProfileCard(
    profile: ProtectionProfileChoice,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val family = profileFamily(profile.id)
    val spec = familySpec(family)
    val accent = familyAccent(family)
    val border = if (profile.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    Surface(
        onClick = onClick,
        color =
            if (profile.selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            },
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(if (profile.selected) 1.5.dp else 1.dp, border),
        modifier =
            modifier.semantics {
                contentDescription =
                    if (profile.selected) "Profilo ${profile.label}, selezionato" else "Profilo ${profile.label}"
            },
    ) {
        Column(
            modifier = Modifier.padding(RedactGuardSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xxs),
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
                    modifier = Modifier.size(30.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(spec.iconRes),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                ReferenceSelectionBadge(selected = profile.selected)
            }
            Text(profile.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                profile.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun PresetSelector(
    presets: List<LocalAiPresetChoice>,
    onPresetSelect: (String) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(RedactGuardSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs),
        ) {
            Text("Modalità di analisi", style = MaterialTheme.typography.titleSmall)
            presets.forEach { preset ->
                FilterChip(
                    selected = preset.selected,
                    onClick = { onPresetSelect(preset.id) },
                    label = {
                        Column {
                            Text(preset.label)
                            preset.description?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SelectionReadinessMessage(
    connection: ConnectionBadgeModel,
    hasSelection: Boolean,
    presetReady: Boolean,
) {
    val message =
        when {
            !connection.analysisReady -> "L’analisi sarà disponibile quando l’AI locale sarà collegata."
            !hasSelection -> "Seleziona almeno una categoria per continuare."
            !presetReady -> "Seleziona una modalità di analisi per continuare."
            else -> null
        }
    message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

private data class FamilySpec(
    val label: String,
    val description: String,
    @DrawableRes val iconRes: Int,
)

private fun familySpec(family: PiiVisualFamily): FamilySpec =
    when (family) {
        PiiVisualFamily.IDENTITY -> FamilySpec("Identità", "Nomi, codici e dati identificativi", R.drawable.ic_rg_identity)
        PiiVisualFamily.CONTACT -> FamilySpec("Contatti", "Email, telefono e recapiti", R.drawable.ic_rg_contact)
        PiiVisualFamily.HEALTH -> FamilySpec("Salute", "Diagnosi, trattamenti e risultati", R.drawable.ic_rg_health)
        PiiVisualFamily.FINANCIAL -> FamilySpec("Finanziarie", "IBAN, conti e dati economici", R.drawable.ic_rg_financial)
        PiiVisualFamily.LOCATION -> FamilySpec("Luoghi", "Indirizzi e localizzazioni private", R.drawable.ic_rg_location)
        PiiVisualFamily.OTHER -> FamilySpec("Altro", "Altre informazioni sensibili", R.drawable.ic_rg_other)
    }

@Composable
private fun familyAccent(family: PiiVisualFamily): Color =
    when (family) {
        PiiVisualFamily.IDENTITY -> piiAccent("identity")
        PiiVisualFamily.CONTACT -> piiAccent("contact")
        PiiVisualFamily.HEALTH -> piiAccent("health")
        PiiVisualFamily.FINANCIAL -> piiAccent("financial")
        PiiVisualFamily.LOCATION -> piiAccent("location")
        PiiVisualFamily.OTHER -> piiAccent("other")
    }

private fun profileFamily(id: String): PiiVisualFamily =
    when (id.uppercase()) {
        "HEALTHCARE" -> PiiVisualFamily.HEALTH
        "FINANCIAL" -> PiiVisualFamily.FINANCIAL
        "LEGAL" -> PiiVisualFamily.OTHER
        else -> PiiVisualFamily.IDENTITY
    }
