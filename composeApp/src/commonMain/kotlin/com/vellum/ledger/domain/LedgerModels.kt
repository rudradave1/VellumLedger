package com.vellum.ledger.domain

import kotlinx.serialization.Serializable

@Serializable
enum class TransactionType {
    Income,
    Expense,
}

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

enum class CardType {
    Visa,
    MasterCard,
    Amex,
}

data class LedgerCard(
    val id: String,
    val cardName: String,
    val cardNumber: String, // Last 4 digits or masked
    val cardType: CardType,
    val expiry: String,
    val balance: Double = 0.0,
    val hexColor: String,
)

data class LedgerSettings(
    val autoSync: Boolean = true,
    val isDarkMode: Boolean = false,
    val lastSyncAtMillis: Long? = null,
    val currency: String = "USD ($)",
)

@Serializable
data class LedgerTransaction(
    val id: String,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val note: String,
    val createdAt: Long,
    val syncStatus: SyncStatus,
)

data class SyncQueueItem(
    val id: String,
    val entityId: String,
    val operationType: String,
    val createdAt: Long,
    val status: QueueStatus,
)

data class LedgerAnalytics(
    val totalIncome: Double,
    val totalExpense: Double,
    val currentBalance: Double,
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
