package com.vellum.ledger.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vellum.ledger.domain.CardType
import com.vellum.ledger.domain.LedgerSnapshot
import com.vellum.ledger.domain.TransactionType
import com.vellum.ledger.data.currentTimeMillis
import com.vellum.ledger.repository.LedgerRepository
import com.vellum.ledger.ui.mapper.UiMapper
import com.vellum.ledger.ui.model.AnalyticsUiModel
import com.vellum.ledger.ui.model.CardUiModel
import com.vellum.ledger.ui.model.SettingsUiModel
import com.vellum.ledger.ui.model.TransactionUiModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.vellum.ledger.ui.util.GlobalErrorHandler
import kotlinx.coroutines.flow.SharedFlow

class LedgerViewModel(
    private val repository: LedgerRepository,
    private val uiMapper: UiMapper
) : ViewModel() {

    val ledger: StateFlow<LedgerSnapshot> = repository.ledger
    val errorEvents: SharedFlow<String> = GlobalErrorHandler.errorEvents

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isSummaryLoading = MutableStateFlow(false)
    val isSummaryLoading: StateFlow<Boolean> = _isSummaryLoading.asStateFlow()

    val transactions: StateFlow<List<TransactionUiModel>> =
        ledger
            .map { snapshot ->
                snapshot.transactions.map { uiMapper.mapToTransactionUi(it, snapshot.settings.currency) }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val cards: StateFlow<List<CardUiModel>> =
        ledger
            .map { snapshot ->
                snapshot.cards.map { uiMapper.mapToCardUi(it, snapshot.settings.currency) }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val settings: StateFlow<SettingsUiModel> =
        combine(ledger, isSummaryLoading) { snapshot, loading ->
            uiMapper.mapToSettingsUi(snapshot.settings, loading)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, uiMapper.mapToSettingsUi(ledger.value.settings, false))

    val analytics: StateFlow<AnalyticsUiModel> =
        ledger
            .map { snapshot -> uiMapper.mapToAnalyticsUi(snapshot) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, uiMapper.mapToAnalyticsUi(ledger.value))

    val isDarkMode: StateFlow<Boolean?> =
        settings
            .map { it.isDarkMode }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val autoSync: StateFlow<Boolean> =
        settings
            .map { it.autoSync }
            .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val currency: StateFlow<String> =
        settings
            .map { it.currency }
            .stateIn(viewModelScope, SharingStarted.Eagerly, "USD ($)")

    val dailyBudget: StateFlow<Long> =
        settings
            .map { it.dailyBudget }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val lastSyncedMessage: StateFlow<String> =
        settings
            .map { it.lastSyncMessage }
            .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    fun addTransaction(amountCents: Long, type: TransactionType, category: String, note: String, timestamp: Long) {
        viewModelScope.launch {
            try {
                if (amountCents <= 0) return@launch
                repository.addTransaction(amountCents, type, category, note, timestamp)
                if (autoSync.value) {
                    syncNow()
                }
            } catch (e: Exception) {
                GlobalErrorHandler.handleError(e)
            }
        }
    }

    fun syncNow() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            try {
                _isSyncing.value = true
                // Simulate a "real" sync delay for better UX
                kotlinx.coroutines.delay(1500)
                val result = repository.syncNow()
                _isSyncing.value = false
            } catch (e: Exception) {
                _isSyncing.value = false
                GlobalErrorHandler.handleError(e)
            }
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch { repository.setDarkMode(enabled) }
    }

    fun toggleBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setBiometricEnabled(enabled) }
    }

    fun toggleAutoSync(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoSync(enabled) }
    }

    fun setCurrency(currency: String) {
        viewModelScope.launch { repository.setCurrency(currency) }
    }

    fun setDailyBudget(amountCents: Long) {
        viewModelScope.launch { repository.setDailyBudget(amountCents) }
    }

    fun retryTransaction(transactionId: String) {
        viewModelScope.launch {
            repository.retryTransaction(transactionId)
            syncNow()
        }
    }

    fun addCard(name: String, number: String, type: CardType, expiry: String, balanceCents: Long, hexColor: String) {
        viewModelScope.launch {
            repository.addCard(name, number, type, expiry, balanceCents, hexColor)
        }
    }

    fun deleteCard(cardId: String) {
        viewModelScope.launch {
            repository.deleteCard(cardId)
        }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
        }
    }

    fun clearAll() {
        viewModelScope.launch { repository.clearAll() }
    }

    fun populateDemoData() {
        viewModelScope.launch { repository.populateDemoData() }
    }

    fun exportCSV(): String {
        return repository.getCsvData()
    }

    fun refreshSummary(force: Boolean = false) {
        if (_isSummaryLoading.value) return
        viewModelScope.launch {
            try {
                _isSummaryLoading.value = true
                repository.refreshMonthlySummary(force)
            } finally {
                _isSummaryLoading.value = false
            }
        }
    }
}
