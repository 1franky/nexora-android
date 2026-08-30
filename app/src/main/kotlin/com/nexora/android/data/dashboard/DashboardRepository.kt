package com.nexora.android.data.dashboard

import com.nexora.android.data.offline.OfflineCache
import com.nexora.android.data.offline.cachedApiCall

private const val CACHE_KEY_DASHBOARD = "dashboard"

class DashboardRepository(
    private val dashboardApi: DashboardApi,
    private val offlineCache: OfflineCache,
) {
    suspend fun getDashboard(fallbackError: String): DashboardResponse =
        cachedApiCall(offlineCache, CACHE_KEY_DASHBOARD, fallbackError) { dashboardApi.getDashboard() }
}
