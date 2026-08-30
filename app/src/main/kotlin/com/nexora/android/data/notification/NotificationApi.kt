package com.nexora.android.data.notification

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface NotificationApi {
    /** GET regenera al vuelo lo que falte para el usuario antes de devolver la lista (ver NotificationService en nexora-api). */
    @GET("notifications")
    suspend fun list(): List<Notification>

    @POST("notifications/{id}/read")
    suspend fun markAsRead(@Path("id") id: String): Notification

    @POST("notifications/read-all")
    suspend fun markAllAsRead()
}
