package com.vellum.ledger.ui.util

import kotlin.math.abs

fun formatMoney(amount: Double, currency: String = "USD ($)"): String {
    val symbol = currency.substringAfter("(").substringBefore(")")
    val totalCents = (abs(amount) * 100 + 0.5).toLong()
    val dollars = totalCents / 100
    val cents = totalCents % 100
    val dollarsStr = dollars.toString().reversed().chunked(3).joinToString(",").reversed()
    return "$symbol$dollarsStr.${cents.toString().padStart(2, '0')}"
}

fun extractCurrencySymbol(currency: String): String {
    return currency.substringAfter("(").substringBefore(")")
}
