package com.vellum.ledger

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform