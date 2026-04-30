package com.vellum.ledger.data

import java.util.UUID

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun newLedgerId(): String = UUID.randomUUID().toString()
