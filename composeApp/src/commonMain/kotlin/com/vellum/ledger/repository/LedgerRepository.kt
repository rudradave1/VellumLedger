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

    suspend fun deleteTransaction(transactionId: String) {
        database.deleteTransaction(transactionId)
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
        println("LedgerRepository: refreshMonthlySummary: currentMonthKey=$currentMonthKey, settings.summaryMonth=${settings.summaryMonth}, hasSummary=${settings.monthlySummary != null}")
        
        val isExistingSummaryError = settings.monthlySummary?.startsWith("Error") == true
        
        if (!force && settings.summaryMonth == currentMonthKey && settings.monthlySummary != null && !isExistingSummaryError) {
            println("LedgerRepository: Summary already exists for this month, skipping.")
            return@withLock
        }

        val transactions = ledger.value.transactions
        val currentMonthStart = LocalDate(today.year, today.month, 1).atStartOfDayIn(tz).toEpochMilliseconds()
        val currentMonthTransactions = transactions.filter { it.createdAt >= currentMonthStart }
        
        println("LedgerRepository: Requesting summary for ${currentMonthTransactions.size} transactions.")
        currentMonthTransactions.forEach { 
            println("LedgerRepository: Transaction: ${it.category}, ${it.amount}, ${it.type}")
        }

        try {
            val summary = api.requestMonthlySummary(currentMonthTransactions)
            println("LedgerRepository: Received summary: ${summary.take(50)}...")
            database.updateSettings { 
                it.copy(monthlySummary = summary, summaryMonth = currentMonthKey) 
            }
        } catch (e: Exception) {
            println("LedgerRepository: Failed to refresh monthly summary: ${e.message}")
        }
    }

    suspend fun populateDemoData() {
        clearAll()
        
        // Set a daily budget for demo
        setDailyBudget(75.0)
        
        // Add some cards with realistic names and balances
        addCard("Chase Sapphire", "4532", CardType.Visa, "12/28", 4250.75, "#1565C0")
        addCard("Apple Card", "8821", CardType.MasterCard, "05/27", 1240.20, "#1A1A1A")
        addCard("Amex Platinum", "1004", CardType.Amex, "08/29", 0.0, "#C62828")
        
        val now = currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        
        // Realistic Transactions
        val items = listOf(
            Triple(12.50, TransactionType.Expense, "Food"),
            Triple(45.00, TransactionType.Expense, "Transport"),
            Triple(3200.00, TransactionType.Income, "Salary"),
            Triple(15.99, TransactionType.Expense, "Entertainment"),
            Triple(8.40, TransactionType.Expense, "Food"),
            Triple(120.00, TransactionType.Expense, "Shopping"),
            Triple(25.00, TransactionType.Expense, "Health"),
            Triple(4.50, TransactionType.Expense, "Transport"),
            Triple(65.20, TransactionType.Expense, "Food"),
            Triple(450.00, TransactionType.Income, "Freelance"),
            Triple(85.00, TransactionType.Expense, "Bills"),
            Triple(14.30, TransactionType.Expense, "Food"),
            Triple(32.00, TransactionType.Expense, "Transport"),
            Triple(1200.00, TransactionType.Expense, "Rent"),
            Triple(9.10, TransactionType.Expense, "Food"),
            Triple(55.00, TransactionType.Expense, "Shopping"),
            Triple(12.99, TransactionType.Expense, "Entertainment"),
        )
        
        items.forEachIndexed { index, (amount, type, category) ->
            val timestamp = now - (index % 10) * day - (index * 1000 * 60 * 45L)
            val note = when(category) {
                "Food" -> listOf("Lunch at Panera", "Whole Foods Market", "Coffee with client", "Dinner takeout").random()
                "Transport" -> listOf("Uber to office", "Shell Gas Station", "Public Transit Pass").random()
                "Salary" -> "Vellum Tech Monthly Salary"
                "Freelance" -> "Mobile App UI Design Kit"
                "Shopping" -> listOf("Amazon Prime - Home Decor", "Nike Store", "Best Buy - Cables").random()
                "Entertainment" -> listOf("Netflix Subscription", "Movie Night", "Spotify Family").random()
                "Bills" -> listOf("Verizon Wireless", "Electric Bill", "Cloud Storage Subscription").random()
                else -> ""
            }
            addTransaction(amount, type, category, note, timestamp)
        }

        // Add some older transactions for "last week" comparison
        val olderItems = listOf(
            Triple(42.50, TransactionType.Expense, "Food"),
            Triple(15.00, TransactionType.Expense, "Transport"),
            Triple(89.99, TransactionType.Expense, "Shopping"),
        )
        olderItems.forEachIndexed { index, (amount, type, category) ->
            val timestamp = now - (8 + index) * day
            addTransaction(amount, type, category, "Weekly $category replenishment", timestamp)
        }

        // Last month transactions
        val lastMonthDay = now - 30 * day
        addTransaction(3200.0, TransactionType.Income, "Salary", "Previous month salary", lastMonthDay)
        addTransaction(1200.0, TransactionType.Expense, "Rent", "Monthly Apartment Rent", lastMonthDay + 1 * day)
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
