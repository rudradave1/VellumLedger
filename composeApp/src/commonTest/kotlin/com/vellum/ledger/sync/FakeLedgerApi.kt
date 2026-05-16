package com.vellum.ledger.sync

import com.vellum.ledger.domain.LedgerTransaction
import com.vellum.ledger.sync.LedgerApi
import com.vellum.ledger.sync.PushResponse
import com.vellum.ledger.sync.PullResponse
import com.vellum.ledger.sync.SyncAcknowledgement
import com.vellum.ledger.sync.SyncException

class FakeLedgerApi(private val randomFail: Boolean = false) : LedgerApi {
    override suspend fun pushBatch(transactions: List<LedgerTransaction>): PushResponse {
        if (randomFail && (0..10).random() > 7) throw SyncException("Random failure")
        return PushResponse(acknowledgements = transactions.map { SyncAcknowledgement(it.id, 1) })
    }

    override suspend fun pullBackupTransactions(): PullResponse {
        return PullResponse()
    }

    override suspend fun requestMonthlySummary(transactions: List<LedgerTransaction>): String {
        return "This is a fake AI insight summary for your transactions."
    }
}
