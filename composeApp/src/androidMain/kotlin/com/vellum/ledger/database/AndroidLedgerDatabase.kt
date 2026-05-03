package com.vellum.ledger.database

import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.vellum.ledger.db.LedgerDb
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

actual fun createLedgerDatabase(): LedgerDatabase {
    // Required to load SQLCipher native libraries
    System.loadLibrary("sqlcipher")

    // In a real production app, this passphrase would be securely managed (e.g., via Keystore)
    val passphrase = "vellum_secure_vault_key"
    val factory = SupportOpenHelperFactory(passphrase.toByteArray(Charsets.UTF_8))

    val driver = AndroidSqliteDriver(
        schema = LedgerDb.Schema,
        context = AndroidLedgerContext.appContext,
        name = "vellum_ledger_encrypted.db",
        factory = factory
    )
    return SqlDelightLedgerDatabase(driver)
}
