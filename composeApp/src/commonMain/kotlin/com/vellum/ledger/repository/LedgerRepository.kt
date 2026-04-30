package com.vellum.ledger.repository

import com.vellum.ledger.data.currentTimeMillis
import com.vellum.ledger.data.newLedgerId
import com.vellum.ledger.database.LedgerDatabase
import com.vellum.ledger.database.createLedgerDatabase
import com.vellum.ledger.domain.LedgerSnapshot
import com.vellum.ledger.domain.LedgerTransaction
import com.vellum.ledger.domain.QueueStatus
import com.vellum.ledger.domain.SyncQueueItem
import com.vellum.ledger.domain.SyncStatus
import com.vellum.ledger.domain.TransactionType
import com.vellum.ledger.sync.SyncResult
import com.vellum.ledger.sync.SyncWorker
import kotlinx.coroutines.flow.StateFlow

class LedgerRepository(
    private val database: LedgerDatabase = createLedgerDatabase(),
    private val syncWorker: SyncWorker = SyncWorker(database),
) {
    val ledger: StateFlow<LedgerSnapshot> = database.state

    suspend fun addTransaction(
        amount: Double,
        type: TransactionType,
        category: String,
        note: String,
    ) {
        val now = currentTimeMillis()
        val transactionId = newLedgerId()
        val transaction = LedgerTransaction(
            id = transactionId,
            amount = amount,
            type = type,
            category = category,
            note = note.trim(),
            createdAt = now,
            syncStatus = SyncStatus.Pending,
        )
        val queueItem = SyncQueueItem(
            id = newLedgerId(),
            entityId = transactionId,
            operationType = "UPSERT_TRANSACTION",
            createdAt = now,
            status = QueueStatus.Pending,
        )

        database.insertTransactionWithQueue(transaction, queueItem)
    }

    suspend fun syncNow(): SyncResult = syncWorker.processQueue()
}
