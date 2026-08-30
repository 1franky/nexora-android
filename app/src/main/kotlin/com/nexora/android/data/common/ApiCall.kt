package com.nexora.android.data.common

import java.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import retrofit2.HttpException

/** Espejo de com.nexora.api.common.web.ApiError (GlobalExceptionHandler en nexora-api). */
@Serializable
private data class ApiErrorBody(val status: Int? = null, val error: String? = null, val message: String? = null)

class ApiException(message: String) : Exception(message)

private val lenientJson = Json { ignoreUnknownKeys = true }

/**
 * Ejecuta una llamada suspend a la API, mapeando cualquier error a un
 * [ApiException] con mensaje legible en español — mismo rol que
 * getApiErrorMessage en nexora-web: si el backend mandó un `message` en el
 * cuerpo del error, se usa tal cual (ya viene en español); si no, se usa
 * [fallbackMessage].
 */
suspend fun <T> apiCall(fallbackMessage: String, block: suspend () -> T): T {
    try {
        return block()
    } catch (e: HttpException) {
        throw ApiException(extractMessage(e.response()?.errorBody()) ?: fallbackMessage)
    } catch (e: IOException) {
        throw ApiException(fallbackMessage)
    }
}

/** Visible a las funciones inline de data.offline (cachedApiCall, writeCall): mismo formato de error. */
@PublishedApi
internal fun extractMessage(errorBody: ResponseBody?): String? {
    val raw = errorBody?.string() ?: return null
    return try {
        lenientJson.decodeFromString(ApiErrorBody.serializer(), raw).message
    } catch (_: Exception) {
        null
    }
}
