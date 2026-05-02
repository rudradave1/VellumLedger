package com.vellum.ledger.database

import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.vellum.ledger.db.LedgerDb

actual fun createLedgerDatabase(): LedgerDatabase {
    // In a real production app, this passphrase would be securely managed (e.g., via Keystore)
    val passphrase = "vellum_secure_vault_key"
    val factory = net.sqlcipher.database.SupportFactory(passphrase.toByteArray())

    val driver = AndroidSqliteDriver(
        schema = LedgerDb.Schema,
        context = AndroidLedgerContext.appContext,
        name = "vellum_ledger_encrypted.db",
        factory = factory
    )
    return SqlDelightLedgerDatabase(driver)
}
