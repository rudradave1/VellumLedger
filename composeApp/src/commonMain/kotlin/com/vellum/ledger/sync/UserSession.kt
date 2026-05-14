package com.vellum.ledger.sync

import kotlinx.coroutines.flow.MutableStateFlow

interface UserSession {
    fun getToken(): String
    fun getUserId(): String
    fun updateSession(token: String, userId: String)
    val isAuthenticated: Boolean
    suspend fun initialize()
}

private data class SessionData(val token: String, val userId: String)

class SimpleUserSession(
    private val deviceIdentityManager: DeviceIdentityManager
) : UserSession {

    private val session = MutableStateFlow(SessionData("", ""))

    override suspend fun initialize() {
        deviceIdentityManager.getSavedSession()?.let { saved ->
            session.value = SessionData(saved.token, saved.userId)
        }
    }

    override fun getToken(): String = session.value.token
    override fun getUserId(): String = session.value.userId

    override fun updateSession(token: String, userId: String) {
        session.value = SessionData(token, userId)
    }

    override val isAuthenticated: Boolean
        get() = session.value.token.isNotEmpty()
}
