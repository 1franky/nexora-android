package com.nexora.android.data.offline

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Toda escritura que la app sabe encolar sin conexión (plan.md, sección 9 "A8"). */
enum class OperationType {
    CREATE_ACCOUNT,
    CREATE_CREDIT_CARD,
    CREATE_TRANSACTION,
    CREATE_TRANSFER,
    CREDIT_CARD_PURCHASE,
    CREDIT_CARD_PAYMENT,
    CREATE_INSTALLMENT_PLAN,
    PAY_INSTALLMENT,

    /** Forzar sincronización SAT (A12) — ver SatRepository.requestSync. */
    SAT_SYNC,
}

enum class PendingOperationStatus { PENDING, FAILED }

/**
 * Una escritura que no se pudo mandar al backend por falta de conexión, en
 * espera de que [com.nexora.android.sync.SyncWorker] la reintente.
 * [idempotencyKey] es tanto la clave primaria local como el valor que se
 * manda en el header `Idempotency-Key` — así un reintento nunca duplica la
 * operación del lado del servidor (ver IdempotencyFilter en nexora-api).
 *
 * A propósito no se guarda "cómo se vería el resultado" (saldo nuevo,
 * interés calculado de un plan MSI/MCI, etc.): esos números los calcula el
 * backend, y fabricarlos aquí para mostrarlos antes de tiempo arriesga
 * mostrarle al usuario una cifra financiera incorrecta. [summary] es solo
 * lo que el usuario ya escribió (monto, comercio, nombre...), para que la
 * UI pueda listar "qué hay pendiente de sincronizar" sin inventar nada.
 */
@Entity(tableName = "pending_operations")
data class PendingOperationEntity(
    @PrimaryKey val idempotencyKey: String,
    val type: String,
    val summary: String,
    val payloadJson: String,
    /** Ej. el cardId de una compra, o "planId|installmentId" de un pago de cuota. Null si la operación no necesita ninguno (ej. crear cuenta). */
    val pathParams: String? = null,
    val createdAt: Long,
    val status: String,
    val attempts: Int = 0,
    val lastError: String? = null,
)

@Dao
interface PendingOperationDao {
    @Insert
    suspend fun insert(entity: PendingOperationEntity)

    @Query("SELECT * FROM pending_operations WHERE status = :status ORDER BY createdAt ASC")
    suspend fun listByStatus(status: String = "PENDING"): List<PendingOperationEntity>

    @Query("SELECT * FROM pending_operations ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<PendingOperationEntity>>

    @Query("DELETE FROM pending_operations WHERE idempotencyKey = :key")
    suspend fun delete(key: String)

    @Query("UPDATE pending_operations SET status = :status, attempts = :attempts, lastError = :error WHERE idempotencyKey = :key")
    suspend fun updateStatus(key: String, status: String, attempts: Int, error: String?)

    @Query("DELETE FROM pending_operations WHERE status = 'FAILED'")
    suspend fun deleteFailed()
}
