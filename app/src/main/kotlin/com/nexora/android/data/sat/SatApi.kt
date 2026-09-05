package com.nexora.android.data.sat

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface SatApi {

    /** Puede tardar unos segundos: valida contra el SAT real antes de responder (ver plan-integracion-sat.md, sección 6). */
    @Multipart
    @POST("sat/certificate")
    suspend fun connectCertificate(
        @Part cer: MultipartBody.Part,
        @Part key: MultipartBody.Part,
        @Query("password") password: String,
    ): SatCertificateResponse

    /** 404 si el usuario no tiene ninguna e.firma conectada — se traduce a null en SatRepository. */
    @GET("sat/certificate")
    suspend fun getCertificateStatus(): SatCertificateResponse

    /** 200 sin cuerpo — borra el material sensible, conserva las facturas ya descargadas. */
    @DELETE("sat/certificate")
    suspend fun deleteCertificate()

    /**
     * 202: la sync corre en background del lado del backend, no hay que esperar
     * a que termine — el usuario se entera por notificaciones. Sin `desde`/`hasta`
     * = incremental; con ambos = rango explícito para traer historial antiguo.
     */
    @POST("sat/sync")
    suspend fun sync(@Body request: SyncRequest)

    @GET("sat/invoices")
    suspend fun listInvoices(
        @Query("tipo") tipo: String? = null,
        @Query("desde") desde: String? = null,
        @Query("hasta") hasta: String? = null,
        @Query("texto") texto: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 25,
    ): PageCfdiInvoiceResponse

    /** application/xml crudo con Content-Disposition: attachment — @Streaming evita bufferear todo el cuerpo en memoria antes de tiempo. */
    @Streaming
    @GET("sat/invoices/{id}/xml")
    suspend fun downloadXml(@Path("id") id: String): ResponseBody

    @GET("sat/contrapartes")
    suspend fun listContrapartes(): List<SatContraparteResponse>

    /** 400 con `message` en español si el RFC tiene formato inválido o ya está registrado. */
    @POST("sat/contrapartes")
    suspend fun createContraparte(@Body request: CreateSatContraparteRequest): SatContraparteResponse

    @DELETE("sat/contrapartes/{id}")
    suspend fun deleteContraparte(@Path("id") id: String)
}
