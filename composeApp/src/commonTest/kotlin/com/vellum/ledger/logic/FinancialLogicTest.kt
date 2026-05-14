package com.vellum.ledger.logic

import com.vellum.ledger.ui.util.ExchangeRateUtil
import com.vellum.ledger.ui.util.formatMoney
import kotlin.test.Test
import kotlin.test.assertEquals

class FinancialLogicTest {

    @Test
    fun testCurrencyConversionPrecision() {
        // Test conversion from JPY to USD (15000 -> 100)
        val inUsd = ExchangeRateUtil.convert(15_000L, "JPY (¥)", "USD ($)")
        assertEquals(100L, inUsd, "Conversion from JPY to USD should be accurate")
        
        // Test conversion from USD to INR (1.0 -> 83.0)
        val inInr = ExchangeRateUtil.convert(100L, "USD ($)", "INR (₹)")
        assertEquals(8300L, inInr, "Conversion from USD to INR should be accurate")
        
        // Test round trip (USD -> EUR -> USD)
        val initial = 10000L
        val inEur = ExchangeRateUtil.convert(initial, "USD ($)", "EUR (€)")
        val backToUsd = ExchangeRateUtil.convert(inEur, "EUR (€)", "USD ($)")
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
