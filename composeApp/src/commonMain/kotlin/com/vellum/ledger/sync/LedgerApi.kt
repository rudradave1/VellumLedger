package com.vellum.ledger.sync

import com.vellum.ledger.domain.LedgerTransaction
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class LedgerApi {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    suspend fun push(transaction: LedgerTransaction) {
        // Placeholder URL. In a real app, this would be your backend.
        val response = client.post("https://api.vellumledger.com/v1/sync") {
            contentType(ContentType.Application.Json)
            setBody(transaction)
        }
        
        if (!response.status.isSuccess()) {
            error("Sync failed with status: ${response.status}")
        }
    }
}
