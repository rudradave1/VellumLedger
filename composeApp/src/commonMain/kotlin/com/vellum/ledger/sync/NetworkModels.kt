package com.vellum.ledger.sync

import com.vellum.ledger.data.currentTimeMillis
import com.vellum.ledger.domain.LedgerTransaction
import com.vellum.ledger.domain.SyncStatus
import com.vellum.ledger.domain.TransactionType
import kotlinx.serialization.Serializable

@Serializable
data class NetworkTransaction(
    val id: String,
    val userId: String,
    val amount: Double,
    val category: String,
    val note: String?,
    val type: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: String
)

@Serializable
data class PushRequest(
    val transactions: List<NetworkTransaction>
)

@Serializable
data class TransactionSummaryDto(
    val id: String,
    val amount: Double,
    val type: String,
    val category: String,
    val note: String,
    val createdAt: Long
)

@Serializable
data class SummaryRequest(
    val transactions: List<TransactionSummaryDto>
)

@Serializable
data class PushResponse(
    val success: Boolean = true,
    val message: String? = null
)

fun LedgerTransaction.toNetwork(userId: String): NetworkTransaction {
    return NetworkTransaction(
        id = id,
        userId = userId,
        amount = amount,
        category = category,
        note = note,
        type = when (type) {
            TransactionType.Income -> "INCOME"
            TransactionType.Expense -> "EXPENSE"
        },
        createdAt = createdAt,
        updatedAt = currentTimeMillis(),
        syncStatus = when (syncStatus) {
            SyncStatus.Pending -> "PENDING"
            SyncStatus.Syncing -> "PENDING"
            SyncStatus.Synced -> "SYNCED"
            SyncStatus.Failed -> "FAILED"
        }
    )
}
