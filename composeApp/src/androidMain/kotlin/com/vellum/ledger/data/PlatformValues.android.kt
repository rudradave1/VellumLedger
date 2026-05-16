package com.vellum.ledger.data

import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.vellum.ledger.BuildConfig
import com.vellum.ledger.database.AndroidLedgerContext
import androidx.core.content.FileProvider
import java.util.UUID
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import java.nio.charset.Charset
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.vellum.ledger.sync.SecureStorage
import android.content.Context
import java.security.SecureRandom

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun newLedgerId(): String = UUID.randomUUID().toString()

actual val appVersion: String = BuildConfig.VERSION_NAME

actual val isDebugBuild: Boolean = BuildConfig.DEBUG

actual fun shareText(text: String, title: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TITLE, title)
        putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val chooser = Intent.createChooser(intent, title).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    AndroidLedgerContext.appContext.startActivity(chooser)
}

actual fun exportCsvFile(csvContent: String, fileName: String) {
    val context = AndroidLedgerContext.appContext
    val bytes = csvContent.toByteArray(Charsets.UTF_8)

    try {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val createdUri = context.contentResolver.insert(collection, values)
                ?: throw IllegalStateException("Unable to create CSV in Downloads")
            context.contentResolver.openOutputStream(createdUri)?.use { output ->
                output.write(bytes)
            } ?: throw IllegalStateException("Unable to write CSV file")
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(createdUri, values, null, null)
            createdUri
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            downloadsDir.mkdirs()
            file.writeBytes(bytes)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(shareIntent, "Export CSV").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        println("exportCsvFile failed: ${e.message}")
        throw e
    }
}

actual fun createDataStore(): DataStore<Preferences> = PreferenceDataStoreFactory.create(
    produceFile = {
        File(AndroidLedgerContext.appContext.filesDir, "ledger.preferences_pb")
    }
)

actual fun createSecureStorage(): SecureStorage = AndroidSecureStorage(AndroidLedgerContext.appContext)

private class AndroidSecureStorage(context: Context) : SecureStorage {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_ledger_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun get(key: String): String? = sharedPrefs.getString(key, null)
    override fun set(key: String, value: String?) {
        sharedPrefs.edit().putString(key, value).apply()
    }
}

actual fun getSecureRandomBytes(size: Int): ByteArray {
    val bytes = ByteArray(size)
    SecureRandom().nextBytes(bytes)
    return bytes
}
