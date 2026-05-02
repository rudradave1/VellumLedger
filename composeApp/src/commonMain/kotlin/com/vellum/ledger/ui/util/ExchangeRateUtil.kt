package com.vellum.ledger.ui.util

object ExchangeRateUtil {
    // Hardcoded rates for demonstration. In a real app, these would be fetched from an API.
    private val rates = mapOf(
        "USD" to 1.0,
        "EUR" to 0.92,
        "GBP" to 0.79,
        "INR" to 83.0,
        "JPY" to 150.0,
        "CNY" to 7.2
    )

    fun convert(amount: Double, from: String, to: String): Double {
        val fromCode = from.substringBefore(" ")
        val toCode = to.substringBefore(" ")
        
        val fromRate = rates[fromCode] ?: 1.0
        val toRate = rates[toCode] ?: 1.0
        
        // Convert to USD first, then to target
        val inUsd = amount / fromRate
        return inUsd * toRate
    }
}
