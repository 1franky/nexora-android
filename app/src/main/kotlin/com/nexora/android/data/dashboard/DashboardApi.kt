package com.nexora.android.data.dashboard

import retrofit2.http.GET
import retrofit2.http.Query

interface DashboardApi {
    /** Sin [month], el backend usa el mes actual (igual que nexora-web). */
    @GET("dashboard")
    suspend fun getDashboard(@Query("month") month: String? = null): DashboardResponse
}
