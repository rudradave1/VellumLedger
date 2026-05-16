package com.vellum.ledger.sync

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RemoteConfig(private val client: HttpClient) {
    private val mutex = Mutex()
    private var cachedUrl: String? = null
    private val defaultUrl = "https://vellum-ledger-api-production.up.railway.app"
    private val configUrl = "https://rudradave1.github.io/vellumledger-privacy/config.json"

    suspend fun getApiUrl(): String = mutex.withLock {
        cachedUrl?.let { return it }

        return try {
            val response = client.get(configUrl)
            val config = response.body<Map<String, String>>()
            val url = config["api_url"] ?: defaultUrl
            cachedUrl = url
            url
        } catch (e: Exception) {
            defaultUrl
        }
    }
}
