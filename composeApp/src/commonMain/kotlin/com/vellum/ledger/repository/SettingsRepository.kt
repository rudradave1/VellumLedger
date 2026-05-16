package com.vellum.ledger.repository

import com.vellum.ledger.database.LedgerDatabase
import com.vellum.ledger.domain.LedgerSettings
import com.vellum.ledger.sync.LedgerApi
import com.vellum.ledger.ui.util.ExchangeRateProvider
import com.vellum.ledger.data.currentTimeMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.*

class SettingsRepository(
    private val database: LedgerDatabase,
    private val api: LedgerApi,
    private val exchangeRateProvider: ExchangeRateProvider
) {
    private val summaryMutex = Mutex()
    private val settingsMutex = Mutex()

    val settings: Flow<LedgerSettings> = database.state.map { it.settings }

    suspend fun setAutoSync(enabled: Boolean) = settingsMutex.withLock {
        database.updateSettings { it.copy(autoSync = enabled) }
    }

    suspend fun setDarkMode(enabled: Boolean?) = settingsMutex.withLock {
        database.updateSettings { it.copy(isDarkMode = enabled) }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) = settingsMutex.withLock {
        database.updateSettings { it.copy(isBiometricEnabled = enabled) }
    }

    suspend fun setCurrency(currency: String) = settingsMutex.withLock {
        exchangeRateProvider.refreshRates()
        database.updateSettings { it.copy(currency = currency) }
    }

    suspend fun setDailyBudget(amountCents: Long) = settingsMutex.withLock {
        database.updateSettings { it.copy(dailyBudget = amountCents) }
    }

    suspend fun refreshMonthlySummary(force: Boolean = false) = summaryMutex.withLock {
        val snapshot = database.state.value
        val settings = snapshot.settings
        val transactions = snapshot.transactions

        val now = currentTimeMillis()
        val tz = TimeZone.currentSystemDefault()
        val today = Instant.fromEpochMilliseconds(now).toLocalDateTime(tz).date
        val currentMonthKey = "${today.year}-${today.monthNumber.toString().padStart(2, '0')}"
        
        val currentMonthStart = LocalDate(today.year, today.month, 1).atStartOfDayIn(tz).toEpochMilliseconds()
        val currentMonthTransactions = transactions.filter { it.createdAt >= currentMonthStart }
        
        val currentTxCount = currentMonthTransactions.size
        val txCountAtCache = settings.transactionCountAtCacheTime
        val isExistingSummaryError = settings.monthlySummary?.contains("check back later", ignoreCase = true) == true || 
                                     settings.monthlySummary?.startsWith("Error") == true
        
        val isStale = currentTxCount - txCountAtCache >= 5
        
        if (!force && settings.summaryMonth == currentMonthKey && settings.monthlySummary != null && !isExistingSummaryError && !isStale) {
            return@withLock
        }

        if (currentMonthTransactions.isEmpty()) {
            return@withLock
        }

        try {
            val summary = api.requestMonthlySummary(currentMonthTransactions)
            settingsMutex.withLock {
                database.updateSettings { 
                    it.copy(
                        monthlySummary = summary, 
                        summaryMonth = currentMonthKey,
                        transactionCountAtCacheTime = currentTxCount
                    )
                }
            }
        } catch (e: Exception) {
            // Log error
        }
    }
}
