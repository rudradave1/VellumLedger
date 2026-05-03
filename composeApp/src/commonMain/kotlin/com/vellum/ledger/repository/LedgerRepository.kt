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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.*

class LedgerRepository(
    private val database: LedgerDatabase = createLedgerDatabase(),
    private val syncWorker: SyncWorker = SyncWorker(database, com.vellum.ledger.sync.KtorLedgerApi()),
    private val api: com.vellum.ledger.sync.LedgerApi = com.vellum.ledger.sync.KtorLedgerApi(),
) {
    val ledger: StateFlow<LedgerSnapshot> = database.state
    private val summaryMutex = Mutex()

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
        val oldCurrency = ledger.value.settings.currency
        if (oldCurrency != currency) {
            database.convertCurrency(oldCurrency, currency)
            database.updateSettings { it.copy(currency = currency) }
        }
    }

    suspend fun setDailyBudget(amount: Double) {
        database.updateSettings { it.copy(dailyBudget = amount) }
    }

    suspend fun retryTransaction(transactionId: String) {
        database.markPending(transactionId)
    }

    suspend fun clearAll() {
        database.clearAll()
    }

    suspend fun refreshMonthlySummary(force: Boolean = false) = summaryMutex.withLock {
        val now = currentTimeMillis()
        val tz = TimeZone.currentSystemDefault()
        val today = Instant.fromEpochMilliseconds(now).toLocalDateTime(tz).date
        val currentMonthKey = "${today.year}-${today.monthNumber.toString().padStart(2, '0')}"
        
        val settings = ledger.value.settings
        if (!force && settings.summaryMonth == currentMonthKey && settings.monthlySummary != null) {
            return@withLock
        }

        val transactions = ledger.value.transactions
        
        // Current Month Data
        val currentMonthStart = LocalDate(today.year, today.month, 1).atStartOfDayIn(tz).toEpochMilliseconds()
        val currentMonthTransactions = transactions.filter { it.createdAt >= currentMonthStart }

        try {
            val summary = api.requestMonthlySummary(currentMonthTransactions)
            database.updateSettings { 
                it.copy(monthlySummary = summary, summaryMonth = currentMonthKey) 
            }
        } catch (e: Exception) {
            println("LedgerRepository: Failed to refresh monthly summary: ${e.message}")
            // Don't throw, just log. We don't want to crash the UI for an insight failure.
        }
    }

    suspend fun populateDemoData() {
        clearAll()
        
        // Set a daily budget for demo
        setDailyBudget(500.0)
        
        // Add some cards
        addCard("Premium Gold", "4532", CardType.Visa, "12/28", 12500.0, "#FFD700")
        addCard("Daily Saver", "8821", CardType.MasterCard, "05/27", 3400.0, "#4CAF50")
        
        val now = currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        
        // Realistic Transactions
        // Last 7 days
        val items = listOf(
            Triple(120.50, TransactionType.Expense, "Food"),
            Triple(45.00, TransactionType.Expense, "Transport"),
            Triple(2500.00, TransactionType.Income, "Salary"),
            Triple(60.00, TransactionType.Expense, "Entertainment"),
            Triple(15.00, TransactionType.Expense, "Food"),
            Triple(200.00, TransactionType.Expense, "Shopping"),
            Triple(80.00, TransactionType.Expense, "Health"),
            Triple(45.00, TransactionType.Expense, "Transport"),
            Triple(110.00, TransactionType.Expense, "Food"),
            Triple(300.00, TransactionType.Income, "Freelance"),
            Triple(50.00, TransactionType.Expense, "Bills"),
            Triple(25.00, TransactionType.Expense, "Food"),
            Triple(75.00, TransactionType.Expense, "Transport"),
            Triple(500.00, TransactionType.Expense, "Rent"),
            Triple(40.00, TransactionType.Expense, "Food"),
            Triple(120.00, TransactionType.Expense, "Shopping"),
            Triple(30.00, TransactionType.Expense, "Entertainment"),
        )
        
        items.forEachIndexed { index, (amount, type, category) ->
            val timestamp = now - (index % 10) * day - (index * 1000 * 60 * 30L)
            val note = when(category) {
                "Food" -> listOf("Lunch at Starbucks", "Grocery shopping", "Dinner with friends").random()
                "Transport" -> listOf("Uber to office", "Gas station", "Bus fare").random()
                "Salary" -> "Monthly paycheck"
                "Freelance" -> "Logo design project"
                "Shopping" -> listOf("New sneakers", "Amazon order", "Gift for Mom").random()
                else -> "Regular $category"
            }
            addTransaction(amount, type, category, note, timestamp)
        }

        // Add some older transactions for "last week" comparison
        val olderItems = listOf(
            Triple(80.00, TransactionType.Expense, "Food"),
            Triple(40.00, TransactionType.Expense, "Transport"),
            Triple(150.00, TransactionType.Expense, "Shopping"),
        )
        olderItems.forEachIndexed { index, (amount, type, category) ->
            val timestamp = now - (8 + index) * day
            addTransaction(amount, type, category, "Last week $category", timestamp)
        }

        // Last month transactions
        val lastMonthDay = now - 30 * day
        addTransaction(3000.0, TransactionType.Income, "Salary", "Last month salary", lastMonthDay)
        addTransaction(1500.0, TransactionType.Expense, "Rent", "Last month rent", lastMonthDay + 1 * day)
        addTransaction(200.0, TransactionType.Expense, "Food", "Last month food", lastMonthDay + 2 * day)
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
