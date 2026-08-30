package com.nexora.android.ui.navigation

/** Rutas de navegación (plan.md, sección 9: A1 "Navegación"). */
sealed class NexoraDestination(val route: String) {
    data object Login : NexoraDestination("login")
    data object Register : NexoraDestination("register")
    data object Dashboard : NexoraDestination("dashboard")
    data object Transactions : NexoraDestination("transactions")
    data object Cards : NexoraDestination("cards")
    data object Notifications : NexoraDestination("notifications")
}

/** Los 4 destinos del bottom nav (mismo orden que en los mockups: Dashboard, Movimientos, Tarjetas, Avisos). */
val BOTTOM_NAV_DESTINATIONS = listOf(
    NexoraDestination.Dashboard,
    NexoraDestination.Transactions,
    NexoraDestination.Cards,
    NexoraDestination.Notifications,
)
