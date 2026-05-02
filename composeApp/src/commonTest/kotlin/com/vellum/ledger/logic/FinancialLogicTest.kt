package com.vellum.ledger.logic

import com.vellum.ledger.ui.util.ExchangeRateUtil
import com.vellum.ledger.ui.util.formatMoney
import kotlin.test.Test
import kotlin.test.assertEquals

class FinancialLogicTest {

    @Test
    fun testCurrencyConversionPrecision() {
        // Test conversion from JPY to USD (150 -> 1.0)
        val inUsd = ExchangeRateUtil.convert(150.0, "JPY (¥)", "USD ($)")
        assertEquals(1.0, inUsd, "Conversion from JPY to USD should be accurate")
        
        // Test conversion from USD to INR (1.0 -> 83.0)
        val inInr = ExchangeRateUtil.convert(1.0, "USD ($)", "INR (₹)")
        assertEquals(83.0, inInr, "Conversion from USD to INR should be accurate")
        
        // Test round trip (USD -> EUR -> USD)
        val initial = 100.0
        val inEur = ExchangeRateUtil.convert(initial, "USD ($)", "EUR (€)")
        val backToUsd = ExchangeRateUtil.convert(inEur, "EUR (€)", "USD ($)")
        assertEquals(initial, backToUsd, 0.0001, "Round trip conversion should maintain precision")
    }

    @Test
    fun testCompactMoneyFormatting() {
        assertEquals("$1.0B", formatMoney(1_000_000_000.0, "USD ($)", compact = true))
        assertEquals("$2.1T", formatMoney(2_147_483_647_000.0, "USD ($)", compact = true))
        assertEquals("$950.5M", formatMoney(950_500_000.0, "USD ($)", compact = true))
        assertEquals("$500.0", formatMoney(500.0, "USD ($)", compact = true), "Amounts < 1000 should not be compacted")
    }

    @Test
    fun testStandardMoneyFormatting() {
        assertEquals("$1,234.56", formatMoney(1234.562, "USD ($)"))
        assertEquals("₹83,000.00", formatMoney(83000.0, "INR (₹)"))
    }
}
