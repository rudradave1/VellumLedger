package com.vellum.ledger.ui.util

import com.vellum.ledger.data.currentTimeMillis
import com.vellum.ledger.domain.LedgerSnapshot
import com.vellum.ledger.domain.TransactionType
import com.vellum.ledger.ui.model.CsvExportRequest
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun buildCsvExportRequest(
    snapshot: LedgerSnapshot,
    nowMillis: Long = currentTimeMillis(),
): CsvExportRequest {
    val fileName = "vellum_ledger_${formatExportDate(nowMillis)}.csv"
    val rows = buildList {
        add("date,description,category,amount,currency,original_currency,sync_status")
        snapshot.transactions.forEach { transaction ->
            val date = formatExportDate(transaction.createdAt)
            val description = if (transaction.note.isNotBlank()) transaction.note else transaction.category
            val convertedAmount = ExchangeRateUtil.convert(
                transaction.originalAmount,
                transaction.originalCurrency,
                snapshot.settings.currency
            )
            val signedAmount = if (transaction.type == TransactionType.Expense) -convertedAmount else convertedAmount
            add(
                listOf(
                    csvField(date),
                    csvField(description),
                    csvField(transaction.category),
                    csvField((signedAmount / 100.0).toString()),
                    csvField(snapshot.settings.currency),
                    csvField(transaction.originalCurrency),
                    csvField(transaction.syncStatus.name.uppercase()),
                ).joinToString(",")
            )
        }
    }

    return CsvExportRequest(
        fileName = fileName,
        csvContent = rows.joinToString("\n", postfix = "\n")
    )
}

private fun formatExportDate(millis: Long): String {
    val instant = Instant.fromEpochMilliseconds(millis)
    val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.year}-${dt.monthNumber.toString().padStart(2, '0')}-${dt.dayOfMonth.toString().padStart(2, '0')}"
}

private fun csvField(value: String): String {
    return "\"${value.replace("\"", "\"\"")}\""
}
