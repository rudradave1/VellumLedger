package com.vellum.ledger.ui.util

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class ExchangeRatesResponse(
    val result: String,
    val base_code: String,
    val rates: Map<String, Double>
)

object ExchangeRateUtil {
    // Default fallback rates
    private var rates = mapOf(
        "USD" to 1.0,
        "EUR" to 0.92,
        "GBP" to 0.79,
        "INR" to 83.0,
        "JPY" to 150.0,
        "CNY" to 7.2
    )

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { 
                ignoreUnknownKeys = true 
                coerceInputValues = true
            })
        }
    }

    /**
     * Fetches live exchange rates from a public API.
     */
    suspend fun refreshRates() {
        try {
            val response: ExchangeRatesResponse = client.get("https://open.er-api.com/v6/latest/USD").body()
            if (response.result == "success") {
                rates = response.rates
                println("ExchangeRateUtil: Rates updated successfully from live API.")
            }
        } catch (e: Exception) {
            println("ExchangeRateUtil: Failed to refresh rates: ${e.message}")
        }
    }

    fun convert(amount: Long, from: String, to: String): Long {
        val fromCode = from.substringBefore(" ").uppercase()
        val toCode = to.substringBefore(" ").uppercase()
        
        val fromRate = rates[fromCode] ?: 1.0
        val toRate = rates[toCode] ?: 1.0
        
        // Convert to USD first, then to target
        val inUsd = amount / fromRate
        return (inUsd * toRate).toLong()
    }
}
