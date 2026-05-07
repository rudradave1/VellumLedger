package com.vellum.ledger.sync

interface UserSession {
    fun getToken(): String
    fun getUserId(): String
}

class SimpleUserSession(
    // TODO: Implement a real Login flow and retrieve these from secure storage (e.g. DataStore)
    // Using temporary demo credentials for testing
    private val token: String = "vellum_ledger_dev_token_2024", 
    private val userId: String = "dev_user_99"
) : UserSession {
    override fun getToken(): String = token
    override fun getUserId(): String = userId
}
