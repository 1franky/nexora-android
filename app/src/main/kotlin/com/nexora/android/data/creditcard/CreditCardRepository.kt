package com.nexora.android.data.creditcard

import com.nexora.android.data.common.apiCall
import com.nexora.android.data.transaction.Transaction
import com.nexora.android.data.transaction.TransferResult

class CreditCardRepository(private val creditCardApi: CreditCardApi) {
    suspend fun listCreditCards(fallbackError: String): List<CreditCard> =
        apiCall(fallbackError) { creditCardApi.listCreditCards() }

    suspend fun createCreditCard(request: CreateCreditCardRequest, fallbackError: String): CreditCard =
        apiCall(fallbackError) { creditCardApi.createCreditCard(request) }

    suspend fun getCreditCard(id: String, fallbackError: String): CreditCard =
        apiCall(fallbackError) { creditCardApi.getCreditCard(id) }

    suspend fun purchase(id: String, request: CreditCardPurchaseRequest, fallbackError: String): Transaction =
        apiCall(fallbackError) { creditCardApi.purchase(id, request) }

    suspend fun pay(id: String, request: CreditCardPaymentRequest, fallbackError: String): TransferResult =
        apiCall(fallbackError) { creditCardApi.pay(id, request) }
}
