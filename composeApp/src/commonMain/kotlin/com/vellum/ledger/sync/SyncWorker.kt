package com.vellum.ledger.sync

import com.vellum.ledger.database.LedgerDatabase
import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlin.random.Random

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

        // Use a snapshot of transactions to avoid potential race conditions during iteration
        val currentTransactions = database.state.value.transactions

        pendingItems.forEach { item ->
            val transaction = currentTransactions.firstOrNull { it.id == item.entityId }
            if (transaction == null) {
                println("SyncWorker: Transaction ${item.entityId} not found for queue item ${item.id}")
                failed += 1
                return@forEach
            }

            var retryCount = 0
            val maxRetries = 3
            var success = false

            while (retryCount <= maxRetries && !success) {
                try {
                    database.markSyncing(transaction.id)
                    
                    if (retryCount > 0) {
                        // Exponential backoff: 2s, 4s, 8s with jitter
                        val baseDelay = (2.0.pow(retryCount) * 1000L).toLong().coerceAtMost(10000L)
                        val jitter = Random.nextLong(0, 500L)
                        val totalDelay = baseDelay + jitter
                        
                        println("SyncWorker: Retrying ${transaction.id} (Attempt ${retryCount + 1}) after ${totalDelay}ms...")
                        delay(totalDelay)
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
