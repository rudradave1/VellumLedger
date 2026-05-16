package com.vellum.ledger.sync

import com.vellum.ledger.data.currentTimeMillis
import com.vellum.ledger.domain.LedgerTransaction
import com.vellum.ledger.domain.SyncStatus
import com.vellum.ledger.domain.TransactionType
import kotlinx.serialization.Serializable
import kotlin.math.roundToLong

@Serializable
data class NetworkTransaction(
    val id: String,
    val userId: String,
    val amount: Long,
    val category: String,
    val note: String?,
    val type: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: String
)

@Serializable
data class BackupTransactionDto(
    val id: String,
    val userId: String,
    val amount: Double,
    val category: String,
    val note: String? = null,
    val type: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class PullResponse(
    val transactions: List<BackupTransactionDto> = emptyList(),
    val serverTime: Long? = null,
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
data class SyncAcknowledgement(
    val id: String,
    val serverVersion: Int
)

@Serializable
data class PushResponse(
    val success: Boolean = true,
    val message: String? = null,
    val acknowledgements: List<SyncAcknowledgement> = emptyList()
)

@Serializable
data class AuthRequest(
    val email: String,
    val password: String,
)

@Serializable
data class AuthResponse(
    val token: String,
    val userId: String
)

fun BackupTransactionDto.toRestoredTransaction(displayCurrency: String): LedgerTransaction {
    val amountCents = amount.roundToLong()
    // Restore approximation: the server does not store originalAmount/originalCurrency,
    // so we infer both from the current display currency when rebuilding local state.
    return LedgerTransaction(
        id = id,
        amount = amountCents,
        originalAmount = amountCents,
        originalCurrency = displayCurrency,
        type = when (type.uppercase()) {
            "INCOME" -> TransactionType.Income
            else -> TransactionType.Expense
        },
        category = category,
        note = note.orEmpty(),
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncStatus = SyncStatus.Synced,
        localVersion = 0,
        serverVersion = 0,
    )
}

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
