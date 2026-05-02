package com.vellum.ledger.database

import com.vellum.ledger.domain.LedgerCard
import com.vellum.ledger.domain.LedgerSnapshot
import com.vellum.ledger.domain.LedgerTransaction
import com.vellum.ledger.domain.LedgerSettings
import com.vellum.ledger.domain.SyncQueueItem
import kotlinx.coroutines.flow.StateFlow

interface LedgerDatabase {
    val state: StateFlow<LedgerSnapshot>

    suspend fun insertTransactionWithQueue(transaction: LedgerTransaction, queueItem: SyncQueueItem)

    suspend fun pendingQueueItems(): List<SyncQueueItem>

    suspend fun markSynced(transactionId: String, queueItemId: String)

    suspend fun markSyncing(transactionId: String)

    suspend fun markFailed(transactionId: String)

    suspend fun markPending(transactionId: String)

    suspend fun updateSettings(transform: (LedgerSettings) -> LedgerSettings)

    suspend fun insertCard(card: LedgerCard)

    suspend fun deleteCard(cardId: String)

    suspend fun clearAll()

    suspend fun convertCurrency(from: String, to: String)
}

expect fun createLedgerDatabase(): LedgerDatabase
