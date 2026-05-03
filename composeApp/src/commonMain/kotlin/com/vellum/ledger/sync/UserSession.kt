package com.vellum.ledger.sync

interface UserSession {
    fun getToken(): String
    fun getUserId(): String
}

class SimpleUserSession(
    private val token: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJ2ZWxsdW0tbGVkZ2VyIiwiYXVkIjoidmVsbHVtLWxlZGdlci11c2VycyIsInVzZXJJZCI6IjJkYWVkYjllLTk5MjItNGU5OS1iYjYzLWQ1NzdjNTc1ZmJiNCIsImV4cCI6MTc4MDM5NzI2Nn0.Ok66NmdQcqI9PpjfA8BcDLG3j61Ew79RtNy6fraSLLE",
    private val userId: String = "2daedb9e-9922-4e99-bb63-d577c575fbb4"
) : UserSession {
    override fun getToken(): String = token
    override fun getUserId(): String = userId
}
