package com.vellum.ledger.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vellum.ledger.domain.usecase.SyncTransactionsUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class LedgerSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val userSession: UserSession by inject()
    private val syncTransactionsUseCase: SyncTransactionsUseCase by inject()

    override suspend fun doWork(): Result {
        return try {
            userSession.initialize()
            syncTransactionsUseCase()

            Result.success()
        } catch (e: Throwable) {
            println("LedgerSyncWorker: background sync failed: ${e.message}")
            Result.retry()
        }
    }
}
