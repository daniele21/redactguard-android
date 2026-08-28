@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package io.github.daniele21.redactguard.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.daniele21.redactguard.R
import io.github.daniele21.redactguard.ui.theme.RedactGuardSpacing

@Composable
internal fun ImportScreen(
    connection: ConnectionBadgeModel,
    onImportPdf: () -> Unit,
    onPasteText: () -> Unit,
) {
    RedactGuardScaffold(step = "Documento", connection = connection) {
        ProductPanel {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RedactGuardSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs),
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            "PROTEZIONE LOCALE",
                            style = MaterialTheme.typography.labelMedium,
                            modifier =
                                Modifier.padding(
                                    horizontal = RedactGuardSpacing.xs,
                                    vertical = RedactGuardSpacing.xxs,
                                ),
                        )
                    }
                    Text(
                        "Proteggi i tuoi documenti.",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Trova e rivedi i dati sensibili senza inviarli nel cloud.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.size(96.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(R.drawable.redactguard_mark),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(70.dp),
                        )
                    }
                }
            }

            ReferenceActionCard(
                title = "Importa un PDF",
                subtitle = "PDF con testo · elaborazione locale",
                iconRes = R.drawable.ic_rg_document,
                onClick = onImportPdf,
                emphasized = true,
            )
            ReferenceActionCard(
                title = "Incolla testo",
                subtitle = "Analizza testo negli appunti",
                iconRes = R.drawable.ic_rg_clipboard,
                onClick = onPasteText,
                accent = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(RedactGuardSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xxs),
                ) {
                    Text(
                        "Solo sul dispositivo",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Importazione, analisi e revisione restano locali. " +
                            "Sono supportati PDF con testo estraibile; scansioni e PDF solo " +
                            "immagine richiedono OCR e non vengono elaborati implicitamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
        ProductPanel(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.sm),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.size(118.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(R.drawable.redactguard_mark),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(86.dp),
                        )
                    }
                }
                Text(
                    "Analisi in corso",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    "Il documento viene analizzato completamente in locale.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LinearProgressIndicator(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription =
                                "Ricerca locale dei dati sensibili in corso"
                        },
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.sm),
            ) {
                ReferencePhaseRow(
                    "Documento preparato",
                    ReferencePhaseState.DONE,
                )
                ReferencePhaseRow(
                    "Ricerca dati sensibili",
                    ReferencePhaseState.ACTIVE,
                )
                ReferencePhaseRow(
                    "Validazione risultati",
                    ReferencePhaseState.PENDING,
                )
            }

            Text(
                "Non mostriamo una percentuale stimata: la revisione si apre solo " +
                    "quando l’analisi è completata e validata. Nessun risultato " +
                    "parziale viene promosso come valido.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Annulla analisi")
        }
    }
}
