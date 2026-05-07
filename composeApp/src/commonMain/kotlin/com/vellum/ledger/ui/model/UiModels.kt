package com.vellum.ledger.ui.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.vellum.ledger.domain.CardType
import com.vellum.ledger.domain.SyncStatus
import com.vellum.ledger.domain.TransactionType

data class TransactionUiModel(
    val id: String,
    val amount: Double,
    val amountFormatted: String,
    val type: TransactionType,
    val category: String,
    val categoryIcon: ImageVector,
    val note: String,
    val createdAt: Long,
    val dateFormatted: String,
    val syncStatus: SyncStatus,
    val color: Color
)

data class CardUiModel(
    val id: String,
    val cardName: String,
    val cardNumberMasked: String,
    val cardType: CardType,
    val expiry: String,
    val balance: Double,
    val balanceFormatted: String,
    val color: Color
)

data class SettingsUiModel(
    val autoSync: Boolean,
    val isDarkMode: Boolean?,
    val isBiometricEnabled: Boolean,
    val lastSyncMessage: String,
    val currency: String,
    val dailyBudget: Double,
    val dailyBudgetFormatted: String,
    val monthlySummary: String?,
    val isSummaryLoading: Boolean
)

data class AnalyticsUiModel(
    val totalIncome: Double,
    val totalIncomeFormatted: String,
    val totalExpense: Double,
    val totalExpenseFormatted: String,
    val currentBalance: Double,
    val currentBalanceFormatted: String,
    val transactions: List<TransactionUiModel>
)

