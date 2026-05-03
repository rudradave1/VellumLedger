package com.vellum.ledger.database

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.SqlDriver
import com.vellum.ledger.data.currentTimeMillis
import com.vellum.ledger.db.LedgerDb
import com.vellum.ledger.domain.CardType
import com.vellum.ledger.domain.LedgerCard
import com.vellum.ledger.domain.LedgerSettings
import com.vellum.ledger.domain.LedgerSnapshot
import com.vellum.ledger.domain.LedgerTransaction
import com.vellum.ledger.domain.QueueStatus
import com.vellum.ledger.domain.SyncQueueItem
import com.vellum.ledger.domain.SyncStatus
import com.vellum.ledger.domain.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

internal class SqlDelightLedgerDatabase(
    driver: SqlDriver,
) : LedgerDatabase {
    private val db = LedgerDb(driver)
    private val queries = db.ledgerQueries

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val transactionsFlow =
        queries
            .selectAllTransactions()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                rows.map { row ->
                    LedgerTransaction(
                        id = row.id,
                        amount = row.amount,
                        type = row.type.toTransactionType(),
                        category = row.category,
                        note = row.note,
                        createdAt = row.created_at,
                        syncStatus = row.sync_status.toSyncStatus(),
                    )
                }
            }

    private val queueFlow =
        queries
            .selectAllQueueItems()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                rows.map { row ->
                    SyncQueueItem(
                        id = row.id,
                        entityId = row.entity_id,
                        operationType = row.operation_type,
                        createdAt = row.created_at,
                        status = row.status.toQueueStatus(),
                    )
                }
            }

    private val cardsFlow =
        queries
            .selectAllCards()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                rows.map { row ->
                    LedgerCard(
                        id = row.id,
                        cardName = row.card_name,
                        cardNumber = row.card_number,
                        cardType = row.card_type.toCardType(),
                        expiry = row.expiry,
                        balance = row.balance,
                        hexColor = row.hex_color,
                    )
                }
            }

    private val settingsFlow =
        queries
            .selectAllSettings()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                val map = rows.associate { it.key to it.value_ }
                LedgerSettings(
                    autoSync = map["auto_sync"]?.toBooleanStrictOrNull() ?: true,
                    isDarkMode = map["dark_mode"]?.toBooleanStrictOrNull() ?: false,
                    lastSyncAtMillis = map["last_sync_at_millis"]?.toLongOrNull(),
                    currency = map["currency"] ?: "USD ($)",
                    dailyBudget = map["daily_budget"]?.toDoubleOrNull() ?: 0.0,
                    monthlySummary = map["monthly_summary"],
                    summaryMonth = map["summary_month"],
                )
            }

    override val state: StateFlow<LedgerSnapshot> =
        combine(transactionsFlow, cardsFlow, queueFlow, settingsFlow) { transactions, cards, queue, settings ->
            LedgerSnapshot(
                transactions = transactions,
                cards = cards,
                queueItems = queue,
                settings = settings,
            )
        }.stateIn(scope, SharingStarted.Eagerly, LedgerSnapshot())

    override suspend fun insertTransactionWithQueue(transaction: LedgerTransaction, queueItem: SyncQueueItem) {
        withContext(Dispatchers.Default) {
            queries.transaction {
                queries.insertTransaction(
                    id = transaction.id,
                    amount = transaction.amount,
                    type = transaction.type.name.uppercase(),
                    category = transaction.category,
                    note = transaction.note,
                    created_at = transaction.createdAt,
                    sync_status = transaction.syncStatus.name.uppercase(),
                )
                queries.insertQueueItem(
                    id = queueItem.id,
                    entity_id = queueItem.entityId,
                    operation_type = queueItem.operationType,
                    created_at = queueItem.createdAt,
                    status = queueItem.status.name.uppercase(),
                )
            }
        }
    }

    override suspend fun pendingQueueItems(): List<SyncQueueItem> =
        withContext(Dispatchers.Default) {
            queries
                .selectPendingQueueItems()
                .executeAsList()
                .map { row ->
                    SyncQueueItem(
                        id = row.id,
                        entityId = row.entity_id,
                        operationType = row.operation_type,
                        createdAt = row.created_at,
                        status = row.status.toQueueStatus(),
                    )
                }
        }

    override suspend fun markSynced(transactionId: String, queueItemId: String) {
        withContext(Dispatchers.Default) {
            queries.transaction {
                queries.updateTransactionSyncStatus(
                    sync_status = SyncStatus.Synced.name.uppercase(),
                    id = transactionId,
                )
                queries.updateQueueStatus(
                    status = QueueStatus.Done.name.uppercase(),
                    id = queueItemId,
                )
            }
        }
    }

    override suspend fun markSyncing(transactionId: String) {
        withContext(Dispatchers.Default) {
            queries.updateTransactionSyncStatus(
                sync_status = SyncStatus.Syncing.name.uppercase(),
                id = transactionId,
            )
        }
    }

    override suspend fun markFailed(transactionId: String) {
        withContext(Dispatchers.Default) {
            queries.updateTransactionSyncStatus(
                sync_status = SyncStatus.Failed.name.uppercase(),
                id = transactionId,
            )
        }
    }

    override suspend fun markPending(transactionId: String) {
        withContext(Dispatchers.Default) {
            queries.updateTransactionSyncStatus(
                sync_status = SyncStatus.Pending.name.uppercase(),
                id = transactionId,
            )
        }
    }

    override suspend fun updateSettings(transform: (LedgerSettings) -> LedgerSettings) {
        val current = state.value.settings
        val next = transform(current)
        withContext(Dispatchers.Default) {
            queries.transaction {
                queries.upsertSetting("auto_sync", next.autoSync.toString())
                queries.upsertSetting("dark_mode", next.isDarkMode.toString())
                queries.upsertSetting("last_sync_at_millis", next.lastSyncAtMillis?.toString() ?: "")
                queries.upsertSetting("currency", next.currency)
                queries.upsertSetting("daily_budget", next.dailyBudget.toString())
                queries.upsertSetting("monthly_summary", next.monthlySummary ?: "")
                queries.upsertSetting("summary_month", next.summaryMonth ?: "")
            }
        }
    }

    override suspend fun insertCard(card: LedgerCard) {
        withContext(Dispatchers.Default) {
            queries.insertCard(
                id = card.id,
                card_name = card.cardName,
                card_number = card.cardNumber,
                card_type = card.cardType.name,
                expiry = card.expiry,
                balance = card.balance,
                hex_color = card.hexColor,
            )
        }
    }

    override suspend fun deleteCard(cardId: String) {
        withContext(Dispatchers.Default) {
            queries.deleteCard(cardId)
        }
    }

    override suspend fun clearAll() {
        withContext(Dispatchers.Default) {
            queries.transaction {
                println("SqlDelightLedgerDatabase: Starting Hard Clear...")
                queries.clearTransactions()
                queries.clearCards()
                queries.clearQueue()
                queries.clearSettings()
                // Force a reactive update by upserting a clear timestamp
                queries.upsertSetting("last_hard_clear", currentTimeMillis().toString())
                println("SqlDelightLedgerDatabase: Hard Clear Successful.")
            }
        }
    }

    override suspend fun convertCurrency(from: String, to: String) {
        withContext(Dispatchers.Default) {
            queries.transaction {
                val transactions = queries.selectAllTransactions().executeAsList()
                transactions.forEach { t ->
                    val newAmount = com.vellum.ledger.ui.util.ExchangeRateUtil.convert(t.amount, from, to)
                    queries.insertTransaction(
                        id = t.id,
                        amount = newAmount,
                        type = t.type,
                        category = t.category,
                        note = t.note,
                        created_at = t.created_at,
                        sync_status = t.sync_status
                    )
                }
                
                val cards = queries.selectAllCards().executeAsList()
                cards.forEach { c ->
                    val newBalance = com.vellum.ledger.ui.util.ExchangeRateUtil.convert(c.balance, from, to)
                    queries.insertCard(
                        id = c.id,
                        card_name = c.card_name,
                        card_number = c.card_number,
                        card_type = c.card_type,
                        expiry = c.expiry,
                        balance = newBalance,
                        hex_color = c.hex_color
                    )
                }
            }
        }
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

private fun String.toCardType(): CardType = when (uppercase()) {
    "VISA" -> CardType.Visa
    "MASTERCARD" -> CardType.MasterCard
    else -> CardType.Amex
}

