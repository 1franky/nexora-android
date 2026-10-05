package com.nexora.android.data.scheduledcharge

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ScheduledChargeApi {
    @GET("scheduled-charges")
    suspend fun list(@Query("accountId") accountId: String? = null): List<ScheduledCharge>

    @GET("scheduled-charges/{id}")
    suspend fun get(@Path("id") id: String): ScheduledCharge

    @POST("scheduled-charges")
    suspend fun create(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: ScheduledChargeRequest,
    ): ScheduledCharge

    @PUT("scheduled-charges/{id}")
    suspend fun update(@Path("id") id: String, @Body request: ScheduledChargeRequest): ScheduledCharge

    @POST("scheduled-charges/{id}/pause")
    suspend fun pause(@Path("id") id: String): ScheduledCharge

    @POST("scheduled-charges/{id}/resume")
    suspend fun resume(@Path("id") id: String): ScheduledCharge

    /** 204: cancelación lógica — los movimientos ya generados se conservan. */
    @DELETE("scheduled-charges/{id}")
    suspend fun cancel(@Path("id") id: String)
}
