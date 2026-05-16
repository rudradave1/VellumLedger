package com.vellum.ledger.domain.usecase

import com.vellum.ledger.domain.*
import com.vellum.ledger.ui.model.AnalyticsUiModel
import com.vellum.ledger.ui.mapper.UiMapper
import kotlinx.coroutines.flow.*

class GetAnalyticsUseCase(
    private val transactionRepository: com.vellum.ledger.repository.TransactionRepository,
    private val settingsRepository: com.vellum.ledger.repository.SettingsRepository,
    private val uiMapper: UiMapper
) {
    private val cache = MutableStateFlow<AnalyticsCache?>(null)

    operator fun invoke(): Flow<AnalyticsUiModel> {
        return combine(
            transactionRepository.transactions,
            settingsRepository.settings
        ) { transactions, settings ->
            val currency = settings.currency
            val currentCache = cache.value

            if (currentCache != null && 
                currentCache.transactions == transactions && 
                currentCache.currency == currency) {
                return@combine currentCache.uiModel
            }

            val uiModel = uiMapper.mapToAnalyticsUi(
                com.vellum.ledger.domain.LedgerSnapshot(
                    transactions = transactions,
                    settings = settings
                )
            )
            cache.value = AnalyticsCache(transactions, currency, uiModel)
            uiModel
        }
    }

    private data class AnalyticsCache(
        val transactions: List<LedgerTransaction>,
        val currency: String,
        val uiModel: AnalyticsUiModel
    )
}
