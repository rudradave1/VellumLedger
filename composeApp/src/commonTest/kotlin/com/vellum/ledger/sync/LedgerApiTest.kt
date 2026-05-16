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
        amount = 100L,
        originalAmount = 100L,
        originalCurrency = "USD ($)",
        type = TransactionType.Expense,
        category = "Food",
        note = "Pizza",
        createdAt = 123456789L,
        syncStatus = SyncStatus.Pending,
        localVersion = 1,
        serverVersion = 0
    )

    private class FakeSecureStorage : SecureStorage {
        private val map = mutableMapOf<String, String?>()
        override fun get(key: String): String? = map[key]
        override fun set(key: String, value: String?) { map[key] = value }
    }

    @Test
    fun testPushSuccess() = runTest {
        val mockEngine = MockEngine { request ->
            if (request.url.toString().contains("config.json")) {
                respond(
                    content = "{\"api_url\":\"https://mock-api.com\"}",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            } else {
                respond(
                    content = "{\"success\":true}",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                })
            }
        }

        val deviceIdentityManager = DeviceIdentityManager(InMemoryPreferencesDataStore(), FakeSecureStorage())
        val userSession = FakeUserSession(token = "token-123", userId = "user-123")
        val remoteConfig = RemoteConfig(client)
        val api = KtorLedgerApi(deviceIdentityManager, remoteConfig, client, userSession)
        
        api.pushBatch(listOf(testTransaction))
        
        // requestHistory[0] is config.json, requestHistory[1] is transactions/push
        assertEquals(2, mockEngine.requestHistory.size)
        val request = mockEngine.requestHistory[1]
        assertEquals(HttpMethod.Post, request.method)
        assertTrue(request.url.toString().contains("/transactions/push"))
        assertEquals("Bearer ${userSession.getToken()}", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun testPushFailure() = runTest {
        val mockEngine = MockEngine { request ->
            when {
                request.url.toString().contains("config.json") -> respond(
                    content = "{\"api_url\":\"https://mock-api.com\"}",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                request.url.encodedPath.endsWith("/transactions/push") -> respond(
                    content = "Unauthorized",
                    status = HttpStatusCode.Unauthorized
                )
                request.url.encodedPath.endsWith("/auth/login") -> respond(
                    content = "Unauthorized",
                    status = HttpStatusCode.Unauthorized
                )
                request.url.encodedPath.endsWith("/auth/register") -> respond(
                    content = "Unauthorized",
                    status = HttpStatusCode.Unauthorized
                )
                else -> error("Unexpected path ${request.url}")
            }
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                })
            }
        }

        val api = KtorLedgerApi(
            DeviceIdentityManager(InMemoryPreferencesDataStore(), FakeSecureStorage()),
            RemoteConfig(client),
            client,
            FakeUserSession(token = "token-123", userId = "user-123")
        )
        
        val result = runCatching { api.pushBatch(listOf(testTransaction)) }
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SyncException)
    }
}

private class FakeUserSession : UserSession {
    private var token: String
    private var userId: String

    constructor(token: String = "", userId: String = "") {
        this.token = token
        this.userId = userId
    }

    override fun getToken(): String = token
    override fun getUserId(): String = userId
    override fun updateSession(token: String, userId: String) {
        this.token = token
        this.userId = userId
    }

    override val isAuthenticated: Boolean
        get() = token.isNotEmpty()

    override suspend fun initialize() = Unit
}

private class InMemoryPreferencesDataStore : androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> {
    private val state = kotlinx.coroutines.flow.MutableStateFlow(androidx.datastore.preferences.core.emptyPreferences())

    override val data = state

    override suspend fun updateData(transform: suspend (t: androidx.datastore.preferences.core.Preferences) -> androidx.datastore.preferences.core.Preferences): androidx.datastore.preferences.core.Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}
