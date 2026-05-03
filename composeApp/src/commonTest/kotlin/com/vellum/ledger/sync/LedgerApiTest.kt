package com.vellum.ledger.sync

import com.vellum.ledger.domain.LedgerTransaction
import com.vellum.ledger.domain.SyncStatus
import com.vellum.ledger.domain.TransactionType
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LedgerApiTest {

    private val testTransaction = LedgerTransaction(
        id = "test-123",
        amount = 100.0,
        type = TransactionType.Expense,
        category = "Food",
        note = "Pizza",
        createdAt = 123456789L,
        syncStatus = SyncStatus.Pending
    )

    @Test
    fun testPushSuccess() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = "{\"success\":true}",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                })
            }
        }

        val userSession = SimpleUserSession()
        val api = KtorLedgerApi(client, userSession)
        
        api.push(testTransaction)
        
        assertEquals(1, mockEngine.requestHistory.size)
        val request = mockEngine.requestHistory[0]
        assertEquals(HttpMethod.Post, request.method)
        assertTrue(request.url.toString().contains("/transactions/push"))
        assertEquals("Bearer ${userSession.getToken()}", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun testPushFailure() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = "Unauthorized",
                status = HttpStatusCode.Unauthorized
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                })
            }
        }

        val api = KtorLedgerApi(client)
        
        val result = runCatching { api.push(testTransaction) }
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SyncException)
    }
}
