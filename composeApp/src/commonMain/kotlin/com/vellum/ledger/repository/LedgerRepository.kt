package com.vellum.ledger.repository

import com.vellum.ledger.data.currentTimeMillis
import com.vellum.ledger.data.newLedgerId
import com.vellum.ledger.database.LedgerDatabase
import com.vellum.ledger.database.createLedgerDatabase
import com.vellum.ledger.domain.*
import com.vellum.ledger.sync.SyncResult
import com.vellum.ledger.sync.SyncWorker
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.toLocalDateTime

class LedgerRepository(
    private val database: LedgerDatabase = createLedgerDatabase(),
    private val syncWorker: SyncWorker = SyncWorker(database),
) {
    val ledger: StateFlow<LedgerSnapshot> = database.state

    suspend fun addTransaction(
        amount: Double,
        type: TransactionType,
        category: String,
        note: String,
        timestamp: Long = currentTimeMillis(),
    ) {
        require(amount > 0.0) { "Amount must be > 0" }
        val transactionId = newLedgerId()
        val transaction = LedgerTransaction(
            id = transactionId,
            amount = amount,
            type = type,
            category = category,
            note = note.trim(),
            createdAt = timestamp,
            syncStatus = SyncStatus.Pending,
        )
        val queueItem = SyncQueueItem(
            id = newLedgerId(),
            entityId = transactionId,
            operationType = "UPSERT_TRANSACTION",
            createdAt = currentTimeMillis(),
            status = QueueStatus.Pending,
        )

        database.insertTransactionWithQueue(transaction, queueItem)
    }

    suspend fun addCard(
        name: String,
        number: String,
        type: CardType,
        expiry: String,
        balance: Double,
        hexColor: String,
    ) {
        val card = LedgerCard(
            id = newLedgerId(),
            cardName = name,
            cardNumber = number,
            cardType = type,
            expiry = expiry,
            balance = balance,
            hexColor = hexColor,
        )
        database.insertCard(card)
    }

    suspend fun deleteCard(cardId: String) {
        database.deleteCard(cardId)
    }

    suspend fun syncNow(): SyncResult {
        val result = syncWorker.processQueue()
        if (result.synced > 0) {
            val now = currentTimeMillis()
            database.updateSettings { it.copy(lastSyncAtMillis = now) }
        }
        return result
    }

    suspend fun setAutoSync(enabled: Boolean) {
        database.updateSettings { it.copy(autoSync = enabled) }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        database.updateSettings { it.copy(isDarkMode = enabled) }
    }

    suspend fun setCurrency(currency: String) {
        database.updateSettings { it.copy(currency = currency) }
    }

    suspend fun retryTransaction(transactionId: String) {
        database.markPending(transactionId)
    }

    suspend fun clearAll() {
        database.clearAll()
    }

    fun getCsvData(): String {
        val snapshot = ledger.value
        val sb = StringBuilder()
        sb.append("Date,Type,Category,Amount,Note,Status\n")
        snapshot.transactions.forEach { t ->
            val date = formatDateTime(t.createdAt)
            sb.append("${date},${t.type},${t.category},${t.amount},\"${t.note}\",${t.syncStatus}\n")
        }
        return sb.toString()
    }
}

private fun formatDateTime(millis: Long): String {
    val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(millis)
    val dt = instant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
    return "${dt.year}-${dt.monthNumber.toString().padStart(2, '0')}-${dt.dayOfMonth.toString().padStart(2, '0')} " +
            "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
}
