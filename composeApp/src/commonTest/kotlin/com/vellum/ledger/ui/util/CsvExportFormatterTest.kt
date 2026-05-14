package com.vellum.ledger.ui.util

import com.vellum.ledger.domain.LedgerSettings
import com.vellum.ledger.domain.LedgerSnapshot
import com.vellum.ledger.domain.LedgerTransaction
import com.vellum.ledger.domain.SyncStatus
import com.vellum.ledger.domain.TransactionType
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CsvExportFormatterTest {

    @Test
    fun zeroTransactionsProducesHeadersOnly() {
        val request = buildCsvExportRequest(LedgerSnapshot())

        assertTrue(request.csvContent.startsWith("date,description,category,amount,currency,original_currency,sync_status"))
        assertEquals(1, request.csvContent.trim().lines().size)
    }

    @Test
    fun exportIncludesSpecialCharactersAndOriginalCurrencyColumns() {
        val snapshot = LedgerSnapshot(
            transactions = listOf(
                LedgerTransaction(
                    id = "tx-1",
                    amount = 12_345L,
                    originalAmount = 12_345L,
                    originalCurrency = "USD ($)",
                    type = TransactionType.Expense,
                    category = "Food",
                    note = "Lunch, \"business\" meeting",
                    createdAt = 1_716_000_000_000L,
                    syncStatus = SyncStatus.Pending,
                ),
                LedgerTransaction(
                    id = "tx-2",
                    amount = 9_000L,
                    originalAmount = 10_000L,
                    originalCurrency = "USD ($)",
                    type = TransactionType.Income,
                    category = "Salary",
                    note = "May payout",
                    createdAt = 1_716_086_400_000L,
                    syncStatus = SyncStatus.Synced,
                ),
            ),
            settings = LedgerSettings(currency = "EUR (€)")
        )

        val request = buildCsvExportRequest(snapshot, nowMillis = 1_716_086_400_000L)
        val lines = request.csvContent.trim().lines()

        assertEquals("date,description,category,amount,currency,original_currency,sync_status", lines.first())
        assertContains(lines[1], "\"Lunch, \"\"business\"\" meeting\"")
        assertContains(lines[1], "\"EUR (€)\"")
        assertContains(lines[1], "\"USD ($)\"")
        assertContains(lines[1], "\"PENDING\"")
        assertContains(lines[2], "\"EUR (€)\"")
        assertContains(lines[2], "\"USD ($)\"")
        assertContains(lines[2], "\"SYNCED\"")
    }
}
