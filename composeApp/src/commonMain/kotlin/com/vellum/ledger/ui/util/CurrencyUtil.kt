package com.vellum.ledger.ui.util

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow

fun formatMoney(amountCents: Long, currency: String = "USD ($)", compact: Boolean = false): String {
    val symbol = currency.substringAfter("(").substringBefore(")")
    val value = abs(amountCents)
    
    if (compact && value >= 100000) { // Compact only for >= 1,000.00
        val dollarValue = value / 100.0
        val suffixes = arrayOf("", "K", "M", "B", "T")
        val i = (ln(dollarValue) / ln(1000.0)).toInt().coerceAtMost(suffixes.size - 1)
        val shortValue = dollarValue / 1000.0.pow(i.toDouble())
        
        // Manual rounding to 1 decimal place without scientific notation
        val rounded = ((shortValue * 10).toLong()) / 10.0
        val roundedStr = if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
        
        return "$symbol$roundedStr${suffixes[i]}"
    }

    val dollars = value / 100
    val cents = value % 100
    
    val dollarsStr = dollars.toString().reversed().chunked(3).joinToString(",").reversed()
    return "$symbol$dollarsStr.${cents.toString().padStart(2, '0')}"
}


fun extractCurrencySymbol(currency: String): String {
    return currency.substringAfter("(").substringBefore(")")
}
