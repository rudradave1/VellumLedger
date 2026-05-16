package com.vellum.ledger.repository

import com.vellum.ledger.database.LedgerDatabase
import com.vellum.ledger.domain.*
import com.vellum.ledger.data.newLedgerId
import com.vellum.ledger.data.currentTimeMillis
import com.vellum.ledger.sync.LedgerApi
import com.vellum.ledger.sync.toRestoredTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepository(
    private val database: LedgerDatabase,
    private val api: LedgerApi
) {

    val transactions: Flow<List<LedgerTransaction>> = database.state.map { it.transactions }

    suspend fun addTransaction(
        amount: Long,
        type: TransactionType,
        category: String,
        note: String,
        timestamp: Long = currentTimeMillis(),
        currency: String = "USD ($)"
    ) {
        val transactionId = newLedgerId()
        val transaction = LedgerTransaction(
            id = transactionId,
            amount = amount,
            originalAmount = amount,
            originalCurrency = currency,
            type = type,
            category = category,
            note = note,
            createdAt = timestamp,
            syncStatus = SyncStatus.Pending
        )

        val queueItem = SyncQueueItem(
            id = newLedgerId(),
            entityId = transactionId,
            operationType = "PUSH",
            createdAt = currentTimeMillis(),
            status = QueueStatus.Pending
        )

        database.insertTransactionWithQueue(transaction, queueItem)
    }

    suspend fun deleteTransaction(id: String) {
        database.deleteTransaction(id)
    }

    suspend fun retryTransaction(transactionId: String) {
        database.markPending(transactionId)
    }

    suspend fun clearAll() {
        database.clearAll()
    }

    data class BackupRestoreResult(val restored: Int, val skipped: Int)

    suspend fun restoreFromBackup(): BackupRestoreResult {
        val response = api.pullBackupTransactions()
        var restored = 0
        var skipped = 0
        
        val settings = database.state.value.settings
        val currency = settings.currency

        response.transactions.forEach { remoteTransaction ->
            val wasInserted = database.restoreTransaction(remoteTransaction.toRestoredTransaction(currency))
            if (wasInserted) restored++ else skipped++
        }

        if (restored > 0) {
            val now = currentTimeMillis()
            database.updateSettings { it.copy(lastSyncAtMillis = now) }
        }

        return BackupRestoreResult(restored = restored, skipped = skipped)
    }

    suspend fun populateDemoData() {
        clearAll()
        
        database.updateSettings { it.copy(dailyBudget = 7500L) }
        
        database.insertCard(LedgerCard(newLedgerId(), "Chase Sapphire", "4532", CardType.Visa, "12/28", 425075L, "#1565C0"))
        database.insertCard(LedgerCard(newLedgerId(), "Apple Card", "8821", CardType.MasterCard, "05/27", 124020L, "#1A1A1A"))
        database.insertCard(LedgerCard(newLedgerId(), "Amex Platinum", "1004", CardType.Amex, "08/29", 0L, "#C62828"))
        
        val now = currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        
        val items = listOf(
            Triple(1250L, TransactionType.Expense, "Food"),
            Triple(4500L, TransactionType.Expense, "Transport"),
            Triple(320000L, TransactionType.Income, "Salary"),
            Triple(1599L, TransactionType.Expense, "Entertainment"),
        )
        
        items.forEachIndexed { index, (amountCents, type, category) ->
            val timestamp = now - (index % 10) * day - (index * 1000 * 60 * 45L)
            addTransaction(amountCents, type, category, "Demo $category", timestamp)
        }
    }
}
