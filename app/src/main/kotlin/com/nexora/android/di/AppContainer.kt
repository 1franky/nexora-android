package com.nexora.android.di

import android.content.Context
import com.nexora.android.BuildConfig
import com.nexora.android.data.auth.AuthApi
import com.nexora.android.data.auth.AuthAuthenticator
import com.nexora.android.data.auth.AuthInterceptor
import com.nexora.android.data.auth.AuthRepository
import com.nexora.android.data.auth.TokenStore
import com.nexora.android.data.account.AccountApi
import com.nexora.android.data.account.AccountRepository
import com.nexora.android.data.category.CategoryApi
import com.nexora.android.data.category.CategoryRepository
import com.nexora.android.data.creditcard.CreditCardApi
import com.nexora.android.data.creditcard.CreditCardRepository
import com.nexora.android.data.dashboard.DashboardApi
import com.nexora.android.data.dashboard.DashboardRepository
import com.nexora.android.data.installment.InstallmentApi
import com.nexora.android.data.installment.InstallmentRepository
import com.nexora.android.data.notification.NotificationApi
import com.nexora.android.data.notification.NotificationRepository
import com.nexora.android.data.transaction.TransactionApi
import com.nexora.android.data.transaction.TransactionRepository
import com.nexora.android.data.user.UserRepository
import com.nexora.android.data.user.UsersApi
import com.nexora.android.ui.theme.ThemePreference
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

/**
 * Contenedor de dependencias manual, sin Hilt: para el tamaño actual del
 * proyecto evita la sobrecarga de KSP/codegen (y el riesgo de una primera
 * compilación fallida por una dependencia beta) a cambio de un poco de
 * cableado explícito aquí. Migrar a Hilt más adelante es un cambio
 * localizado a este archivo si el proyecto lo justifica.
 */
class AppContainer(context: Context) {

    val tokenStore = TokenStore(context.applicationContext)
    val themePreference = ThemePreference(context.applicationContext)

    private val json = Json { ignoreUnknownKeys = true }
    private val jsonConverterFactory = json.asConverterFactory("application/json".toMediaType())

    private fun loggingInterceptor() = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }

    // Cliente "pelado", sin AuthInterceptor ni AuthAuthenticator: lo usa únicamente
    // refreshApi, para que refrescar el token nunca pueda disparar una llamada circular.
    private val plainOkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor())
        .build()

    private val refreshRetrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(plainOkHttpClient)
        .addConverterFactory(jsonConverterFactory)
        .build()

    private val refreshApi: AuthApi = refreshRetrofit.create()

    private val authenticatedOkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(tokenStore))
        .addInterceptor(loggingInterceptor())
        .authenticator(AuthAuthenticator(tokenStore, refreshApi))
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(authenticatedOkHttpClient)
        .addConverterFactory(jsonConverterFactory)
        .build()

    private val authApi: AuthApi = retrofit.create()
    private val usersApi: UsersApi = retrofit.create()
    private val dashboardApi: DashboardApi = retrofit.create()
    private val accountApi: AccountApi = retrofit.create()
    private val categoryApi: CategoryApi = retrofit.create()
    private val transactionApi: TransactionApi = retrofit.create()
    private val creditCardApi: CreditCardApi = retrofit.create()
    private val installmentApi: InstallmentApi = retrofit.create()
    private val notificationApi: NotificationApi = retrofit.create()

    val authRepository = AuthRepository(authApi, usersApi, tokenStore)
    val userRepository = UserRepository(usersApi)
    val dashboardRepository = DashboardRepository(dashboardApi)
    val accountRepository = AccountRepository(accountApi)
    val categoryRepository = CategoryRepository(categoryApi)
    val transactionRepository = TransactionRepository(transactionApi)
    val creditCardRepository = CreditCardRepository(creditCardApi)
    val installmentRepository = InstallmentRepository(installmentApi)
    val notificationRepository = NotificationRepository(notificationApi)
}
