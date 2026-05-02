package com.vellum.ledger.data

import android.content.Intent
import com.vellum.ledger.BuildConfig
import com.vellum.ledger.database.AndroidLedgerContext
import java.util.UUID

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun newLedgerId(): String = UUID.randomUUID().toString()

actual val appVersion: String = BuildConfig.VERSION_NAME

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
