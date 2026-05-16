package com.vellum.ledger.domain

import kotlinx.serialization.Serializable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import com.vellum.ledger.data.currentTimeMillis

@Serializable
enum class SyncStatus {
    Pending,
    Syncing,
    Synced,
    Failed,
}

enum class QueueStatus {
    Pending,
    Done,
}

enum class TransactionType {
    Income,
    Expense,
}

enum class CardType {
    Visa,
    MasterCard,
    Amex,
}

@Serializable
data class LedgerCard(
    val id: String,
    val cardName: String,
    val cardNumber: String, // Last 4 digits or masked
    val cardType: CardType,
    val expiry: String,
    val balance: Long = 0,
    val hexColor: String,
)

@Serializable
data class LedgerSettings(
    val autoSync: Boolean = true,
    val isDarkMode: Boolean? = null,
    val isBiometricEnabled: Boolean = false,
    val lastSyncAtMillis: Long? = null,
    val currency: String = "USD ($)",
    val dailyBudget: Long = 0,
    val monthlySummary: String? = null,
    val summaryMonth: String? = null, // Format: YYYY-MM
    val transactionCountAtCacheTime: Int = 0,
) {
    companion object {
        const val KEY_AUTO_SYNC = "auto_sync"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        const val KEY_LAST_SYNC_AT = "last_sync_at_millis"
        const val KEY_CURRENCY = "currency"
        const val KEY_DAILY_BUDGET = "daily_budget"
        const val KEY_MONTHLY_SUMMARY = "monthly_summary"
        const val KEY_SUMMARY_MONTH = "summary_month"
        const val KEY_TX_COUNT_AT_CACHE = "tx_count_at_cache_time"
    }
}

@Serializable
data class LedgerTransaction(
    val id: String,
    val amount: Long,
    val originalAmount: Long,
    val originalCurrency: String,
    val type: TransactionType,
    val category: String,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    val syncStatus: SyncStatus,
    val localVersion: Int = 1,
    val serverVersion: Int = 0,
)

data class SyncQueueItem(
    val id: String,
    val entityId: String,
    val operationType: String,
    val createdAt: Long,
    val status: QueueStatus,
)

data class LedgerAnalytics(
    val totalIncome: Long,
    val totalExpense: Long,
    val currentBalance: Long,
)

data class LedgerSnapshot(
    val transactions: List<LedgerTransaction> = emptyList(),
    val cards: List<LedgerCard> = emptyList(),
    val queueItems: List<SyncQueueItem> = emptyList(),
    val settings: LedgerSettings = LedgerSettings(),
) {
    val analytics: LedgerAnalytics
        get() {
            val income = transactions
                .filter { it.type == TransactionType.Income }
                .sumOf { it.amount }
            val expense = transactions
                .filter { it.type == TransactionType.Expense }
                .sumOf { it.amount }
            return LedgerAnalytics(
                totalIncome = income,
                totalExpense = expense,
                currentBalance = income - expense,
            )
        }

    val pendingCount: Int
        get() = transactions.count { it.syncStatus == SyncStatus.Pending }
}
