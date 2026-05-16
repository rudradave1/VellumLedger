package com.vellum.ledger.logic

import com.vellum.ledger.ui.util.ExchangeRateProvider
import com.vellum.ledger.ui.util.formatMoney
import com.vellum.ledger.database.LedgerDatabase
import com.vellum.ledger.domain.LedgerSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FinancialLogicTest {

    private class FakeDatabase : LedgerDatabase {
        override val state = MutableStateFlow(LedgerSnapshot())
        override suspend fun insertTransactionWithQueue(transaction: com.vellum.ledger.domain.LedgerTransaction, queueItem: com.vellum.ledger.domain.SyncQueueItem) {}
        override suspend fun restoreTransaction(transaction: com.vellum.ledger.domain.LedgerTransaction): Boolean = true
        override suspend fun pendingQueueItems(): List<com.vellum.ledger.domain.SyncQueueItem> = emptyList()
        override suspend fun markSynced(transactionId: String, queueItemId: String, serverVersion: Int) {}
        override suspend fun markSyncing(transactionId: String) {}
        override suspend fun markFailed(transactionId: String) {}
        override suspend fun markPending(transactionId: String) {}
        override suspend fun updateSettings(transform: (com.vellum.ledger.domain.LedgerSettings) -> com.vellum.ledger.domain.LedgerSettings) {}
        override suspend fun insertCard(card: com.vellum.ledger.domain.LedgerCard) {}
        override suspend fun deleteCard(cardId: String) {}
        override suspend fun deleteTransaction(transactionId: String) {}
        override suspend fun clearAll() {}
        override suspend fun saveExchangeRates(rates: Map<String, Double>) {}
        override suspend fun loadExchangeRates(): Map<String, Double> = emptyMap()
    }

    @Test
    fun testCurrencyConversionPrecision() = runTest {
        val provider = ExchangeRateProvider(FakeDatabase())
        
        // Test conversion from JPY to USD (15000 -> 100)
        val inUsd = provider.convert(15_000L, "JPY (¥)", "USD ($)")
        assertEquals(100L, inUsd, "Conversion from JPY to USD should be accurate")
        
        // Test conversion from USD to INR (1.0 -> 83.0)
        val inInr = provider.convert(100L, "USD ($)", "INR (₹)")
        assertEquals(8300L, inInr, "Conversion from USD to INR should be accurate")
        
        // Test round trip (USD -> EUR -> USD)
        val initial = 10000L
        val inEur = provider.convert(initial, "USD ($)", "EUR (€)")
        val backToUsd = provider.convert(inEur, "EUR (€)", "USD ($)")
        assertEquals(initial, backToUsd, "Round trip conversion should maintain precision")
    }

    @Test
    fun testCompactMoneyFormatting() {
        assertEquals("$1B", formatMoney(100_000_000_000L, "USD ($)", compact = true))
        assertEquals("$2.1T", formatMoney(214_748_364_700_000L, "USD ($)", compact = true))
        assertEquals("$950.5M", formatMoney(95_050_000_000L, "USD ($)", compact = true))
        assertEquals("$500.00", formatMoney(50_000L, "USD ($)", compact = true), "Amounts < 1000 should not be compacted")
    }

    @Test
    fun testStandardMoneyFormatting() {
        assertEquals("$1,234.56", formatMoney(123_456L, "USD ($)"))
        assertEquals("₹83,000.00", formatMoney(8_300_000L, "INR (₹)"))
    }
}
