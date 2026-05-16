package com.vellum.ledger.sync

interface SecureStorage {
    fun get(key: String): String?
    fun set(key: String, value: String?)
}
