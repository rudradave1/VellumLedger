package com.vellum.ledger.sync

import com.vellum.ledger.database.LedgerDatabase
import kotlinx.coroutines.delay

data class SyncResult(
    val attempted: Int,
    val synced: Int,
    val failed: Int,
)

class SyncWorker(
    private val database: LedgerDatabase,
    private val api: LedgerApi = LedgerApi(),
) {
    suspend fun processQueue(): SyncResult {
        val pendingItems = database.pendingQueueItems()
        var synced = 0
        var failed = 0

        pendingItems.forEach { item ->
            val transaction = database.state.value.transactions.firstOrNull { it.id == item.entityId }
            if (transaction == null) {
                failed += 1
                return@forEach
            }

            try {
                database.markSyncing(transaction.id)
                delay(1000) // Simulate network delay for "Sync in progress" signal
                api.push(transaction)
                database.markSynced(transaction.id, item.id)
                synced += 1
            } catch (_: Throwable) {
                database.markFailed(transaction.id)
                failed += 1
            }
        }

        return SyncResult(
            attempted = pendingItems.size,
            synced = synced,
            failed = failed,
        )
    }
}
