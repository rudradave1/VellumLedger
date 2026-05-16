package com.vellum.ledger.domain.usecase

import com.vellum.ledger.database.LedgerDatabase
import com.vellum.ledger.ui.model.CsvExportRequest
import com.vellum.ledger.ui.util.buildCsvExportRequest

class ExportTransactionsUseCase(
    private val database: LedgerDatabase
) {
    operator fun invoke(): CsvExportRequest {
        return buildCsvExportRequest(database.state.value)
    }
}
