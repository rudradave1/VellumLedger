package com.vellum.ledger

import android.app.Application
import com.vellum.ledger.di.initKoin
import com.vellum.ledger.database.AndroidLedgerContext
import org.koin.android.ext.koin.androidContext

class LedgerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidLedgerContext.appContext = this
        initKoin {
            androidContext(this@LedgerApplication)
        }
    }
}
