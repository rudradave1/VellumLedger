package com.vellum.ledger.sync

import com.vellum.ledger.domain.LedgerTransaction
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface LedgerApi {
    /**
     * Pushes a single transaction to the remote server.
     * Throws [SyncException] if the sync fails.
     */
    suspend fun push(transaction: LedgerTransaction)

    /**
     * Requests a monthly summary insight from the server.
     * Returns the insight string.
     */
    suspend fun requestMonthlySummary(transactions: List<LedgerTransaction>): String
}

class KtorLedgerApi(
    private val client: HttpClient = createDefaultHttpClient(),
    private val userSession: UserSession = SimpleUserSession()
) : LedgerApi {
    
    companion object {
        private const val BASE_URL = "https://vellum-ledger-api-production.up.railway.app"
    }

    override suspend fun push(transaction: LedgerTransaction) {
        val userId = userSession.getUserId()
        val networkTransaction = transaction.toNetwork(userId)
        val requestBody = PushRequest(transactions = listOf(networkTransaction))
        println("LedgerApi: Pushing request body: $requestBody")

        val response = try {
            client.post("$BASE_URL/transactions/push") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer ${userSession.getToken()}")
                setBody(requestBody)
            }
        } catch (e: Exception) {
            println("LedgerApi: Network Error: ${e.message}")
            throw SyncException("Network error: ${e.message}")
        }

        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText().ifBlank { response.status.description }
            println("LedgerApi: Push failed (${response.status}): $errorBody")
            throw SyncException("Sync failed: $errorBody")
        }
        
        println("LedgerApi: Successfully synced transaction ${transaction.id}")
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
        println("LedgerApi: Request Body: ${Json.encodeToString(requestBody)}")

        val response = try {
            client.post("$BASE_URL/insights/monthly") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer ${userSession.getToken()}")
                setBody(requestBody)
            }
        } catch (e: Exception) {
            println("LedgerApi: Network Error: ${e.message}")
            throw SyncException("Network error: ${e.message}")
        }

        val responseBody = response.bodyAsText()
        println("LedgerApi: Response Status: ${response.status}")
        println("LedgerApi: Response Body: $responseBody")

        return when (response.status) {
            HttpStatusCode.OK -> responseBody
            HttpStatusCode.TooManyRequests -> "Summary already generated this month. Check back later."
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
    override suspend fun push(transaction: LedgerTransaction) {
        if (randomFail && (0..10).random() > 7) throw SyncException("Random failure")
    }

    override suspend fun requestMonthlySummary(transactions: List<LedgerTransaction>): String {
        return "This is a fake AI insight summary for your transactions."
    }
}
