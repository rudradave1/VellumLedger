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
import android.util.Base64
import com.vellum.ledger.data.getSecureRandomBytes
import com.vellum.ledger.data.createSecureStorage
import kotlin.text.Charsets

actual fun createLedgerDatabase(): LedgerDatabase {
    // Required to load SQLCipher native libraries
    System.loadLibrary("sqlcipher")

    val dbName = "vellum_ledger_encrypted_v4.db"
    val context = AndroidLedgerContext.appContext
    val dbFile = context.getDatabasePath(dbName)
    
    // Recovery for empty files which cause SQLCipher to report "not a database"
    if (dbFile.exists() && dbFile.length() == 0L) {
        println("AndroidLedgerDatabase: Empty database file found. Deleting.")
        dbFile.delete()
    }

    val passphrase = getOrCreateDatabaseKey()
    val factory = SupportOpenHelperFactory(passphrase)

    fun createSqlDelightDriver(f: SupportOpenHelperFactory): AndroidSqliteDriver {
        return AndroidSqliteDriver(
            schema = LedgerDb.Schema,
            context = context,
            name = dbName,
            factory = f,
            callback = object : AndroidSqliteDriver.Callback(LedgerDb.Schema) {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.enableWriteAheadLogging()
                }
            },
        )
    }

    if (!dbFile.exists()) {
        return SqlDelightLedgerDatabase(createSqlDelightDriver(factory))
    }

    // Attempt to open with current passphrase
    try {
        val testDb = net.zetetic.database.sqlcipher.SQLiteDatabase.openDatabase(
            dbFile.absolutePath, 
            passphrase, 
            null, 
            net.zetetic.database.sqlcipher.SQLiteDatabase.OPEN_READONLY,
            null,
            null
        )
        testDb.close()
        println("AndroidLedgerDatabase: Current passphrase validated.")
        return SqlDelightLedgerDatabase(createSqlDelightDriver(factory))
    } catch (e: Exception) {
        if (e.message?.contains("file is not a database", ignoreCase = true) == true) {
            println("AndroidLedgerDatabase: Passphrase mismatch. Trying legacy recovery...")
            val legacyPassphrase = getLegacyDatabaseKey()
            try {
                val legacyDb = net.zetetic.database.sqlcipher.SQLiteDatabase.openDatabase(
                    dbFile.absolutePath, 
                    legacyPassphrase, 
                    null, 
                    net.zetetic.database.sqlcipher.SQLiteDatabase.OPEN_READONLY,
                    null,
                    null
                )
                legacyDb.close()
                
                // Success with legacy key! Update SecureStorage for future runs.
                val secureStorage = createSecureStorage()
                secureStorage.set("db_passphrase_salt_v2", "vellum_ledger_db_passphrase_salt")
                println("AndroidLedgerDatabase: Recovered using legacy salt. SecureStorage updated.")
                
                return SqlDelightLedgerDatabase(createSqlDelightDriver(SupportOpenHelperFactory(legacyPassphrase)))
            } catch (recoveryException: Exception) {
                println("AndroidLedgerDatabase: Recovery failed: ${recoveryException.message}")
                throw e
            }
        } else {
            throw e
        }
    }
}

private fun getLegacyDatabaseKey(): ByteArray {
    val keyAlias = "vellum_ledger_db_key_v3"
    val provider = "AndroidKeyStore"
    val ks = KeyStore.getInstance(provider).apply { load(null) }
    val secretKey = ks.getKey(keyAlias, null) as SecretKey
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(secretKey)
    return mac.doFinal("vellum_ledger_db_passphrase_salt".toByteArray(Charsets.UTF_8))
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
    
    val secureStorage = createSecureStorage()
    val saltKey = "db_passphrase_salt_v2"
    var salt = secureStorage.get(saltKey)
    
    if (salt == null) {
        val dbFile = AndroidLedgerContext.appContext.getDatabasePath("vellum_ledger_encrypted_v4.db")
        if (dbFile.exists()) {
            val legacySalt = "vellum_ledger_db_passphrase_salt"
            secureStorage.set(saltKey, legacySalt)
            salt = legacySalt
            println("AndroidLedgerDatabase: Migrated to legacy salt for existing database.")
        } else {
            val newSalt = getSecureRandomBytes(32)
            val saltString = Base64.encodeToString(newSalt, Base64.NO_WRAP)
            secureStorage.set(saltKey, saltString)
            salt = saltString
            println("AndroidLedgerDatabase: Generated new secure salt.")
        }
    }
    
    return mac.doFinal(salt!!.toByteArray(Charsets.UTF_8))
}

