package com.vellum.ledger.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vellum.ledger.domain.*
import com.vellum.ledger.repository.*
import com.vellum.ledger.domain.usecase.*
import com.vellum.ledger.sync.UserSession
import com.vellum.ledger.ui.mapper.UiMapper
import com.vellum.ledger.ui.model.*
import com.vellum.ledger.ui.util.GlobalErrorHandler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LedgerViewModel(
    private val transactionRepository: TransactionRepository,
    private val cardRepository: CardRepository,
    private val settingsRepository: SettingsRepository,
    private val getAnalyticsUseCase: GetAnalyticsUseCase,
    private val syncTransactionsUseCase: SyncTransactionsUseCase,
    private val exportTransactionsUseCase: ExportTransactionsUseCase,
    private val uiMapper: UiMapper,
    private val userSession: UserSession
) : ViewModel() {

    val errorEvents: SharedFlow<String> = GlobalErrorHandler.errorEvents
    private val _saveEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val saveEvents: SharedFlow<String> = _saveEvents

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isSummaryLoading = MutableStateFlow(false)
    val isSummaryLoading: StateFlow<Boolean> = _isSummaryLoading.asStateFlow()

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    init {
        viewModelScope.launch {
            userSession.initialize()
            refreshSummary(force = false)
        }
    }

    val transactions: StateFlow<List<TransactionUiModel>> =
        combine(transactionRepository.transactions, settingsRepository.settings) { txs, settings ->
            txs.map { uiMapper.mapToTransactionUi(it, settings.currency) }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val cards: StateFlow<List<CardUiModel>> =
        combine(cardRepository.cards, settingsRepository.settings) { cards, settings ->
            cards.map { uiMapper.mapToCardUi(it, settings.currency) }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val settings: StateFlow<SettingsUiModel> =
        combine(settingsRepository.settings, isSummaryLoading) { settings, loading ->
            uiMapper.mapToSettingsUi(settings, loading)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, uiMapper.mapToSettingsUi(LedgerSettings(), false))

    val analytics: StateFlow<AnalyticsUiModel> =
        getAnalyticsUseCase()
            .stateIn(viewModelScope, SharingStarted.Eagerly, uiMapper.mapToAnalyticsUi(LedgerSnapshot()))

    val isDarkMode: StateFlow<Boolean?> =
        settings.map { it.isDarkMode }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val autoSync: StateFlow<Boolean> =
        settings.map { it.autoSync }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val currency: StateFlow<String> =
        settings.map { it.currency }.stateIn(viewModelScope, SharingStarted.Eagerly, "USD ($)")

    val dailyBudget: StateFlow<Long> =
        settings.map { it.dailyBudget }.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val lastSyncedMessage: StateFlow<String> =
        settings.map { it.lastSyncMessage }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    fun addTransaction(amountCents: Long, type: TransactionType, category: String, note: String, timestamp: Long) {
        viewModelScope.launch {
            try {
                if (amountCents <= 0) return@launch
                transactionRepository.addTransaction(amountCents, type, category, note, timestamp, currency.value)
                _saveEvents.tryEmit("Transaction saved locally")
                if (autoSync.value) performSync()
            } catch (e: Exception) {
                GlobalErrorHandler.handleError(e)
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch { performSync() }
    }

    fun restoreFromBackup() {
        viewModelScope.launch {
            if (_isRestoring.value) return@launch
            try {
                _isRestoring.value = true
                val result = transactionRepository.restoreFromBackup()
                _saveEvents.tryEmit("Restored ${result.restored} transactions from backup")
            } catch (e: Exception) {
                GlobalErrorHandler.handleError(e)
            } finally {
                _isRestoring.value = false
            }
        }
    }

    private suspend fun performSync() {
        if (_isSyncing.value) return
        try {
            _isSyncing.value = true
            syncTransactionsUseCase()
        } catch (e: Exception) {
            GlobalErrorHandler.handleError(e)
        } finally {
            _isSyncing.value = false
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDarkMode(enabled) }
    }

    fun toggleBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setBiometricEnabled(enabled) }
    }

    fun toggleAutoSync(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoSync(enabled) }
    }

    fun setCurrency(currency: String) {
        viewModelScope.launch { settingsRepository.setCurrency(currency) }
    }

    fun setDailyBudget(amountCents: Long) {
        viewModelScope.launch { settingsRepository.setDailyBudget(amountCents) }
    }

    fun retryTransaction(transactionId: String) {
        viewModelScope.launch {
            transactionRepository.retryTransaction(transactionId)
            performSync()
        }
    }

    fun addCard(name: String, number: String, type: CardType, expiry: String, balanceCents: Long, hexColor: String) {
        viewModelScope.launch {
            cardRepository.addCard(name, number, type, expiry, balanceCents, hexColor)
        }
    }

    fun deleteCard(cardId: String) {
        viewModelScope.launch { cardRepository.deleteCard(cardId) }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch { transactionRepository.deleteTransaction(transactionId) }
    }

    fun clearAll() {
        viewModelScope.launch { transactionRepository.clearAll() }
    }

    fun populateDemoData() {
        viewModelScope.launch { transactionRepository.populateDemoData() }
    }

    fun exportCSV(): CsvExportRequest {
        return exportTransactionsUseCase()
    }

    fun refreshSummary(force: Boolean = false) {
        if (_isSummaryLoading.value) return
        viewModelScope.launch {
            try {
                _isSummaryLoading.value = true
                settingsRepository.refreshMonthlySummary(force)
            } finally {
                _isSummaryLoading.value = false
            }
        }
    }
}
