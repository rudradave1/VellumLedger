package com.vellum.ledger.database

import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vellum.ledger.db.LedgerDb
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import kotlin.text.Charsets

actual fun createLedgerDatabase(): LedgerDatabase {
    // Required to load SQLCipher native libraries
    System.loadLibrary("sqlcipher")

    // TODO: In a production app, do NOT hardcode this. 
    // Use Android Keystore to generate and retrieve a unique key for the user.
    val passphrase = "vellum_secure_vault_key_change_me_in_production"
    val factory = SupportOpenHelperFactory(passphrase.toByteArray(Charsets.UTF_8))

    val driver = AndroidSqliteDriver(
        schema = LedgerDb.Schema,
        context = AndroidLedgerContext.appContext,
        name = "vellum_ledger_encrypted_v2.db",
        factory = factory,
        callback = object : AndroidSqliteDriver.Callback(LedgerDb.Schema) {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.enableWriteAheadLogging()
            }
        },
    )
    return SqlDelightLedgerDatabase(driver)
}
