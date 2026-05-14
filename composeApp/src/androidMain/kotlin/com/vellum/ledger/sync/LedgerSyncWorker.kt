package com.vellum.ledger.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vellum.ledger.database.AndroidLedgerContext
import com.vellum.ledger.data.createDataStore
import com.vellum.ledger.repository.LedgerRepository

class LedgerSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            AndroidLedgerContext.appContext = applicationContext
            val dataStore = createDataStore()
            val deviceIdentityManager = DeviceIdentityManager(dataStore)
            val repository = LedgerRepository(deviceIdentityManager = deviceIdentityManager)

            repository.initialize()
            repository.syncNow()

            Result.success()
        } catch (e: Throwable) {
            println("LedgerSyncWorker: background sync failed: ${e.message}")
            Result.retry()
        }
    }
}
