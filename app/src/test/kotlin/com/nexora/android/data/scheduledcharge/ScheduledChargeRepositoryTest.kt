package com.nexora.android.data.scheduledcharge

import com.nexora.android.data.account.AccountType
import com.nexora.android.data.common.ApiException
import com.nexora.android.data.offline.FakeCachedResponseDao
import com.nexora.android.data.offline.OfflineCache
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

private fun sample(id: String, accountId: String = "acc-1") = ScheduledCharge(
    id = id, accountId = accountId, accountName = "BBVA", accountType = AccountType.CREDIT_CARD, currency = "MXN",
    name = "Disney+", amount = 119.0, frequency = ScheduledChargeFrequency.MONTHLY, dayOfMonth = 15,
    startDate = "2026-10-15", nextRunDate = "2026-10-15", status = ScheduledChargeStatus.ACTIVE, createdAt = "2026-10-05T00:00:00Z",
)

private class FakeScheduledChargeApi : ScheduledChargeApi {
    var online = true
    var charges = listOf(sample("sc-1"), sample("sc-2", accountId = "acc-2"))
    val idempotencyKeys = mutableListOf<String>()

    private fun checkOnline() {
        if (!online) throw IOException("sin red")
    }

    override suspend fun list(accountId: String?): List<ScheduledCharge> {
        checkOnline()
        return charges.filter { accountId == null || it.accountId == accountId }
    }

    override suspend fun get(id: String): ScheduledCharge = error("no usado")

    override suspend fun create(idempotencyKey: String, request: ScheduledChargeRequest): ScheduledCharge {
        checkOnline()
        idempotencyKeys += idempotencyKey
        return sample("sc-new", request.accountId)
    }

    override suspend fun update(id: String, request: ScheduledChargeRequest): ScheduledCharge = error("no usado")

    override suspend fun pause(id: String): ScheduledCharge {
        checkOnline()
        return sample(id).copy(status = ScheduledChargeStatus.PAUSED)
    }

    override suspend fun resume(id: String): ScheduledCharge = error("no usado")

    override suspend fun cancel(id: String) = checkOnline()
}

class ScheduledChargeRepositoryTest {

    private val api = FakeScheduledChargeApi()
    private val repository = ScheduledChargeRepository(api, OfflineCache(FakeCachedResponseDao(), Json { ignoreUnknownKeys = true }))

    @Test
    fun `sin conexion la lista sale del cache, por cuenta`() = runTest {
        repository.list(null, "error")
        repository.list("acc-2", "error")
        api.online = false

        assertEquals(listOf("sc-1", "sc-2"), repository.list(null, "error").map { it.id })
        assertEquals(listOf("sc-2"), repository.list("acc-2", "error").map { it.id })
    }

    @Test
    fun `las escrituras no se encolan sin conexion`() = runTest {
        api.online = false
        val request = ScheduledChargeRequest(
            accountId = "acc-1", name = "Gym", amount = 500.0, frequency = ScheduledChargeFrequency.MONTHLY,
            dayOfMonth = 1, startDate = "2026-11-01",
        )
        for (action in listOf<suspend () -> Unit>(
            { repository.create(request, "sin conexión") },
            { repository.pause("sc-1", "sin conexión") },
            { repository.cancel("sc-1", "sin conexión") },
        )) {
            try {
                action()
                fail("debería haber lanzado ApiException")
            } catch (e: ApiException) {
                assertEquals("sin conexión", e.message)
            }
        }
    }

    @Test
    fun `el alta manda una Idempotency-Key`() = runTest {
        val request = ScheduledChargeRequest(
            accountId = "acc-1", name = "Gym", amount = 500.0, frequency = ScheduledChargeFrequency.MONTHLY,
            dayOfMonth = 1, startDate = "2026-11-01",
        )
        repository.create(request, "error")
        assertNotNull(api.idempotencyKeys.singleOrNull()?.takeIf { it.isNotBlank() })
    }
}
