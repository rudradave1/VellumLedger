package com.vellum.ledger.data

expect fun currentTimeMillis(): Long

expect fun newLedgerId(): String

expect val appVersion: String

expect fun shareText(text: String, title: String)
