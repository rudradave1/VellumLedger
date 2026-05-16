package com.vellum.ledger.data

import com.vellum.ledger.sync.SecureStorage

expect fun currentTimeMillis(): Long

expect fun newLedgerId(): String

expect val appVersion: String

expect val isDebugBuild: Boolean

expect val isNetworkAvailable: Boolean

expect fun shareText(text: String, title: String)

expect fun exportCsvFile(csvContent: String, fileName: String)

expect fun createDataStore(): androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>

expect fun createSecureStorage(): SecureStorage

expect fun getSecureRandomBytes(size: Int): ByteArray
