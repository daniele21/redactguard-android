@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package io.github.daniele21.redactguard.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.daniele21.redactguard.domain.analysis.AnalysisPromptIssue
import io.github.daniele21.redactguard.domain.analysis.AnalysisPromptValidation
import io.github.daniele21.redactguard.ui.theme.RedactGuardSpacing

internal data class AnalysisPromptSettingsUiModel(
    val currentPrompt: String,
    val isCustom: Boolean,
    val defaultPrompt: String,
    val protectedRules: String,
    val maxCharacters: Int,
)

@Composable
internal fun AnalysisPromptSettingsCard(
    model: AnalysisPromptSettingsUiModel,
    onEdit: () -> Unit,
) {
    ProductPanel {
        Column(verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Istruzioni per il rilevamento",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Guidano l'AI locale nel riconoscimento delle informazioni da proteggere.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                PromptModeBadge(isCustom = model.isCustom)
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier =
                    Modifier.fillMaxWidth().semantics {
                        contentDescription =
                            if (model.isCustom) {
                                "Prompt di analisi personalizzato. Anteprima delle istruzioni correnti."
                            } else {
                                "Prompt di analisi predefinito. Anteprima delle istruzioni correnti."
                            }
                    },
            ) {
                Text(
                    text = model.currentPrompt,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(RedactGuardSpacing.sm),
                )
            }

            Text(
                "RedactGuard aggiunge sempre regole di integrità protette: ID e testo devono corrispondere esattamente al documento e i tipi assenti devono essere omessi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Un prompt personalizzato resta solo nelle preferenze private dell'app e si applica dalla prossima analisi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                Text("Visualizza e modifica")
            }
        }
    }
}

@Composable
private fun PromptModeBadge(isCustom: Boolean) {
    Surface(
        color = if (isCustom) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = if (isCustom) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            if (isCustom) "Personalizzato" else "Predefinito",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = RedactGuardSpacing.sm, vertical = RedactGuardSpacing.xxs),
        )
    }
}

@Composable
internal fun AnalysisPromptEditorDialog(
    model: AnalysisPromptSettingsUiModel,
    validate: (String) -> AnalysisPromptValidation,
    onSave: (String) -> Boolean,
    onDismiss: () -> Unit,
) {
    var draft by remember(model.currentPrompt) { mutableStateOf(model.currentPrompt) }
    var protectedRulesVisible by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    val validation = validate(draft)
    val hasChanges = validation.normalized != model.currentPrompt
    val error = promptValidationError(validation)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .imePadding()
                        .padding(horizontal = RedactGuardSpacing.md, vertical = RedactGuardSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.sm),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) { Text("Annulla") }
                    Text(
                        "Prompt di analisi",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Button(
                        onClick = {
                            saveFailed = !onSave(draft)
                            if (!saveFailed) onDismiss()
                        },
                        enabled = validation.isValid && hasChanges,
                    ) {
                        Text("Salva")
                    }
                }

                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.sm),
                ) {
                    Text(
                        "Personalizza il modo in cui RedactGuard chiede all'AI locale di rilevare le informazioni. Le modifiche valgono dalla prossima analisi e non cambiano un'analisi già in corso.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Evita di inserire qui contenuti del documento, password o altri segreti: questo testo viene salvato come preferenza locale finché non lo modifichi o ripristini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    OutlinedTextField(
                        value = draft,
                        onValueChange = {
                            draft = it
                            saveFailed = false
                        },
                        label = { Text("Istruzioni modificabili") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp),
                        minLines = 12,
                        isError = error != null || saveFailed,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            error ?: if (saveFailed) "Impossibile salvare il prompt." else "Prompt modificabile dall'utente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (error != null || saveFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "${validation.normalized.length}/${model.maxCharacters}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (validation.normalized.length > model.maxCharacters) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            draft = model.defaultPrompt
                            saveFailed = false
                        },
                        enabled = validation.normalized != model.defaultPrompt,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Ripristina predefinito")
                    }

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
                            Text(
                                "Regole di integrità sempre applicate",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "Restano attive anche con un prompt personalizzato. Impediscono di considerare valido un risultato con categorie inventate, valori non presenti nel testo o placeholder per dati assenti.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(onClick = { protectedRulesVisible = !protectedRulesVisible }) {
                                Text(if (protectedRulesVisible) "Nascondi regole protette" else "Mostra regole protette")
                            }
                            if (protectedRulesVisible) {
                                Text(
                                    model.protectedRules,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    modifier =
                                        Modifier.semantics {
                                            contentDescription = "Regole di integrità protette e non modificabili di RedactGuard"
                                        },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun promptValidationError(validation: AnalysisPromptValidation): String? =
    when {
        AnalysisPromptIssue.EMPTY in validation.issues -> "Il prompt non può essere vuoto."
        AnalysisPromptIssue.TOO_LONG in validation.issues -> "Il prompt supera il limite massimo consentito."
        AnalysisPromptIssue.UNSUPPORTED_CONTROL_CHARACTER in validation.issues -> "Il prompt contiene caratteri di controllo non supportati."
        else -> null
    }
