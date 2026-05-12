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
        if (pendingItems.isEmpty()) return SyncResult(0, 0, 0)

        // Use a snapshot of transactions to avoid potential race conditions during iteration
        val currentTransactions = database.state.value.transactions

        val transactionsToSync = pendingItems.mapNotNull { item ->
            currentTransactions.firstOrNull { it.id == item.entityId }
        }

        if (transactionsToSync.isEmpty()) {
            return SyncResult(pendingItems.size, 0, pendingItems.size)
        }

        var retryCount = 0
        val maxRetries = 3
        var success = false

        while (retryCount <= maxRetries && !success) {
            try {
                transactionsToSync.forEach { database.markSyncing(it.id) }
                
                if (retryCount > 0) {
                    val baseDelay = (2.0.pow(retryCount) * 1000L).toLong().coerceAtMost(10000L)
                    val jitter = Random.nextLong(0, 500L)
                    delay(baseDelay + jitter)
                }

                api.pushBatch(transactionsToSync)
                
                transactionsToSync.forEach { transaction ->
                    val queueItem = pendingItems.first { it.entityId == transaction.id }
                    database.markSynced(transaction.id, queueItem.id)
                }
                success = true
            } catch (e: Throwable) {
                retryCount++
                if (retryCount > maxRetries) {
                    transactionsToSync.forEach { database.markFailed(it.id) }
                }
            }
        }

        return SyncResult(
            attempted = transactionsToSync.size,
            synced = if (success) transactionsToSync.size else 0,
            failed = if (success) 0 else transactionsToSync.size,
        )
    }
}

