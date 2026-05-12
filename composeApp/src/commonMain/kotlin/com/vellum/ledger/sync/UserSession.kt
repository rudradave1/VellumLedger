package com.vellum.ledger.sync

interface UserSession {
    fun getToken(): String
    fun getUserId(): String
    fun updateSession(token: String, userId: String)
}

class SimpleUserSession(
    // TODO: Implement a real Login flow and retrieve these from secure storage (e.g. DataStore)
    // Using temporary demo credentials for testing
    private var token: String = "vellum_ledger_dev_token_2024", 
    private var userId: String = "dev_user_99"
) : UserSession {
    override fun getToken(): String = token
    override fun getUserId(): String = userId

    override fun updateSession(token: String, userId: String) {
        this.token = token
        this.userId = userId
    }

    companion object {
        val default = SimpleUserSession()
    }
}
