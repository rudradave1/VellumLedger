package com.vellum.ledger.sync

import com.vellum.ledger.domain.LedgerTransaction
import kotlinx.coroutines.delay
import kotlin.random.Random

class FakeLedgerApi(
    private val randomFail: Boolean = true,
) {
    suspend fun push(transaction: LedgerTransaction) {
        delay(1_000)
        if (randomFail && Random.nextFloat() < 0.30f) {
            error("Temporary fake API failure")
        }
    }
}
