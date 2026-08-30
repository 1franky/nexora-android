package com.nexora.android.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.nexora.android.R
import com.nexora.android.ui.navigation.BOTTOM_NAV_DESTINATIONS
import com.nexora.android.ui.navigation.NexoraDestination

private fun iconFor(destination: NexoraDestination): ImageVector = when (destination) {
    NexoraDestination.Dashboard -> Icons.Outlined.Dashboard
    NexoraDestination.Transactions -> Icons.AutoMirrored.Outlined.List
    NexoraDestination.Cards -> Icons.Outlined.CreditCard
    else -> Icons.Outlined.Notifications
}

private fun labelResFor(destination: NexoraDestination): Int = when (destination) {
    NexoraDestination.Dashboard -> R.string.nav_dashboard
    NexoraDestination.Transactions -> R.string.nav_transactions
    NexoraDestination.Cards -> R.string.nav_cards
    else -> R.string.nav_notifications
}

@Composable
fun NexoraBottomBar(currentRoute: String?, onSelect: (NexoraDestination) -> Unit) {
    NavigationBar {
        BOTTOM_NAV_DESTINATIONS.forEach { destination ->
            val label = stringResource(labelResFor(destination))
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { onSelect(destination) },
                icon = { Icon(iconFor(destination), contentDescription = label) },
                label = { Text(label) },
            )
        }
    }
}
