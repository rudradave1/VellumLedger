package com.vellum.ledger.ui.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import io.ktor.client.plugins.ResponseException
import io.ktor.client.network.sockets.ConnectTimeoutException

object GlobalErrorHandler {
    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents = _errorEvents.asSharedFlow()

    suspend fun handleError(t: Throwable) {
        val message = when (t) {
            is ConnectTimeoutException -> "Network timeout. Please check your connection."
            is ResponseException -> "Server error: ${t.response.status}"
            else -> {
                val className = t::class.simpleName ?: ""
                if (className.contains("Sql", ignoreCase = true) || 
                    t.message?.contains("SQL", ignoreCase = true) == true) {
                    "Database error. Please try again or clear data."
                } else {
                    t.message ?: "An unexpected error occurred."
                }
            }
        }
        _errorEvents.emit(message)
    }
}
