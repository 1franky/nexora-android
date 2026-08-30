package com.nexora.android.data.offline

import kotlinx.serialization.json.Json

/** Envoltura de [CachedResponseDao] que serializa/deserializa con el mismo [Json] que usa Retrofit. */
class OfflineCache(@PublishedApi internal val dao: CachedResponseDao, @PublishedApi internal val json: Json) {

    suspend inline fun <reified T> put(key: String, value: T) {
        dao.put(CachedResponseEntity(key, json.encodeToString(value), System.currentTimeMillis()))
    }

    suspend inline fun <reified T> get(key: String): T? {
        val entity = dao.get(key) ?: return null
        return try {
            json.decodeFromString<T>(entity.json)
        } catch (_: Exception) {
            null
        }
    }
}
