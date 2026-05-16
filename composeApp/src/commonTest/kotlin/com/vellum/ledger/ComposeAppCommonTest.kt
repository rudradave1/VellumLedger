package com.vellum.ledger

import com.vellum.ledger.database.LedgerDatabase
import com.vellum.ledger.domain.*
import com.vellum.ledger.sync.*
import com.vellum.ledger.domain.usecase.SyncTransactionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComposeAppCommonTest {

    @Test
    fun snapshotCalculatesAnalyticsFromLocalTransactions() {
        val snapshot = LedgerSnapshot(
            transactions = listOf(
                transaction(amount = 200L, type = TransactionType.Income),
                transaction(amount = 35L, type = TransactionType.Expense),
            ),
        )

        assertEquals(200L, snapshot.analytics.totalIncome)
        assertEquals(35L, snapshot.analytics.totalExpense)
        assertEquals(165L, snapshot.analytics.currentBalance)
    }

    @Test
    fun syncUseCaseMarksPendingTransactionSyncedAndQueueDone() = runBlocking {
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

        val result = SyncTransactionsUseCase(
            database = database,
            api = FakeLedgerApi(randomFail = false),
        )()

        assertEquals(1, result.attempted)
        assertEquals(1, result.synced)
        assertEquals(0, result.failed)
        assertEquals(SyncStatus.Synced, database.state.value.transactions.single().syncStatus)
        assertEquals(QueueStatus.Done, database.state.value.queueItems.single().status)
    }

    @Test
    fun deleteTransactionRemovesOrphanedQueueItems() = runBlocking {
        val database = FakeLedgerDatabase()
        val txId = "tx-123"
        val transaction = transaction(syncStatus = SyncStatus.Pending).copy(id = txId)
        val queueItem = SyncQueueItem(
            id = "q-1",
            entityId = txId,
            operationType = "PUSH",
            createdAt = 1L,
            status = QueueStatus.Pending
        )
        
        database.insertTransactionWithQueue(transaction, queueItem)
        assertEquals(1, database.state.value.transactions.size)
        assertEquals(1, database.state.value.queueItems.size)
        
        database.deleteTransaction(txId)
        
        assertTrue(database.state.value.transactions.isEmpty())
        assertTrue(database.state.value.queueItems.isEmpty(), "Queue item should be deleted along with transaction")
    }
}

private fun transaction(
    amount: Long = 25L,
    type: TransactionType = TransactionType.Expense,
    syncStatus: SyncStatus = SyncStatus.Pending,
) = LedgerTransaction(
    id = "transaction-$amount-$type",
    amount = amount,
    originalAmount = amount,
    originalCurrency = "USD ($)",
    type = type,
    category = "Food",
    note = "",
    createdAt = 1L,
    syncStatus = syncStatus,
    localVersion = 1,
    serverVersion = 0,
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

    override suspend fun restoreTransaction(transaction: LedgerTransaction): Boolean {
        val exists = mutableState.value.transactions.any { it.id == transaction.id }
        if (exists) return false
        mutableState.value = mutableState.value.copy(
            transactions = mutableState.value.transactions + transaction,
        )
        return true
    }

    override suspend fun pendingQueueItems(): List<SyncQueueItem> =
        mutableState.value.queueItems.filter { it.status == QueueStatus.Pending }

    override suspend fun markSynced(transactionId: String, queueItemId: String, serverVersion: Int) {
        mutableState.value = LedgerSnapshot(
            transactions = mutableState.value.transactions.map {
                if (it.id == transactionId) it.copy(syncStatus = SyncStatus.Synced, serverVersion = serverVersion) else it
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

    override suspend fun insertCard(card: LedgerCard) {
        mutableState.value = mutableState.value.copy(
            cards = mutableState.value.cards + card
        )
    }

    override suspend fun deleteCard(cardId: String) {
        mutableState.value = mutableState.value.copy(
            cards = mutableState.value.cards.filter { it.id != cardId }
        )
    }

    override suspend fun clearAll() {
        mutableState.value = LedgerSnapshot(settings = mutableState.value.settings)
    }

    override suspend fun deleteTransaction(transactionId: String) {
        mutableState.value = mutableState.value.copy(
            transactions = mutableState.value.transactions.filter { it.id != transactionId },
            queueItems = mutableState.value.queueItems.filter { it.entityId != transactionId }
        )
    }

    override suspend fun saveExchangeRates(rates: Map<String, Double>) = Unit

    override suspend fun loadExchangeRates(): Map<String, Double> = emptyMap()
}
