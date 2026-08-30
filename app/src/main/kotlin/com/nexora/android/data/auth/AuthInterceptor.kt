package com.nexora.android.data.auth

import okhttp3.Interceptor
import okhttp3.Response

/** Agrega el access token a toda petición que tenga uno guardado; los endpoints públicos simplemente lo ignoran. */
class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val accessToken = tokenStore.getTokensBlocking()?.accessToken
            ?: return chain.proceed(request)

        val authenticated = request.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()
        return chain.proceed(authenticated)
    }
}
