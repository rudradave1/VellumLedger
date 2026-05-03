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
    private val api: LedgerApi = KtorLedgerApi(),
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

            var retryCount = 0
            val maxRetries = 2
            var success = false

            while (retryCount <= maxRetries && !success) {
                try {
                    database.markSyncing(transaction.id)
                    
                    if (retryCount > 0) {
                        println("SyncWorker: Retrying ${transaction.id} (Attempt ${retryCount + 1})...")
                        delay(2000L * retryCount) 
                    }

                    api.push(transaction)
                    database.markSynced(transaction.id, item.id)
                    synced += 1
                    success = true
                } catch (e: Throwable) {
                    retryCount++
                    println("SyncWorker: Attempt $retryCount failed for ${transaction.id}: ${e.message}")
                    
                    if (retryCount > maxRetries) {
                        println("SyncWorker: Final failure for ${transaction.id}. Marking as failed.")
                        database.markFailed(transaction.id)
                        failed += 1
                    }
                }
            }
        }

        return SyncResult(
            attempted = pendingItems.size,
            synced = synced,
            failed = failed,
        )
    }
}
