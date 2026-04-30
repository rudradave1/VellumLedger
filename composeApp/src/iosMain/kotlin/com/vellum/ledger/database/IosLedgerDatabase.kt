package com.vellum.ledger.database

import com.vellum.ledger.domain.LedgerSnapshot
import com.vellum.ledger.domain.LedgerTransaction
import com.vellum.ledger.domain.QueueStatus
import com.vellum.ledger.domain.SyncQueueItem
import com.vellum.ledger.domain.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual fun createLedgerDatabase(): LedgerDatabase = IosLedgerDatabase()

private class IosLedgerDatabase : LedgerDatabase {
    private var transactions = emptyList<LedgerTransaction>()
    private var queueItems = emptyList<SyncQueueItem>()
    private val mutableState = MutableStateFlow(LedgerSnapshot())

    override val state: StateFlow<LedgerSnapshot> = mutableState.asStateFlow()

    override suspend fun insertTransactionWithQueue(
        transaction: LedgerTransaction,
        queueItem: SyncQueueItem,
    ) {
        transactions = (transactions + transaction).sortedByDescending { it.createdAt }
        queueItems = queueItems + queueItem
        refresh()
    }

    override suspend fun pendingQueueItems(): List<SyncQueueItem> =
        queueItems.filter { it.status == QueueStatus.Pending }.sortedBy { it.createdAt }

    override suspend fun markSynced(transactionId: String, queueItemId: String) {
        transactions = transactions.map {
            if (it.id == transactionId) it.copy(syncStatus = SyncStatus.Synced) else it
        }
        queueItems = queueItems.map {
            if (it.id == queueItemId) it.copy(status = QueueStatus.Done) else it
        }
        refresh()
    }

    override suspend fun markSyncing(transactionId: String) {
        transactions = transactions.map {
            if (it.id == transactionId) it.copy(syncStatus = SyncStatus.Syncing) else it
        }
        refresh()
    }

    override suspend fun markFailed(transactionId: String) {
        transactions = transactions.map {
            if (it.id == transactionId) it.copy(syncStatus = SyncStatus.Failed) else it
        }
        refresh()
    }

    private fun refresh() {
        mutableState.value = LedgerSnapshot(
            transactions = transactions,
            queueItems = queueItems,
        )
    }
}
