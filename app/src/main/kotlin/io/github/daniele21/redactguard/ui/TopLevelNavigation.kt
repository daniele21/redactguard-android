@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package io.github.daniele21.redactguard.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import io.github.daniele21.redactguard.R

internal enum class RedactGuardTopLevelDestination(
    val label: String,
    @DrawableRes val iconRes: Int,
) {
    ANALYZE("Analizza", R.drawable.ic_rg_document),
    LOCAL_AI("AI locale", R.drawable.ic_rg_ai_local),
    SETTINGS("Impostazioni", R.drawable.ic_rg_settings),
}

internal enum class TopLevelNavigationMode {
    BOTTOM_BAR,
    RAIL,
}

internal fun topLevelNavigationMode(windowClass: ProductWindowClass): TopLevelNavigationMode =
    when (windowClass) {
        ProductWindowClass.COMPACT -> TopLevelNavigationMode.BOTTOM_BAR

        ProductWindowClass.MEDIUM,
        ProductWindowClass.EXPANDED,
        -> TopLevelNavigationMode.RAIL
    }

@Composable
internal fun RedactGuardAppShell(
    windowClass: ProductWindowClass,
    currentDestination: RedactGuardTopLevelDestination,
    onDestinationSelected: (RedactGuardTopLevelDestination) -> Unit,
    content: @Composable () -> Unit,
) {
    when (topLevelNavigationMode(windowClass)) {
        TopLevelNavigationMode.BOTTOM_BAR -> {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    RedactGuardNavigationBar(
                        currentDestination = currentDestination,
                        onDestinationSelected = onDestinationSelected,
                    )
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    content()
                }
            }
        }

        TopLevelNavigationMode.RAIL -> {
            Row(modifier = Modifier.fillMaxSize()) {
                RedactGuardNavigationRail(
                    currentDestination = currentDestination,
                    onDestinationSelected = onDestinationSelected,
                )
                Box(
                    modifier = Modifier.fillMaxHeight().fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun RedactGuardNavigationBar(
    currentDestination: RedactGuardTopLevelDestination,
    onDestinationSelected: (RedactGuardTopLevelDestination) -> Unit,
) {
    NavigationBar {
        RedactGuardTopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = destination == currentDestination,
                onClick = { onDestinationSelected(destination) },
                icon = { DestinationIcon(destination) },
                label = { androidx.compose.material3.Text(destination.label) },
            )
        }
    }
}

@Composable
private fun RedactGuardNavigationRail(
    currentDestination: RedactGuardTopLevelDestination,
    onDestinationSelected: (RedactGuardTopLevelDestination) -> Unit,
) {
    NavigationRail {
        RedactGuardTopLevelDestination.entries.forEach { destination ->
            NavigationRailItem(
                selected = destination == currentDestination,
                onClick = { onDestinationSelected(destination) },
                icon = { DestinationIcon(destination) },
                label = { androidx.compose.material3.Text(destination.label) },
                alwaysShowLabel = true,
            )
        }
    }
}

@Composable
private fun DestinationIcon(destination: RedactGuardTopLevelDestination) {
    Icon(
        painter = painterResource(destination.iconRes),
        contentDescription = null,
    )
}
