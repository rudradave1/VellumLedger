package com.vellum.ledger.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.vellum.ledger.database.AndroidLedgerContext
import java.util.concurrent.TimeUnit

private const val IMMEDIATE_WORK_NAME = "ledger-sync-immediate"
private const val PERIODIC_WORK_NAME = "ledger-sync-periodic"

actual fun scheduleLedgerSync() {
    val context = AndroidLedgerContext.appContext
    if (context is Context) {
        val workManager = WorkManager.getInstance(context)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val immediateRequest = OneTimeWorkRequestBuilder<LedgerSyncWorker>()
            .setConstraints(constraints)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<LedgerSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            immediateRequest,
        )

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest,
        )
    }
}
