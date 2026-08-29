@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package io.github.daniele21.redactguard.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs),
            ) {
                Text(
                    "Proteggi i tuoi documenti.",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Trova e rivedi i dati sensibili senza inviarli nel cloud.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ConnectionBadge(connection)
            }
            Image(
                painter = painterResource(R.drawable.rg_hero_document_shield),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(104.dp),
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xs),
        ) {
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
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier =
                    Modifier.padding(
                        horizontal = RedactGuardSpacing.sm,
                        vertical = RedactGuardSpacing.xs,
                    ),
                verticalArrangement = Arrangement.spacedBy(RedactGuardSpacing.xxs),
            ) {
                Text(
                    "Elaborazione 100% locale",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    "I tuoi dati non lasciano il dispositivo. Scansioni e PDF solo immagine non vengono elaborati senza una funzione OCR dedicata.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
