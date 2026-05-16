package com.vellum.ledger.data

import platform.Foundation.*
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.vellum.ledger.sync.SecureStorage
import okio.Path.Companion.toPath
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.refTo
import platform.Security.*

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1_000).toLong()

actual fun newLedgerId(): String = NSUUID().UUIDString()

actual val appVersion: String = "1.2.0-ios"

actual val isDebugBuild: Boolean = false

actual val isNetworkAvailable: Boolean = true // Simplified for iOS

actual fun shareText(text: String, title: String) {
    val window = UIApplication.sharedApplication.keyWindow
    val rootViewController = window?.rootViewController
    
    val activityViewController = UIActivityViewController(listOf(text), null)
    rootViewController?.presentViewController(activityViewController, true, null)
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun exportCsvFile(csvContent: String, fileName: String) {
    val fileManager = NSFileManager.defaultManager
    val documentsDirectory = fileManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    val fileUrl = requireNotNull(documentsDirectory).URLByAppendingPathComponent(fileName)
        ?: throw IllegalStateException("Unable to create CSV file URL")

    val bytes = csvContent.encodeToByteArray()
    val success = bytes.usePinned { pinned ->
        val data = NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        data.writeToURL(fileUrl, atomically = true)
    }

    if (!success) {
        throw IllegalStateException("Unable to write CSV file")
    }

    val window = UIApplication.sharedApplication.keyWindow
    val rootViewController = window?.rootViewController
    val activityViewController = UIActivityViewController(listOf(fileUrl), null)
    rootViewController?.presentViewController(activityViewController, true, null)
}

@OptIn(ExperimentalForeignApi::class)
actual fun createDataStore(): DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
    produceFile = {
        val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
        (requireNotNull(documentDirectory).path + "/ledger.preferences_pb").toPath()
    }
)

actual fun createSecureStorage(): SecureStorage = IosSecureStorage()

private class IosSecureStorage : SecureStorage {
    override fun get(key: String): String? = NSUserDefaults.standardUserDefaults.stringForKey(key)
    override fun set(key: String, value: String?) {
        if (value == null) {
            NSUserDefaults.standardUserDefaults.removeObjectForKey(key)
        } else {
            NSUserDefaults.standardUserDefaults.setObject(value, forKey = key)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun getSecureRandomBytes(size: Int): ByteArray {
    return ByteArray(size).apply {
        val status = SecRandomCopyBytes(kSecRandomDefault, size.toULong(), this.refTo(0))
        if (status != 0) throw IllegalStateException("Failed to generate secure random bytes")
    }
}
