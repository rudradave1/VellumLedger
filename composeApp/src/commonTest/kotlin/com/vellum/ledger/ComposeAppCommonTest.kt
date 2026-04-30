package com.vellum.ledger

import com.vellum.ledger.database.LedgerDatabase
import com.vellum.ledger.domain.LedgerSnapshot
import com.vellum.ledger.domain.LedgerSettings
import com.vellum.ledger.domain.LedgerTransaction
import com.vellum.ledger.domain.QueueStatus
import com.vellum.ledger.domain.SyncQueueItem
import com.vellum.ledger.domain.SyncStatus
import com.vellum.ledger.domain.TransactionType
import com.vellum.ledger.sync.FakeLedgerApi
import com.vellum.ledger.sync.SyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class ComposeAppCommonTest {

    @Test
    fun snapshotCalculatesAnalyticsFromLocalTransactions() {
        val snapshot = LedgerSnapshot(
            transactions = listOf(
                transaction(amount = 200.0, type = TransactionType.Income),
                transaction(amount = 35.5, type = TransactionType.Expense),
            ),
        )

        assertEquals(200.0, snapshot.analytics.totalIncome)
        assertEquals(35.5, snapshot.analytics.totalExpense)
        assertEquals(164.5, snapshot.analytics.currentBalance)
    }

    @Test
    fun syncWorkerMarksPendingTransactionSyncedAndQueueDone() = runBlocking {
        val database = FakeLedgerDatabase()
        val transaction = transaction(syncStatus = SyncStatus.Pending)
        val queueItem = SyncQueueItem(
            id = "queue-1",
            entityId = transaction.id,
            operationType = "UPSERT_TRANSACTION",
            createdAt = 2L,
            status = QueueStatus.Pending,
        )
        database.insertTransactionWithQueue(transaction, queueItem)

        val result = SyncWorker(
            database = database,
            api = FakeLedgerApi(randomFail = false),
        ).processQueue()

        assertEquals(1, result.attempted)
        assertEquals(1, result.synced)
        assertEquals(0, result.failed)
        assertEquals(SyncStatus.Synced, database.state.value.transactions.single().syncStatus)
        assertEquals(QueueStatus.Done, database.state.value.queueItems.single().status)
    }
}

private fun transaction(
    amount: Double = 25.0,
    type: TransactionType = TransactionType.Expense,
    syncStatus: SyncStatus = SyncStatus.Pending,
) = LedgerTransaction(
    id = "transaction-$amount-$type",
    amount = amount,
    type = type,
    category = "Food",
    note = "",
    createdAt = 1L,
    syncStatus = syncStatus,
)

private class FakeLedgerDatabase : LedgerDatabase {
    private val mutableState = MutableStateFlow(LedgerSnapshot())

    override val state: StateFlow<LedgerSnapshot> = mutableState.asStateFlow()

    override suspend fun insertTransactionWithQueue(
        transaction: LedgerTransaction,
        queueItem: SyncQueueItem,
    ) {
        mutableState.value = LedgerSnapshot(
            transactions = mutableState.value.transactions + transaction,
            queueItems = mutableState.value.queueItems + queueItem,
            settings = mutableState.value.settings,
        )
    }

    override suspend fun pendingQueueItems(): List<SyncQueueItem> =
        mutableState.value.queueItems.filter { it.status == QueueStatus.Pending }

    override suspend fun markSynced(transactionId: String, queueItemId: String) {
        mutableState.value = LedgerSnapshot(
            transactions = mutableState.value.transactions.map {
                if (it.id == transactionId) it.copy(syncStatus = SyncStatus.Synced) else it
            },
            queueItems = mutableState.value.queueItems.map {
                if (it.id == queueItemId) it.copy(status = QueueStatus.Done) else it
            },
            settings = mutableState.value.settings,
        )
    }

    override suspend fun markSyncing(transactionId: String) {
        mutableState.value = mutableState.value.copy(
            transactions = mutableState.value.transactions.map {
                if (it.id == transactionId) it.copy(syncStatus = SyncStatus.Syncing) else it
            },
        )
    }

    override suspend fun markFailed(transactionId: String) {
        mutableState.value = mutableState.value.copy(
            transactions = mutableState.value.transactions.map {
                if (it.id == transactionId) it.copy(syncStatus = SyncStatus.Failed) else it
            },
        )
    }

    override suspend fun markPending(transactionId: String) {
        mutableState.value = mutableState.value.copy(
            transactions = mutableState.value.transactions.map {
                if (it.id == transactionId) it.copy(syncStatus = SyncStatus.Pending) else it
            },
        )
    }

    override suspend fun updateSettings(transform: (LedgerSettings) -> LedgerSettings) {
        mutableState.value = mutableState.value.copy(settings = transform(mutableState.value.settings))
    }

    override suspend fun clearAll() {
        mutableState.value = LedgerSnapshot(settings = mutableState.value.settings)
    }
}
