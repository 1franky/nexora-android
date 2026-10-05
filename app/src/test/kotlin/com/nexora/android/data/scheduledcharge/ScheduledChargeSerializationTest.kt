package com.nexora.android.data.scheduledcharge

import com.nexora.android.data.account.AccountType
import com.nexora.android.data.notification.Notification
import com.nexora.android.data.notification.NotificationType
import com.nexora.android.data.transaction.Transaction
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Compatibilidad del contrato B14 con el mismo Json que usa Retrofit (AppContainer). */
class ScheduledChargeSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parsea un ScheduledChargeResponse de nexora-api`() {
        val raw = """
            {"id":"sc-1","accountId":"acc-1","accountName":"BBVA Azul","accountType":"CREDIT_CARD","currency":"MXN",
             "name":"Disney+","amount":119.00,"categoryId":null,"description":null,"frequency":"MONTHLY",
             "dayOfMonth":15,"monthOfYear":null,"startDate":"2026-10-15","endDate":null,"nextRunDate":"2026-10-15",
             "lastRunDate":null,"status":"ACTIVE","lastError":null,"createdAt":"2026-10-05T18:00:00.123Z"}
        """.trimIndent()
        val charge = json.decodeFromString<ScheduledCharge>(raw)
        assertEquals(AccountType.CREDIT_CARD, charge.accountType)
        assertEquals(119.0, charge.amount, 0.0)
        assertEquals(ScheduledChargeStatus.ACTIVE, charge.status)
        assertNull(charge.monthOfYear)
    }

    @Test
    fun `transaccion sin scheduledChargeId (backend anterior a B14) sigue parseando`() {
        val raw = """{"id":"t1","accountId":"a1","type":"EXPENSE","amount":10,"balanceEffect":-10,"date":"2026-10-05"}"""
        assertNull(json.decodeFromString<Transaction>(raw).scheduledChargeId)
    }

    @Test
    fun `transaccion generada por un cargo programado trae su id`() {
        val raw = """{"id":"t1","accountId":"a1","type":"CREDIT_CARD_PURCHASE","amount":119,"balanceEffect":-119,
            "date":"2026-10-15","merchant":"Disney+","scheduledChargeId":"sc-1","campoFuturo":true}"""
        assertEquals("sc-1", json.decodeFromString<Transaction>(raw).scheduledChargeId)
    }

    @Test
    fun `tipos de notificacion nuevos y desconocidos no rompen el parseo`() {
        val raw = """[
            {"id":"n1","type":"SCHEDULED_CHARGE_POSTED","title":"t","message":"m","relatedEntityId":"sc-1","status":"UNREAD","createdAt":"2026-10-05T00:05:00Z"},
            {"id":"n2","type":"SCHEDULED_CHARGE_FAILED","title":"t","message":"m","status":"READ","createdAt":"2026-10-05T00:05:00Z"},
            {"id":"n3","type":"ALGO_QUE_AUN_NO_EXISTE","title":"t","message":"m","status":"UNREAD","createdAt":"2026-10-05T00:05:00Z"}
        ]"""
        val notifications = json.decodeFromString<List<Notification>>(raw)
        assertEquals(
            listOf(NotificationType.SCHEDULED_CHARGE_POSTED, NotificationType.SCHEDULED_CHARGE_FAILED, NotificationType.UNKNOWN),
            notifications.map { it.type },
        )
    }

    @Test
    fun `request mensual se serializa con monthOfYear null`() {
        val request = ScheduledChargeRequest(
            accountId = "acc-1", name = "Gym", amount = 500.0, frequency = ScheduledChargeFrequency.MONTHLY,
            dayOfMonth = 1, startDate = "2026-11-01",
        )
        val encoded = Json { encodeDefaults = true }.encodeToString(ScheduledChargeRequest.serializer(), request)
        assertEquals(true, encoded.contains("\"monthOfYear\":null"))
        assertEquals(true, encoded.contains("\"frequency\":\"MONTHLY\""))
    }
}
