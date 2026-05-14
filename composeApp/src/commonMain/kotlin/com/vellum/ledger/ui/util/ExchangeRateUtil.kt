package com.vellum.ledger.ui.util

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.vellum.ledger.database.LedgerDatabase
import com.vellum.ledger.data.currentTimeMillis

@Serializable
private data class ExchangeRatesResponse(
    val result: String,
    val base_code: String,
    val rates: Map<String, Double>
)

object ExchangeRateUtil {
    private var database: LedgerDatabase? = null

    // Default hardcoded fallback rates if no cache exists
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
        install(HttpTimeout) {
            requestTimeoutMillis = 5000
            connectTimeoutMillis = 5000
            socketTimeoutMillis = 5000
        }
    }

    fun initialize(db: LedgerDatabase) {
        database = db
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
                
                // Save to cache
                database?.saveExchangeRates(rates)
            }
        } catch (e: Exception) {
            println("ExchangeRateUtil: Failed to refresh rates: ${e.message}. Falling back to cache.")
            GlobalErrorHandler.handleError(Exception("Failed to fetch live rates. Using cached data.", e))
            
            // Attempt to load from cache if fetch fails
            loadFromCache()
        }
    }

    private suspend fun loadFromCache() {
        database?.let { db ->
            val cachedRates = db.loadExchangeRates()
            if (cachedRates.isNotEmpty()) {
                rates = cachedRates
                println("ExchangeRateUtil: Loaded rates from cache.")
            } else {
                println("ExchangeRateUtil: Cache is empty, using hardcoded defaults.")
            }
        }
    }
    
    fun isAvailable(): Boolean {
        // If we only have the default few rates, it might mean we've never synced
        return rates.size > 10 || database != null
    }

    fun convert(amount: Long, from: String, to: String): Long {
        if (from == "LEGACY" || from == to) return amount
        
        val fromCode = from.substringBefore(" ").uppercase()
        val toCode = to.substringBefore(" ").uppercase()
        
        val fromRate = rates[fromCode] ?: 1.0
        val toRate = rates[toCode] ?: 1.0
        
        // Convert using Double for precision
        val inUsd = amount.toDouble() / fromRate
        return (inUsd * toRate).toLong()
    }
}
