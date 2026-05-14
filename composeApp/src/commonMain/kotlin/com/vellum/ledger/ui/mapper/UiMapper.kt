package com.vellum.ledger.ui.mapper

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.vellum.ledger.domain.*
import com.vellum.ledger.ui.model.*
import com.vellum.ledger.ui.provider.StringProvider
import com.vellum.ledger.ui.theme.*
import com.vellum.ledger.ui.util.parseHexColor
import kotlinx.datetime.*

class UiMapper(private val stringProvider: StringProvider) {

    fun mapToTransactionUi(transaction: LedgerTransaction, currency: String): TransactionUiModel {
        val (icon, _) = categoryIconAndTint(transaction.category)
        val color = if (transaction.type == TransactionType.Income) IncomeColor else ExpenseColor
        
        val convertedAmount = com.vellum.ledger.ui.util.ExchangeRateUtil.convert(
            transaction.originalAmount,
            transaction.originalCurrency,
            currency
        )
        
        val dateText = Instant.fromEpochMilliseconds(transaction.createdAt)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .let { "${it.dayOfMonth} ${it.month.name.take(3)}, ${it.year}" }

        return TransactionUiModel(
            id = transaction.id,
            amount = convertedAmount,
            amountFormatted = (if (transaction.type == TransactionType.Income) "+" else "-") + stringProvider.formatMoney(convertedAmount, currency),
            type = transaction.type,
            category = transaction.category,
            categoryIcon = icon,
            note = transaction.note,
            createdAt = transaction.createdAt,
            dateFormatted = dateText,
            syncStatus = transaction.syncStatus,
            color = color
        )
    }

    fun mapToCardUi(card: LedgerCard, currency: String): CardUiModel {
        return CardUiModel(
            id = card.id,
            cardName = card.cardName,
            cardNumberMasked = "**** **** **** ${card.cardNumber}",
            cardType = card.cardType,
            expiry = card.expiry,
            balance = card.balance,
            balanceFormatted = stringProvider.formatMoney(card.balance, currency),
            color = parseHexColor(card.hexColor)
        )
    }

    fun mapToSettingsUi(settings: LedgerSettings, isSummaryLoading: Boolean): SettingsUiModel {
        return SettingsUiModel(
            autoSync = settings.autoSync,
            isDarkMode = settings.isDarkMode,
            isBiometricEnabled = settings.isBiometricEnabled,
            lastSyncMessage = stringProvider.formatLastSync(settings.lastSyncAtMillis),
            currency = settings.currency,
            dailyBudget = settings.dailyBudget,
            dailyBudgetFormatted = stringProvider.formatMoney(settings.dailyBudget, settings.currency),
            monthlySummary = settings.monthlySummary,
            isSummaryLoading = isSummaryLoading,
            areRatesAvailable = com.vellum.ledger.ui.util.ExchangeRateUtil.isAvailable()
        )
    }

    fun mapToAnalyticsUi(ledger: LedgerSnapshot): AnalyticsUiModel {
        val currency = ledger.settings.currency
        
        val convertedTransactions = ledger.transactions.map { mapToTransactionUi(it, currency) }
        
        val totalIncome = convertedTransactions
            .filter { it.type == TransactionType.Income }
            .sumOf { it.amount }
            
        val totalExpense = convertedTransactions
            .filter { it.type == TransactionType.Expense }
            .sumOf { it.amount }

        return AnalyticsUiModel(
            totalIncome = totalIncome,
            totalIncomeFormatted = stringProvider.formatMoney(totalIncome, currency),
            totalExpense = totalExpense,
            totalExpenseFormatted = stringProvider.formatMoney(totalExpense, currency),
            currentBalance = totalIncome - totalExpense,
            currentBalanceFormatted = stringProvider.formatMoney(totalIncome - totalExpense, currency),
            transactions = convertedTransactions
        )
    }

    private fun categoryIconAndTint(category: String): Pair<ImageVector, Color> = when (category.lowercase()) {
        "food" -> Icons.Outlined.Restaurant to Color(0xFF3525CD)
        "transport" -> Icons.Outlined.DirectionsCar to Color(0xFF3525CD)
        "shopping" -> Icons.Outlined.ShoppingCart to Color(0xFF3525CD)
        "bills" -> Icons.Outlined.ElectricBolt to Color(0xFF3525CD)
        "salary" -> Icons.Outlined.Payments to Color(0xFF3525CD)
        else -> Icons.Outlined.ShoppingCart to Color(0xFF3525CD)
    }
}
