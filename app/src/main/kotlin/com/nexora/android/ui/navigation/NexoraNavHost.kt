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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.nexora.android.data.account.AccountType
import com.nexora.android.di.AppContainer
import com.nexora.android.ui.accounts.AccountsScreen
import com.nexora.android.ui.cards.CardDetailScreen
import com.nexora.android.ui.cards.CardsScreen
import com.nexora.android.ui.components.NexoraBottomBar
import com.nexora.android.ui.components.OfflineBanner
import com.nexora.android.ui.dashboard.DashboardScreen
import com.nexora.android.ui.dashboard.MonthExpensesScreen
import com.nexora.android.ui.dashboard.QuincenaScreen
import com.nexora.android.ui.dashboard.UpcomingPaymentsScreen
import com.nexora.android.ui.forgotpassword.ForgotPasswordScreen
import com.nexora.android.ui.lock.LockScreen
import com.nexora.android.ui.login.LoginScreen
import com.nexora.android.ui.notifications.NotificationsScreen
import com.nexora.android.ui.register.RegisterScreen
import com.nexora.android.ui.resetpassword.ResetPasswordScreen
import com.nexora.android.ui.settings.SettingsScreen
import com.nexora.android.ui.transactions.MovementKind
import com.nexora.android.ui.transactions.TransactionsScreen
import kotlinx.coroutines.launch

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

    // A10: cerrar sesión también resetea isUnlocked — evita que, si el proceso sigue
    // vivo, un login posterior arranque ya "desbloqueado" en memoria por una sesión
    // anterior. Cubre cualquier camino de logout (Dashboard, LockScreen) desde un
    // único lugar, igual que la navegación a Login/Dashboard de más abajo.
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated == false) container.appLockManager.lock()
    }

    when (isAuthenticated) {
        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        true -> LockGate(container = container)
        false -> AuthenticatedAwareNavHost(container = container, startAuthenticated = false, isAuthenticated = false)
    }
}

/**
 * Gate de bloqueo (A10, plan.md sección 13): `lockEnabled && !isUnlocked` se
 * decide al mismo nivel que `isAuthenticated`, no como una ruta más del
 * NavHost — así no queda en el back stack ni un `popBackStack` lo esquiva.
 */
@Composable
private fun LockGate(container: AppContainer) {
    val lockEnabled by container.appLockManager.lockEnabled.collectAsStateWithLifecycle(initialValue = null)
    val isUnlocked by container.appLockManager.isUnlocked.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    if (lockEnabled == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    // AuthenticatedAwareNavHost queda SIEMPRE montado (mismo NavHostController, mismo
    // back stack) — LockScreen se superpone encima en vez de reemplazarlo, para no
    // perder en qué pantalla estaba el usuario cada vez que la app se bloquea/
    // desbloquea (si reemplazara al NavHost, rememberNavController() crearía uno
    // nuevo en cada desbloqueo y siempre volvería a Dashboard).
    Box(Modifier.fillMaxSize()) {
        AuthenticatedAwareNavHost(container = container, startAuthenticated = true, isAuthenticated = true)
        if (lockEnabled == true && !isUnlocked) {
            LockScreen(
                appLockManager = container.appLockManager,
                onLogout = { scope.launch { container.authRepository.logout() } },
            )
        }
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
                        onNavigateToForgotPassword = { navController.navigate(NexoraDestination.ForgotPassword.route) },
                    )
                }
                composable(NexoraDestination.Register.route) {
                    RegisterScreen(
                        authRepository = container.authRepository,
                        onNavigateBack = { navController.popBackStack() },
                    )
                }
                composable(NexoraDestination.ForgotPassword.route) {
                    ForgotPasswordScreen(
                        authRepository = container.authRepository,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToResetPassword = { email ->
                            navController.navigate(NexoraDestination.ResetPassword.routeFor(email))
                        },
                    )
                }
                composable(
                    route = NexoraDestination.ResetPassword.route,
                    arguments = listOf(navArgument("email") { type = NavType.StringType; defaultValue = "" }),
                ) { backStackEntry ->
                    val email = backStackEntry.arguments?.getString("email").orEmpty()
                    ResetPasswordScreen(
                        authRepository = container.authRepository,
                        initialEmail = email,
                        onNavigateBack = { navController.popBackStack() },
                        // Limpia el back stack igual que el logout (ver LaunchedEffect de más
                        // arriba) — el backend ya revocó las sesiones activas, no tiene
                        // sentido dejar ForgotPassword/ResetPassword/Login apiladas.
                        onNavigateToLogin = {
                            navController.navigate(NexoraDestination.Login.route) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                    )
                }
                composable(NexoraDestination.Dashboard.route) {
                    DashboardScreen(
                        dashboardRepository = container.dashboardRepository,
                        userRepository = container.userRepository,
                        authRepository = container.authRepository,
                        onNavigateToAccounts = { type -> navController.navigate(NexoraDestination.Accounts.routeFor(type)) },
                        onNavigateToNewTransaction = { kind -> navController.navigate(NexoraDestination.NewTransaction.routeFor(kind)) },
                        onNavigateToCards = { navController.navigateToTopLevel(NexoraDestination.Cards.route) },
                        onNavigateToUpcomingPayments = { navController.navigate(NexoraDestination.UpcomingPayments.route) },
                        onNavigateToMonthExpenses = { navController.navigate(NexoraDestination.MonthExpenses.route) },
                        onNavigateToQuincena = { navController.navigate(NexoraDestination.Quincena.route) },
                        onNavigateToSettings = { navController.navigate(NexoraDestination.Settings.route) },
                    )
                }
                composable(NexoraDestination.Settings.route) {
                    SettingsScreen(
                        appLockManager = container.appLockManager,
                        onNavigateBack = { navController.popBackStack() },
                    )
                }
                composable(
                    route = NexoraDestination.Accounts.route,
                    arguments = listOf(navArgument("type") { type = NavType.StringType; defaultValue = "all" }),
                ) { backStackEntry ->
                    val typeArg = backStackEntry.arguments?.getString("type")
                    val filterType = typeArg?.let { runCatching { AccountType.valueOf(it) }.getOrNull() }
                    AccountsScreen(
                        accountRepository = container.accountRepository,
                        onNavigateBack = { navController.popBackStack() },
                        filterType = filterType,
                    )
                }
                composable(NexoraDestination.UpcomingPayments.route) {
                    UpcomingPaymentsScreen(
                        dashboardRepository = container.dashboardRepository,
                        onNavigateBack = { navController.popBackStack() },
                        onCardClick = { cardId -> navController.navigate(NexoraDestination.CardDetail.routeFor(cardId)) },
                    )
                }
                composable(NexoraDestination.MonthExpenses.route) {
                    MonthExpensesScreen(
                        transactionRepository = container.transactionRepository,
                        accountRepository = container.accountRepository,
                        categoryRepository = container.categoryRepository,
                        onNavigateBack = { navController.popBackStack() },
                    )
                }
                composable(NexoraDestination.Quincena.route) {
                    QuincenaScreen(
                        dashboardRepository = container.dashboardRepository,
                        onNavigateBack = { navController.popBackStack() },
                        onCardClick = { cardId -> navController.navigate(NexoraDestination.CardDetail.routeFor(cardId)) },
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
