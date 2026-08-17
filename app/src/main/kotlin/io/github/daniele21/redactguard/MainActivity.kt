package io.github.daniele21.redactguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import io.github.daniele21.redactguard.ui.ConnectionBadgeProjector
import io.github.daniele21.redactguard.ui.ImportScreen
import io.github.daniele21.redactguard.ui.LocalAiConnectionStatus

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ImportScreen(
                    connection = ConnectionBadgeProjector.project(LocalAiConnectionStatus.UNAVAILABLE),
                    onImportPdf = {},
                )
            }
        }
    }
}
