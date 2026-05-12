package com.vellum.ledger.sync

import com.vellum.ledger.domain.LedgerTransaction
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

interface LedgerApi {
    /**
     * Pushes multiple transactions to the remote server in a single batch.
     * Throws [SyncException] if the sync fails.
     */
    suspend fun pushBatch(transactions: List<LedgerTransaction>)

    /**
     * Requests a monthly summary insight from the server.
     * Returns the insight string.
     */
    suspend fun requestMonthlySummary(transactions: List<LedgerTransaction>): String
}

class KtorLedgerApi(
    private val client: HttpClient = createDefaultHttpClient(),
    private val userSession: UserSession = SimpleUserSession.default
) : LedgerApi {
    
    companion object {
        private const val BASE_URL = "https://vellum-ledger-api-production.up.railway.app"
    }

    private suspend fun authenticate() {
        println("LedgerApi: Attempting auto-registration for dev user...")
        try {
            val response = client.post("$BASE_URL/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(AuthRequest(email = "dev_user_${(1..1000).random()}@vellum.com", password = "password123"))
            }
            if (response.status.isSuccess()) {
                val authResponse = response.body<AuthResponse>()
                userSession.updateSession(authResponse.token, authResponse.userId)
                println("LedgerApi: Successfully registered and obtained new token.")
            } else {
                println("LedgerApi: Auto-registration failed: ${response.status}")
            }
        } catch (e: Exception) {
            println("LedgerApi: Error during auto-registration: ${e.message}")
        }
    }

    override suspend fun pushBatch(transactions: List<LedgerTransaction>) {
        if (transactions.isEmpty()) return
        
        var userId = userSession.getUserId()
        var networkTransactions = transactions.map { it.toNetwork(userId) }
        var requestBody = PushRequest(transactions = networkTransactions)
        println("LedgerApi: Pushing ${transactions.size} transactions.")

        var response = try {
            client.post("$BASE_URL/transactions/push") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer ${userSession.getToken()}")
                setBody(requestBody)
            }
        } catch (e: Exception) {
            println("LedgerApi: Network Error: ${e.message}")
            throw SyncException("Network error: ${e.message}")
        }

        if (response.status == HttpStatusCode.Unauthorized) {
            println("LedgerApi: Unauthorized (401). Trying to re-authenticate...")
            authenticate()
            
            // Retry once with new token
            userId = userSession.getUserId()
            networkTransactions = transactions.map { it.toNetwork(userId) }
            requestBody = PushRequest(transactions = networkTransactions)
            
            response = client.post("$BASE_URL/transactions/push") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer ${userSession.getToken()}")
                setBody(requestBody)
            }
        }

        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText().ifBlank { response.status.description }
            println("LedgerApi: Push failed (${response.status}): $errorBody")
            throw SyncException("Sync failed: $errorBody")
        }
        
        println("LedgerApi: Successfully synced batch of ${transactions.size} items.")
    }

    override suspend fun requestMonthlySummary(transactions: List<LedgerTransaction>): String {
        val dtos = transactions.map {
            TransactionSummaryDto(
                id = it.id,
                amount = it.amount,
                type = it.type.name.uppercase(),
                category = it.category,
                note = it.note,
                createdAt = it.createdAt
            )
        }
        val requestBody = SummaryRequest(transactions = dtos)
        
        println("LedgerApi: requestMonthlySummary: Sending ${transactions.size} transactions.")

        var response = try {
            client.post("$BASE_URL/insights/monthly") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer ${userSession.getToken()}")
                setBody(requestBody)
            }
        } catch (e: Exception) {
            println("LedgerApi: Network Error: ${e.message}")
            throw SyncException("Network error: ${e.message}")
        }

        if (response.status == HttpStatusCode.Unauthorized) {
            println("LedgerApi: Unauthorized (401) during summary request. Trying to re-authenticate...")
            authenticate()
            
            response = client.post("$BASE_URL/insights/monthly") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer ${userSession.getToken()}")
                setBody(requestBody)
            }
        }

        val responseBody = response.bodyAsText()
        println("LedgerApi: Response Status: ${response.status}")
        println("LedgerApi: Response Body: $responseBody")

        return when (response.status) {
            HttpStatusCode.OK -> responseBody
            HttpStatusCode.TooManyRequests -> {
                println("LedgerApi: Rate limit hit (429).")
                throw SyncException("Rate limit reached")
            }
            else -> {
                println("LedgerApi: Summary request failed (${response.status}): $responseBody")
                throw SyncException("Failed to get summary: $responseBody")
            }
        }
    }
}

class SyncException(message: String) : Exception(message)

/**
 * Factory function for the default Ktor HttpClient used in synchronization.
 */
private fun createDefaultHttpClient() = HttpClient {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            prettyPrint = true
        })
    }
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                println("Ktor: $message")
            }
        }
        level = LogLevel.ALL
    }
}

// Compatibility alias
typealias LedgerApiImpl = KtorLedgerApi

class FakeLedgerApi(private val randomFail: Boolean = false) : LedgerApi {
    override suspend fun pushBatch(transactions: List<LedgerTransaction>) {
        if (randomFail && (0..10).random() > 7) throw SyncException("Random failure")
    }

    override suspend fun requestMonthlySummary(transactions: List<LedgerTransaction>): String {
        return "This is a fake AI insight summary for your transactions."
    }
}
