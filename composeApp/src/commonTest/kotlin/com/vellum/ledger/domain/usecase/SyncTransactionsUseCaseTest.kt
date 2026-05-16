package com.vellum.ledger.domain.usecase

import com.vellum.ledger.database.LedgerDatabase
import com.vellum.ledger.domain.*
import com.vellum.ledger.sync.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncTransactionsUseCaseTest {

    private class FakeApi : LedgerApi {
        var pushBatchCalled = false
        var pushedTransactions: List<LedgerTransaction> = emptyList()
        var pushResponse = PushResponse(success = true)

        override suspend fun pushBatch(transactions: List<LedgerTransaction>): PushResponse {
            pushBatchCalled = true
            pushedTransactions = transactions
            return pushResponse
        }

        override suspend fun pullBackupTransactions(): PullResponse = PullResponse()
        override suspend fun requestMonthlySummary(transactions: List<LedgerTransaction>): String = ""
    }

    private class FakeDatabase : LedgerDatabase {
        private val _state = MutableStateFlow(LedgerSnapshot())
        override val state = _state
        
        var pendingQueueItemsList = mutableListOf<SyncQueueItem>()
        var markSyncedCalled = false
        var markSyncingCalled = false

        override suspend fun insertTransactionWithQueue(transaction: LedgerTransaction, queueItem: SyncQueueItem) {
            _state.value = _state.value.copy(transactions = _state.value.transactions + transaction)
        }
        override suspend fun restoreTransaction(transaction: LedgerTransaction): Boolean = true
        override suspend fun pendingQueueItems(): List<SyncQueueItem> = pendingQueueItemsList
        override suspend fun markSynced(transactionId: String, queueItemId: String, serverVersion: Int) {
            markSyncedCalled = true
        }
        override suspend fun markSyncing(transactionId: String) {
            markSyncingCalled = true
        }
        override suspend fun markFailed(transactionId: String) {}
        override suspend fun markPending(transactionId: String) {}
        override suspend fun updateSettings(transform: (LedgerSettings) -> LedgerSettings) {}
        override suspend fun insertCard(card: LedgerCard) {}
        override suspend fun deleteCard(cardId: String) {}
        override suspend fun deleteTransaction(transactionId: String) {}
        override suspend fun clearAll() {}
        override suspend fun saveExchangeRates(rates: Map<String, Double>) {}
        override suspend fun loadExchangeRates(): Map<String, Double> = emptyMap()
    }

    @Test
    fun testSyncSuccess() = runTest {
        val db = FakeDatabase()
        val api = FakeApi()
        val useCase = SyncTransactionsUseCase(db, api)

        val tx = LedgerTransaction(
            id = "tx1", amount = 100, originalAmount = 100, originalCurrency = "USD",
            type = TransactionType.Expense, category = "Food", note = "", createdAt = 123,
            syncStatus = SyncStatus.Pending
        )
        db.insertTransactionWithQueue(tx, SyncQueueItem("q1", "tx1", "PUSH", 123, QueueStatus.Pending))
        db.pendingQueueItemsList.add(SyncQueueItem("q1", "tx1", "PUSH", 123, QueueStatus.Pending))

        val result = useCase()

        assertTrue(api.pushBatchCalled)
        assertEquals(1, result.synced)
        assertTrue(db.markSyncedCalled)
        assertTrue(db.markSyncingCalled)
    }

    @Test
    fun testSyncEmptyQueue() = runTest {
        val db = FakeDatabase()
        val api = FakeApi()
        val useCase = SyncTransactionsUseCase(db, api)

        val result = useCase()

        assertEquals(0, result.attempted)
        assertEquals(0, result.synced)
        assertEquals(0, result.failed)
        assertTrue(!api.pushBatchCalled)
    }
}
