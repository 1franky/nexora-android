package com.nexora.android.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nexora.android.di.AppContainer
import com.nexora.android.ui.accounts.AccountsScreen
import com.nexora.android.ui.cards.CardDetailScreen
import com.nexora.android.ui.cards.CardsScreen
import com.nexora.android.ui.components.NexoraBottomBar
import com.nexora.android.ui.components.OfflineBanner
import com.nexora.android.ui.dashboard.DashboardScreen
import com.nexora.android.ui.login.LoginScreen
import com.nexora.android.ui.notifications.NotificationsScreen
import com.nexora.android.ui.register.RegisterScreen
import com.nexora.android.ui.transactions.MovementKind
import com.nexora.android.ui.transactions.TransactionsScreen

/**
 * Navega a uno de los destinos "de nivel superior" (los del bottom nav, y
 * Cards como destino de las acciones rápidas "Comprar"/"Pagar" del
 * dashboard) de forma robusta.
 *
 * Si el destino ya está vivo en el back stack — p.ej. se volvió a Tarjetas
 * con la flecha "atrás" desde el detalle de una tarjeta — un `navigate()`
 * con `popUpTo(startDestination){saveState=true}; launchSingleTop=true;
 * restoreState=true` (el patrón estándar de Compose Navigation para bottom
 * nav) no lo detecta: ese `popUpTo` sí quita las pantallas de encima, pero
 * `launchSingleTop`/`restoreState` solo reutilizan la entrada de destino si
 * coincide con el *tope* del stack en ese momento — y en este caso el tope
 * ya es el propio destino buscado, así que Compose Navigation lo trata como
 * "ya estás aquí" y no dispara la recomposición que muestra la pantalla.
 * Resultado (reproducido a mano): Dashboard -> Tarjetas -> detalle de
 * tarjeta -> atrás -> tocar "Dashboard" en el bottom nav no hacía nada; solo
 * el gesto/botón de atrás del sistema regresaba al dashboard.
 *
 * `popBackStack(route, inclusive = false)` sí resuelve ese caso (es
 * exactamente lo que hace el botón de atrás del sistema). Si el destino NO
 * está vivo en el stack (primera visita, o su estado fue guardado y
 * removido por un cambio de pestaña anterior), `popBackStack` devuelve
 * `false` sin tocar el stack, y se cae al patrón estándar de arriba.
 */
private fun NavHostController.navigateToTopLevel(route: String) {
    val poppedToExisting = popBackStack(route, false)
    if (!poppedToExisting) {
        navigate(route) {
            popUpTo(graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}

/**
 * Un único NavHost; qué se ve depende de isAuthenticated (fuente de verdad:
 * TokenStore vía AuthRepository, ver AppContainer). Ninguna pantalla navega
 * "a Dashboard" o "a Login" por sí misma tras loguearse/cerrar sesión — solo
 * cambia el estado en el repositorio, y este efecto reacciona.
 */
@Composable
fun NexoraNavHost(container: AppContainer) {
    val isAuthenticated by container.authRepository.isAuthenticated.collectAsStateWithLifecycle(initialValue = null)

    when (isAuthenticated) {
        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else -> AuthenticatedAwareNavHost(container = container, startAuthenticated = isAuthenticated == true, isAuthenticated = isAuthenticated)
    }
}

@Composable
private fun AuthenticatedAwareNavHost(
    container: AppContainer,
    startAuthenticated: Boolean,
    isAuthenticated: Boolean?,
    navController: NavHostController = rememberNavController(),
) {
    LaunchedEffect(isAuthenticated) {
        when (isAuthenticated) {
            true -> navController.navigate(NexoraDestination.Dashboard.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
            false -> navController.navigate(NexoraDestination.Login.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
            null -> Unit
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = BOTTOM_NAV_DESTINATIONS.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NexoraBottomBar(currentRoute = currentRoute) { destination ->
                    navController.navigateToTopLevel(destination.route)
                }
            }
        },
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding).fillMaxSize()) {
            if (isAuthenticated == true) {
                OfflineBanner(connectivityObserver = container.connectivityObserver, pendingOperationDao = container.pendingOperationDao)
            }
            NavHost(
                navController = navController,
                startDestination = if (startAuthenticated) NexoraDestination.Dashboard.route else NexoraDestination.Login.route,
                modifier = Modifier.weight(1f),
            ) {
                composable(NexoraDestination.Login.route) {
                    LoginScreen(
                        authRepository = container.authRepository,
                        onNavigateToRegister = { navController.navigate(NexoraDestination.Register.route) },
                    )
                }
                composable(NexoraDestination.Register.route) {
                    RegisterScreen(
                        authRepository = container.authRepository,
                        onNavigateBack = { navController.popBackStack() },
                    )
                }
                composable(NexoraDestination.Dashboard.route) {
                    DashboardScreen(
                        dashboardRepository = container.dashboardRepository,
                        userRepository = container.userRepository,
                        authRepository = container.authRepository,
                        onNavigateToAccounts = { navController.navigate(NexoraDestination.Accounts.route) },
                        onNavigateToNewTransaction = { kind -> navController.navigate(NexoraDestination.NewTransaction.routeFor(kind)) },
                        onNavigateToCards = { navController.navigateToTopLevel(NexoraDestination.Cards.route) },
                    )
                }
                composable(NexoraDestination.Accounts.route) {
                    AccountsScreen(
                        accountRepository = container.accountRepository,
                        onNavigateBack = { navController.popBackStack() },
                    )
                }
                composable(NexoraDestination.Transactions.route) {
                    TransactionsScreen(
                        transactionRepository = container.transactionRepository,
                        accountRepository = container.accountRepository,
                        categoryRepository = container.categoryRepository,
                    )
                }
                composable(
                    route = NexoraDestination.NewTransaction.route,
                    arguments = listOf(navArgument("kind") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val kind = backStackEntry.arguments?.getString("kind")?.let {
                        runCatching { MovementKind.valueOf(it) }.getOrDefault(MovementKind.EXPENSE)
                    } ?: MovementKind.EXPENSE
                    TransactionsScreen(
                        transactionRepository = container.transactionRepository,
                        accountRepository = container.accountRepository,
                        categoryRepository = container.categoryRepository,
                        initialKind = kind,
                    )
                }
                composable(NexoraDestination.Cards.route) {
                    CardsScreen(
                        creditCardRepository = container.creditCardRepository,
                        onCardClick = { cardId -> navController.navigate(NexoraDestination.CardDetail.routeFor(cardId)) },
                    )
                }
                composable(
                    route = NexoraDestination.CardDetail.route,
                    arguments = listOf(navArgument("cardId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val cardId = backStackEntry.arguments?.getString("cardId")
                    if (cardId != null) {
                        CardDetailScreen(
                            cardId = cardId,
                            creditCardRepository = container.creditCardRepository,
                            transactionRepository = container.transactionRepository,
                            categoryRepository = container.categoryRepository,
                            accountRepository = container.accountRepository,
                            installmentRepository = container.installmentRepository,
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }
                }
                composable(NexoraDestination.Notifications.route) {
                    NotificationsScreen(notificationRepository = container.notificationRepository)
                }
            }
        }
    }
}
