package com.nexora.android.data.offline

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

/**
 * Última respuesta buena conocida de un GET, guardada como JSON crudo bajo
 * la clave que arma cada repositorio (ver [OfflineCache]). Una sola tabla
 * genérica en vez de una tabla por tipo de dato: el esquema real (Account,
 * CreditCard, Transaction, InstallmentPlan...) ya vive en los DTOs de
 * kotlinx.serialization que consume el resto de la app — no hace falta
 * duplicarlo en entidades Room con relaciones y TypeConverters.
 */
@Entity(tableName = "cached_responses")
data class CachedResponseEntity(
    @PrimaryKey val cacheKey: String,
    val json: String,
    val updatedAt: Long,
)

@Dao
interface CachedResponseDao {
    @Query("SELECT * FROM cached_responses WHERE cacheKey = :key")
    suspend fun get(key: String): CachedResponseEntity?

    @Upsert
    suspend fun put(entity: CachedResponseEntity)
}
