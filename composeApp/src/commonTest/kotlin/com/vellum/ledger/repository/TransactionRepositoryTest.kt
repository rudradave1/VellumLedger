package com.vellum.ledger.repository

import com.vellum.ledger.domain.*
import com.vellum.ledger.sync.LedgerApi
import com.vellum.ledger.sync.PushResponse
import com.vellum.ledger.sync.PullResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import com.vellum.ledger.database.LedgerDatabase

class TransactionRepositoryTest {

    private class FakeApi : LedgerApi {
        override suspend fun pushBatch(transactions: List<LedgerTransaction>): PushResponse = PushResponse(success = true)
        override suspend fun pullBackupTransactions(): PullResponse = PullResponse()
        override suspend fun requestMonthlySummary(transactions: List<LedgerTransaction>): String = ""
    }

    private class FakeDatabase : LedgerDatabase {
        private val _state = MutableStateFlow(LedgerSnapshot())
        override val state = _state

        var lastInsertedTransaction: LedgerTransaction? = null
        var lastInsertedQueueItem: SyncQueueItem? = null

        override suspend fun insertTransactionWithQueue(transaction: LedgerTransaction, queueItem: SyncQueueItem) {
            lastInsertedTransaction = transaction
            lastInsertedQueueItem = queueItem
            _state.value = _state.value.copy(
                transactions = _state.value.transactions + transaction,
                queueItems = _state.value.queueItems + queueItem
            )
        }

        override suspend fun restoreTransaction(transaction: LedgerTransaction): Boolean = true
        override suspend fun pendingQueueItems(): List<SyncQueueItem> = emptyList()
        override suspend fun markSynced(transactionId: String, queueItemId: String, serverVersion: Int) {}
        override suspend fun markSyncing(transactionId: String) {}
        override suspend fun markFailed(transactionId: String) {}
        override suspend fun markPending(transactionId: String) {}
        override suspend fun updateSettings(transform: (LedgerSettings) -> LedgerSettings) {
            _state.value = _state.value.copy(settings = transform(_state.value.settings))
        }
        override suspend fun insertCard(card: LedgerCard) {}
        override suspend fun deleteCard(cardId: String) {}
        override suspend fun deleteTransaction(transactionId: String) {
            _state.value = _state.value.copy(
                transactions = _state.value.transactions.filter { it.id != transactionId },
                queueItems = _state.value.queueItems.filter { it.entityId != transactionId }
            )
        }
        override suspend fun clearAll() {
            _state.value = LedgerSnapshot(settings = _state.value.settings)
        }
        override suspend fun saveExchangeRates(rates: Map<String, Double>) {}
        override suspend fun loadExchangeRates(): Map<String, Double> = emptyMap()
    }

    @Test
    fun testAddTransaction() = runTest {
        val db = FakeDatabase()
        val repository = TransactionRepository(db, FakeApi())

        repository.addTransaction(
            amount = 1000L,
            type = TransactionType.Expense,
            category = "Food",
            note = "Coffee"
        )

        val tx = db.lastInsertedTransaction!!
        assertEquals(1000L, tx.amount)
        assertEquals(TransactionType.Expense, tx.type)
        assertEquals("Food", tx.category)
        assertEquals("Coffee", tx.note)
        assertEquals(SyncStatus.Pending, tx.syncStatus)

        val queueItem = db.lastInsertedQueueItem!!
        assertEquals(tx.id, queueItem.entityId)
        assertEquals("PUSH", queueItem.operationType)
        assertEquals(QueueStatus.Pending, queueItem.status)
        
        val transactions = repository.transactions.first()
        assertEquals(1, transactions.size)
        assertEquals(tx.id, transactions[0].id)
    }

    @Test
    fun testDeleteTransaction() = runTest {
        val db = FakeDatabase()
        val repository = TransactionRepository(db, FakeApi())

        repository.addTransaction(1000L, TransactionType.Expense, "Food", "Coffee")
        val txId = db.lastInsertedTransaction!!.id
        
        assertEquals(1, repository.transactions.first().size)
        assertEquals(1, db.state.value.queueItems.size)
        
        repository.deleteTransaction(txId)
        
        assertEquals(0, repository.transactions.first().size)
        assertEquals(0, db.state.value.queueItems.size, "Queue item should be deleted along with transaction")
    }
}
