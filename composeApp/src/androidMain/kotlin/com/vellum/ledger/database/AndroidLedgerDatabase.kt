package com.vellum.ledger.database

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vellum.ledger.db.LedgerDb
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import kotlin.text.Charsets

actual fun createLedgerDatabase(): LedgerDatabase {
    // Required to load SQLCipher native libraries
    System.loadLibrary("sqlcipher")

    val passphrase = getOrCreateDatabaseKey()
    val factory = SupportOpenHelperFactory(passphrase)

    val driver = AndroidSqliteDriver(
        schema = LedgerDb.Schema,
        context = AndroidLedgerContext.appContext,
        name = "vellum_ledger_encrypted_v4.db",
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

private fun getOrCreateDatabaseKey(): ByteArray {
    val keyAlias = "vellum_ledger_db_key_v3"
    val provider = "AndroidKeyStore"
    val ks = KeyStore.getInstance(provider).apply { load(null) }

    if (!ks.containsAlias(keyAlias)) {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, provider)
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_SIGN,
        ).build()
        keyGenerator.init(spec)
        keyGenerator.generateKey()
    }

    val secretKey = ks.getKey(keyAlias, null) as SecretKey
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(secretKey)
    return mac.doFinal("vellum_ledger_db_passphrase_salt".toByteArray(Charsets.UTF_8))
}

