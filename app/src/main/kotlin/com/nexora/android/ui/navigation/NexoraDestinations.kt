package com.nexora.android.ui.navigation

import com.nexora.android.ui.transactions.MovementKind

/** Rutas de navegación (plan.md, sección 9: A1 "Navegación"). */
sealed class NexoraDestination(val route: String) {
    data object Login : NexoraDestination("login")
    data object Register : NexoraDestination("register")
    data object Dashboard : NexoraDestination("dashboard")
    data object Accounts : NexoraDestination("accounts")
    data object Transactions : NexoraDestination("transactions")
    data object Cards : NexoraDestination("cards")
    data object Notifications : NexoraDestination("notifications")

    /** Detalle de una tarjeta — no es un destino fijo, se arma con el id. */
    data object CardDetail : NexoraDestination("cards/{cardId}") {
        fun routeFor(cardId: String) = "cards/$cardId"
    }

    /**
     * Movimientos, pero abriendo directo la hoja de "nuevo movimiento" con un tipo
     * preseleccionado — usado por las acciones rápidas del dashboard (ingreso/gasto/
     * transferir). No es el mismo destino que [Transactions] a propósito: mantiene el
     * comparado de ruta exacta que usa el bottom nav (BOTTOM_NAV_DESTINATIONS) simple,
     * sin argumentos opcionales.
     */
    data object NewTransaction : NexoraDestination("transactions/new/{kind}") {
        fun routeFor(kind: MovementKind) = "transactions/new/${kind.name}"
    }
}

/** Los 4 destinos del bottom nav (mismo orden que en los mockups: Dashboard, Movimientos, Tarjetas, Avisos). */
val BOTTOM_NAV_DESTINATIONS = listOf(
    NexoraDestination.Dashboard,
    NexoraDestination.Transactions,
    NexoraDestination.Cards,
    NexoraDestination.Notifications,
)
