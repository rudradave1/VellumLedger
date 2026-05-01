package com.vellum.ledger.database

import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.vellum.ledger.db.LedgerDb

actual fun createLedgerDatabase(): LedgerDatabase {
    val driver = AndroidSqliteDriver(
        schema = LedgerDb.Schema,
        context = AndroidLedgerContext.appContext,
        name = "vellum_ledger_v2.db",
    )
    return SqlDelightLedgerDatabase(driver)
}
