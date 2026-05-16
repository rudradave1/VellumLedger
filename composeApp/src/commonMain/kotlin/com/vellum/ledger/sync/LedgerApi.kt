package com.vellum.ledger.sync

import com.vellum.ledger.data.isDebugBuild
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface LedgerApi {
    /**
     * Pushes multiple transactions to the remote server in a single batch.
     * Throws [SyncException] if the sync fails.
     */
    suspend fun pushBatch(transactions: List<LedgerTransaction>): PushResponse

    /**
     * Pulls all backed-up transactions for the current device identity.
     */
    suspend fun pullBackupTransactions(): PullResponse

    /**
     * Requests a monthly summary insight from the server.
     * Returns the insight string.
     */
    suspend fun requestMonthlySummary(transactions: List<LedgerTransaction>): String
}

data class SyncResult(
    val attempted: Int = 0,
    val synced: Int = 0,
    val failed: Int = 0,
)

class KtorLedgerApi(
    private val deviceIdentityManager: DeviceIdentityManager,
    private val client: HttpClient = createDefaultHttpClient(),
    private val userSession: UserSession
) : LedgerApi {
    private val authMutex = Mutex()
    
    companion object {
        private const val BASE_URL = "https://vellum-ledger-api-production.up.railway.app"
    }

    private suspend fun authenticate(): Unit = authMutex.withLock {
        val deviceId = deviceIdentityManager.getOrCreateDeviceId()
        val authRequest = AuthRequest(deviceId = deviceId)

        if (isDebugBuild) {
            println("LedgerApi: Attempting authentication for device $deviceId...")
        }
        
        try {
            // Try login first
            var response = client.post("$BASE_URL/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(authRequest)
            }
            
            // If login fails (user doesn't exist), try register
            if (response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.NotFound) {
                if (isDebugBuild) {
                    println("LedgerApi: Device not registered, attempting registration...")
                }
                response = client.post("$BASE_URL/auth/register") {
                    contentType(ContentType.Application.Json)
                    setBody(authRequest)
                }
            }
            
            // If registration fails with Conflict, try one final login
            if (response.status == HttpStatusCode.Conflict) {
                if (isDebugBuild) {
                    println("LedgerApi: Registration conflict (409). Attempting final login...")
                }
                response = client.post("$BASE_URL/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(authRequest)
                }
            }
            
            if (response.status.isSuccess()) {
                val authResponse = response.body<AuthResponse>()
                userSession.updateSession(authResponse.token, authResponse.userId)
                deviceIdentityManager.saveSession(authResponse.token, authResponse.userId)
                if (isDebugBuild) {
                    println("LedgerApi: Authentication successful.")
                }
            } else {
                val error = "Authentication failed: ${response.status}"
                if (isDebugBuild) {
                    println("LedgerApi: $error")
                }
                throw SyncException(error)
            }
        } catch (e: Exception) {
            if (isDebugBuild) {
                println("LedgerApi: Error during authentication: ${e.message}")
            }
            if (e !is SyncException) {
                throw SyncException("Auth failure: ${e.message}")
            } else throw e
        }
    }

    override suspend fun pushBatch(transactions: List<LedgerTransaction>): PushResponse {
        if (transactions.isEmpty()) return PushResponse()
        
        return executeWithAuthRetry {
            val userId = userSession.getUserId()
            val networkTransactions = transactions.map { it.toNetwork(userId) }
            val requestBody = PushRequest(transactions = networkTransactions)
            if (isDebugBuild) {
                println("LedgerApi: Pushing ${transactions.size} transactions.")
            }

            val response = client.post("$BASE_URL/transactions/push") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer ${userSession.getToken()}")
                setBody(requestBody)
            }
            
            if (response.status == HttpStatusCode.Unauthorized) {
                throw AuthException()
            }
            
            if (!response.status.isSuccess()) {
                val errorBody = response.bodyAsText().ifBlank { response.status.description }
                throw SyncException("Push failed: $errorBody")
            }

            val responseText = response.bodyAsText().trim()
            if (responseText.isBlank()) {
                return@executeWithAuthRetry PushResponse(success = true)
            }

            runCatching {
                Json.decodeFromString(PushResponse.serializer(), responseText)
            }.getOrElse {
                if (isDebugBuild) {
                    println("LedgerApi: Push response was not JSON, treating as success. Body=$responseText")
                }
                PushResponse(success = true)
            }
        }
    }

    override suspend fun pullBackupTransactions(): PullResponse {
        return executeWithAuthRetry {
            val response = client.get("$BASE_URL/transactions/pull") {
                header(HttpHeaders.Authorization, "Bearer ${userSession.getToken()}")
            }

            if (response.status == HttpStatusCode.Unauthorized) {
                throw AuthException()
            }

            if (!response.status.isSuccess()) {
                throw SyncException("Pull failed: ${response.status}")
            }

            response.body()
        }
    }

    override suspend fun requestMonthlySummary(transactions: List<LedgerTransaction>): String {
        return executeWithAuthRetry {
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
            
            if (isDebugBuild) {
                println("LedgerApi: requestMonthlySummary: Sending ${transactions.size} transactions.")
            }

            val response = client.post("$BASE_URL/insights/monthly") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer ${userSession.getToken()}")
                setBody(requestBody)
            }

            if (response.status == HttpStatusCode.Unauthorized) {
                throw AuthException()
            }

            if (response.status == HttpStatusCode.TooManyRequests) {
                throw SyncException("Rate limit reached")
            }

            if (!response.status.isSuccess()) {
                throw SyncException("Summary failed: ${response.status}")
            }

            response.bodyAsText()
        }
    }

    private suspend fun <T> executeWithAuthRetry(block: suspend () -> T): T {
        if (!userSession.isAuthenticated) {
            if (isDebugBuild) {
                println("LedgerApi: Session not authenticated. Initializing...")
            }
            authenticate()
        }
        return try {
            block()
        } catch (e: AuthException) {
            if (isDebugBuild) {
                println("LedgerApi: Unauthorized. Retrying with new token...")
            }
            authenticate()
            try {
                block()
            } catch (retryException: AuthException) {
                val finalError = "Authentication failed after retry."
                com.vellum.ledger.ui.util.GlobalErrorHandler.handleError(SyncException(finalError))
                throw SyncException(finalError)
            }
        } catch (e: Exception) {
            if (e !is SyncException) {
                com.vellum.ledger.ui.util.GlobalErrorHandler.handleError(e)
            }
            throw e
        }
    }
}

private class AuthException : Exception()


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
        level = if (isDebugBuild) LogLevel.ALL else LogLevel.NONE
    }
}

// Compatibility alias
// typealias LedgerApiImpl = KtorLedgerApi
