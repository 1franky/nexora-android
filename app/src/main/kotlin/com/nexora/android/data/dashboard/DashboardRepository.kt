package com.nexora.android.data.dashboard

import com.nexora.android.data.common.apiCall

class DashboardRepository(private val dashboardApi: DashboardApi) {
    suspend fun getDashboard(fallbackError: String): DashboardResponse =
        apiCall(fallbackError) { dashboardApi.getDashboard() }
}
