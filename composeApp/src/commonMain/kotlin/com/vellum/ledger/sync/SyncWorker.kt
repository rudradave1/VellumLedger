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
    private val api: LedgerApi,
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

                val response = api.pushBatch(transactionsToSync)

                if (!response.success) {
                    throw SyncException(response.message ?: "Backend reported sync failure")
                }

                val acknowledgements = if (response.acknowledgements.isEmpty()) {
                    println(
                        "SyncWorker: Backend returned success without acknowledgements; " +
                            "treating all ${transactionsToSync.size} items as synced."
                    )
                    transactionsToSync.map { tx ->
                        SyncAcknowledgement(
                            id = tx.id,
                            serverVersion = maxOf(1, tx.serverVersion + 1),
                        )
                    }
                } else {
                    response.acknowledgements
                }

                val acknowledgedIds = acknowledgements.map { it.id }.toSet()

                acknowledgements.forEach { ack ->
                    val localTx = database.state.value.transactions.firstOrNull { it.id == ack.id }
                    val txAtStart = transactionsToSync.firstOrNull { it.id == ack.id }
                    
                    if (localTx != null && txAtStart != null) {
                        if (localTx.localVersion > txAtStart.localVersion) {
                            println("SyncWorker: Conflict for ${ack.id}. Local v${localTx.localVersion} > Sync v${txAtStart.localVersion}. Re-sync required.")
                            database.markPending(ack.id)
                        } else {
                            val queueItem = pendingItems.first { it.entityId == ack.id }
                            database.markSynced(ack.id, queueItem.id, ack.serverVersion)
                        }
                    }
                }
                
                // If any transactions were NOT acknowledged, move them back to Pending so they aren't stuck in Syncing
                transactionsToSync.filter { it.id !in acknowledgedIds }.forEach {
                    database.markPending(it.id)
                }

                success = true
                return SyncResult(
                    attempted = transactionsToSync.size,
                    synced = acknowledgedIds.size,
                    failed = transactionsToSync.size - acknowledgedIds.size,
                )
            } catch (e: Throwable) {
                retryCount++
                println("SyncWorker: Attempt $retryCount failed: ${e.message}")
                
                // Show a non-blocking error so the user knows why it's still "Syncing"
                com.vellum.ledger.ui.util.GlobalErrorHandler.handleError(
                    Exception("Sync Attempt $retryCount failed. Retrying... (${e.message})")
                )

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
