package com.vellum.ledger.repository

import com.vellum.ledger.data.currentTimeMillis
import com.vellum.ledger.data.newLedgerId
import com.vellum.ledger.database.LedgerDatabase
import com.vellum.ledger.database.createLedgerDatabase
import com.vellum.ledger.domain.*
import com.vellum.ledger.sync.SyncResult
import com.vellum.ledger.sync.SyncWorker
import com.vellum.ledger.sync.toRestoredTransaction
import com.vellum.ledger.ui.util.buildCsvExportRequest
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.*

class LedgerRepository(
    private val database: LedgerDatabase = createLedgerDatabase(),
    private val deviceIdentityManager: com.vellum.ledger.sync.DeviceIdentityManager,
    private val userSession: com.vellum.ledger.sync.UserSession = com.vellum.ledger.sync.SimpleUserSession(deviceIdentityManager),
    private val api: com.vellum.ledger.sync.LedgerApi = com.vellum.ledger.sync.KtorLedgerApi(deviceIdentityManager = deviceIdentityManager, userSession = userSession),
    private val syncWorker: SyncWorker = SyncWorker(database, api),
) {
    init {
        com.vellum.ledger.ui.util.ExchangeRateUtil.initialize(database)
    }
    
    suspend fun initialize() {
        userSession.initialize()
        com.vellum.ledger.sync.scheduleLedgerSync()
    }

    val ledger: StateFlow<LedgerSnapshot> = database.state
    private val summaryMutex = Mutex()

    suspend fun addTransaction(
        amountCents: Long,
        type: TransactionType,
        category: String,
        note: String,
        timestamp: Long = currentTimeMillis(),
    ) {
        require(amountCents > 0) { "Amount must be > 0" }
        val currentCurrency = ledger.value.settings.currency
        val transactionId = newLedgerId()
        val transaction = LedgerTransaction(
            id = transactionId,
            amount = amountCents,
            originalAmount = amountCents,
            originalCurrency = currentCurrency,
            type = type,
            category = category,
            note = note.trim(),
            createdAt = timestamp,
            updatedAt = timestamp,
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
        com.vellum.ledger.sync.scheduleLedgerSync()
    }

    suspend fun addCard(
        name: String,
        number: String,
        type: CardType,
        expiry: String,
        balanceCents: Long,
        hexColor: String,
    ) {
        val card = LedgerCard(
            id = newLedgerId(),
            cardName = name,
            cardNumber = number,
            cardType = type,
            expiry = expiry,
            balance = balanceCents,
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

    data class BackupRestoreResult(
        val restored: Int,
        val skipped: Int,
    )

    suspend fun restoreFromBackup(): BackupRestoreResult {
        val currentCurrency = ledger.value.settings.currency
        val response = api.pullBackupTransactions()
        var restored = 0
        var skipped = 0

        response.transactions.forEach { remoteTransaction ->
            val restoredTransaction = remoteTransaction.toRestoredTransaction(currentCurrency)
            if (database.restoreTransaction(restoredTransaction)) {
                restored += 1
            } else {
                skipped += 1
            }
        }

        if (restored > 0) {
            database.updateSettings { it.copy(lastSyncAtMillis = currentTimeMillis()) }
        }

        return BackupRestoreResult(restored = restored, skipped = skipped)
    }

    suspend fun setAutoSync(enabled: Boolean) {
        database.updateSettings { it.copy(autoSync = enabled) }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        database.updateSettings { it.copy(isDarkMode = enabled) }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        database.updateSettings { it.copy(isBiometricEnabled = enabled) }
    }

    suspend fun setCurrency(currency: String) {
        val oldCurrency = ledger.value.settings.currency
        if (oldCurrency != currency) {
            // Fetch live rates to ensure they are available for UI mapping
            com.vellum.ledger.ui.util.ExchangeRateUtil.refreshRates()
            database.updateSettings { it.copy(currency = currency) }
        }
    }

    suspend fun setDailyBudget(amountCents: Long) {
        database.updateSettings { it.copy(dailyBudget = amountCents) }
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
        val transactions = ledger.value.transactions
        val currentMonthStart = LocalDate(today.year, today.month, 1).atStartOfDayIn(tz).toEpochMilliseconds()
        val currentMonthTransactions = transactions.filter { it.createdAt >= currentMonthStart }
        
        val currentTxCount = currentMonthTransactions.size
        val txCountAtCache = settings.transactionCountAtCacheTime
        val isExistingSummaryError = settings.monthlySummary?.contains("check back later", ignoreCase = true) == true || 
                                     settings.monthlySummary?.startsWith("Error") == true
        
        val isStale = currentTxCount - txCountAtCache >= 5
        
        if (!force && settings.summaryMonth == currentMonthKey && settings.monthlySummary != null && !isExistingSummaryError && !isStale) {
            println("LedgerRepository: Summary is fresh, skipping.")
            return@withLock
        }

        if (currentMonthTransactions.isEmpty()) {
            println("LedgerRepository: No transactions for this month, skipping summary.")
            return@withLock
        }

        println("LedgerRepository: Requesting summary for $currentTxCount transactions. (Stale: $isStale)")

        try {
            val summary = api.requestMonthlySummary(currentMonthTransactions)
            database.updateSettings { 
                it.copy(
                    monthlySummary = summary, 
                    summaryMonth = currentMonthKey,
                    transactionCountAtCacheTime = currentTxCount
                )
            }
        } catch (e: Exception) {
            println("LedgerRepository: Failed to refresh monthly summary: ${e.message}")
        }
    }

    suspend fun populateDemoData() {
        clearAll()
        
        // Set a daily budget for demo
        setDailyBudget(7500L)
        
        // Add some cards with realistic names and balances
        addCard("Chase Sapphire", "4532", CardType.Visa, "12/28", 425075L, "#1565C0")
        addCard("Apple Card", "8821", CardType.MasterCard, "05/27", 124020L, "#1A1A1A")
        addCard("Amex Platinum", "1004", CardType.Amex, "08/29", 0L, "#C62828")
        
        val now = currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        
        // Realistic Transactions
        val items = listOf(
            Triple(1250L, TransactionType.Expense, "Food"),
            Triple(4500L, TransactionType.Expense, "Transport"),
            Triple(320000L, TransactionType.Income, "Salary"),
            Triple(1599L, TransactionType.Expense, "Entertainment"),
            Triple(840L, TransactionType.Expense, "Food"),
            Triple(12000L, TransactionType.Expense, "Shopping"),
            Triple(2500L, TransactionType.Expense, "Health"),
            Triple(450L, TransactionType.Expense, "Transport"),
            Triple(6520L, TransactionType.Expense, "Food"),
            Triple(45000L, TransactionType.Income, "Freelance"),
            Triple(8500L, TransactionType.Expense, "Bills"),
            Triple(1430L, TransactionType.Expense, "Food"),
            Triple(3200L, TransactionType.Expense, "Transport"),
            Triple(120000L, TransactionType.Expense, "Rent"),
            Triple(910L, TransactionType.Expense, "Food"),
            Triple(5500L, TransactionType.Expense, "Shopping"),
            Triple(1299L, TransactionType.Expense, "Entertainment"),
        )
        
        items.forEachIndexed { index, (amountCents, type, category) ->
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
            addTransaction(amountCents, type, category, note, timestamp)
        }

        // Add some older transactions for "last week" comparison
        val olderItems = listOf(
            Triple(4250L, TransactionType.Expense, "Food"),
            Triple(1500L, TransactionType.Expense, "Transport"),
            Triple(8999L, TransactionType.Expense, "Shopping"),
        )
        olderItems.forEachIndexed { index, (amountCents, type, category) ->
            val timestamp = now - (8 + index) * day
            addTransaction(amountCents, type, category, "Weekly $category replenishment", timestamp)
        }

        // Last month transactions
        val lastMonthDay = now - 30 * day
        addTransaction(320000L, TransactionType.Income, "Salary", "Previous month salary", lastMonthDay)
        addTransaction(120000L, TransactionType.Expense, "Rent", "Monthly Apartment Rent", lastMonthDay + 1 * day)
    }

    fun buildCsvExportRequest() = buildCsvExportRequest(ledger.value)
}
