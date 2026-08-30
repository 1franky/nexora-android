package com.nexora.android.data.offline

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Fakes en memoria de los DAO de Room — JVM puro, sin Robolectric. */

class FakeCachedResponseDao : CachedResponseDao {
    val store = mutableMapOf<String, CachedResponseEntity>()

    override suspend fun get(key: String): CachedResponseEntity? = store[key]

    override suspend fun put(entity: CachedResponseEntity) {
        store[entity.cacheKey] = entity
    }
}

class FakePendingOperationDao : PendingOperationDao {
    val entities = mutableListOf<PendingOperationEntity>()

    override suspend fun insert(entity: PendingOperationEntity) {
        entities += entity
    }

    override suspend fun listByStatus(status: String): List<PendingOperationEntity> =
        entities.filter { it.status == status }

    override fun observeAll(): Flow<List<PendingOperationEntity>> = flowOf(entities.toList())

    override suspend fun delete(key: String) {
        entities.removeAll { it.idempotencyKey == key }
    }

    override suspend fun updateStatus(key: String, status: String, attempts: Int, error: String?) {
        val index = entities.indexOfFirst { it.idempotencyKey == key }
        if (index >= 0) entities[index] = entities[index].copy(status = status, attempts = attempts, lastError = error)
    }

    override suspend fun deleteFailed() {
        entities.removeAll { it.status == PendingOperationStatus.FAILED.name }
    }
}
