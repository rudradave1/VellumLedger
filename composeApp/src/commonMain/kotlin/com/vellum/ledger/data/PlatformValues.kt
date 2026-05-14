package com.vellum.ledger.data

expect fun currentTimeMillis(): Long

expect fun newLedgerId(): String

expect val appVersion: String

expect fun shareText(text: String, title: String)

expect fun exportCsvFile(csvContent: String, fileName: String)

expect fun createDataStore(): androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
