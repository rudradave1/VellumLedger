package com.vellum.ledger.database

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.vellum.ledger.db.LedgerDb

actual fun createLedgerDatabase(): LedgerDatabase {
    val driver = NativeSqliteDriver(LedgerDb.Schema, "vellum_ledger_v2.db")
    return SqlDelightLedgerDatabase(driver)
}
