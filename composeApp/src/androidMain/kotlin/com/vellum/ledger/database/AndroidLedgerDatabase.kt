package com.vellum.ledger.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.vellum.ledger.domain.LedgerSnapshot
import com.vellum.ledger.domain.LedgerTransaction
import com.vellum.ledger.domain.QueueStatus
import com.vellum.ledger.domain.SyncQueueItem
import com.vellum.ledger.domain.SyncStatus
import com.vellum.ledger.domain.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual fun createLedgerDatabase(): LedgerDatabase = AndroidLedgerDatabase(AndroidLedgerContext.appContext)

private class AndroidLedgerDatabase(
    context: Context,
) : SQLiteOpenHelper(context, "vellum_ledger.db", null, 1), LedgerDatabase {
    private val mutableState = MutableStateFlow(readSnapshot())

    override val state: StateFlow<LedgerSnapshot> = mutableState.asStateFlow()

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS transactions (
                id TEXT PRIMARY KEY,
                amount REAL,
                type TEXT,
                category TEXT,
                note TEXT,
                created_at INTEGER,
                sync_status TEXT
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS sync_queue (
                id TEXT PRIMARY KEY,
                entity_id TEXT,
                operation_type TEXT,
                created_at INTEGER,
                status TEXT
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    override suspend fun insertTransactionWithQueue(
        transaction: LedgerTransaction,
        queueItem: SyncQueueItem,
    ) = synchronized(this) {
        writableDatabase.beginTransaction()
        try {
            writableDatabase.insertOrThrow("transactions", null, transaction.toValues())
            writableDatabase.insertOrThrow("sync_queue", null, queueItem.toValues())
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
        refresh()
    }

    override suspend fun pendingQueueItems(): List<SyncQueueItem> = synchronized(this) {
        readQueueItems(status = QueueStatus.Pending)
    }

    override suspend fun markSynced(transactionId: String, queueItemId: String) = synchronized(this) {
        writableDatabase.beginTransaction()
        try {
            val transactionValues = ContentValues().apply {
                put("sync_status", SyncStatus.Synced.name.uppercase())
            }
            writableDatabase.update(
                "transactions",
                transactionValues,
                "id = ?",
                arrayOf(transactionId),
            )

            val queueValues = ContentValues().apply {
                put("status", QueueStatus.Done.name.uppercase())
            }
            writableDatabase.update(
                "sync_queue",
                queueValues,
                "id = ?",
                arrayOf(queueItemId),
            )
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
        refresh()
    }

    override suspend fun markSyncing(transactionId: String) = synchronized(this) {
        val transactionValues = ContentValues().apply {
            put("sync_status", SyncStatus.Syncing.name.uppercase())
        }
        writableDatabase.update(
            "transactions",
            transactionValues,
            "id = ?",
            arrayOf(transactionId),
        )
        refresh()
    }

    override suspend fun markFailed(transactionId: String) = synchronized(this) {
        val transactionValues = ContentValues().apply {
            put("sync_status", SyncStatus.Failed.name.uppercase())
        }
        writableDatabase.update(
            "transactions",
            transactionValues,
            "id = ?",
            arrayOf(transactionId),
        )
        refresh()
    }

    private fun refresh() {
        mutableState.value = readSnapshot()
    }

    private fun readSnapshot(): LedgerSnapshot = LedgerSnapshot(
        transactions = readTransactions(),
        queueItems = readQueueItems(status = null),
    )

    private fun readTransactions(): List<LedgerTransaction> {
        val items = mutableListOf<LedgerTransaction>()
        readableDatabase.query(
            "transactions",
            arrayOf("id", "amount", "type", "category", "note", "created_at", "sync_status"),
            null,
            null,
            null,
            null,
            "created_at DESC",
        ).use { cursor ->
            while (cursor.moveToNext()) {
                items += LedgerTransaction(
                    id = cursor.getString(0),
                    amount = cursor.getDouble(1),
                    type = cursor.getString(2).toTransactionType(),
                    category = cursor.getString(3),
                    note = cursor.getString(4),
                    createdAt = cursor.getLong(5),
                    syncStatus = cursor.getString(6).toSyncStatus(),
                )
            }
        }
        return items
    }

    private fun readQueueItems(status: QueueStatus?): List<SyncQueueItem> {
        val items = mutableListOf<SyncQueueItem>()
        readableDatabase.query(
            "sync_queue",
            arrayOf("id", "entity_id", "operation_type", "created_at", "status"),
            status?.let { "status = ?" },
            status?.let { arrayOf(it.name.uppercase()) },
            null,
            null,
            "created_at ASC",
        ).use { cursor ->
            while (cursor.moveToNext()) {
                items += SyncQueueItem(
                    id = cursor.getString(0),
                    entityId = cursor.getString(1),
                    operationType = cursor.getString(2),
                    createdAt = cursor.getLong(3),
                    status = cursor.getString(4).toQueueStatus(),
                )
            }
        }
        return items
    }

    private fun LedgerTransaction.toValues() = ContentValues().apply {
        put("id", id)
        put("amount", amount)
        put("type", type.name.uppercase())
        put("category", category)
        put("note", note)
        put("created_at", createdAt)
        put("sync_status", syncStatus.name.uppercase())
    }

    private fun SyncQueueItem.toValues() = ContentValues().apply {
        put("id", id)
        put("entity_id", entityId)
        put("operation_type", operationType)
        put("created_at", createdAt)
        put("status", status.name.uppercase())
    }
}

private fun String.toTransactionType(): TransactionType = when (uppercase()) {
    "INCOME" -> TransactionType.Income
    else -> TransactionType.Expense
}

private fun String.toSyncStatus(): SyncStatus = when (uppercase()) {
    "SYNCED" -> SyncStatus.Synced
    "SYNCING" -> SyncStatus.Syncing
    "FAILED" -> SyncStatus.Failed
    else -> SyncStatus.Pending
}

private fun String.toQueueStatus(): QueueStatus = when (uppercase()) {
    "DONE" -> QueueStatus.Done
    else -> QueueStatus.Pending
}
