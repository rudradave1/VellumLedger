package com.vellum.ledger.ui.viewmodel

import com.vellum.ledger.domain.LedgerSnapshot
import com.vellum.ledger.domain.TransactionType
import com.vellum.ledger.data.currentTimeMillis
import com.vellum.ledger.repository.LedgerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LedgerViewModel(private val repository: LedgerRepository) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val ledger: StateFlow<LedgerSnapshot> = repository.ledger

    val isDarkMode: StateFlow<Boolean> =
        ledger
            .map { it.settings.isDarkMode }
            .stateIn(viewModelScope, SharingStarted.Eagerly, ledger.value.settings.isDarkMode)

    val autoSync: StateFlow<Boolean> =
        ledger
            .map { it.settings.autoSync }
            .stateIn(viewModelScope, SharingStarted.Eagerly, ledger.value.settings.autoSync)

    val lastSyncedMessage: StateFlow<String> =
        ledger
            .map { snapshot -> formatLastSync(snapshot.settings.lastSyncAtMillis, nowMillis = currentTimeMillis()) }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                formatLastSync(ledger.value.settings.lastSyncAtMillis, nowMillis = currentTimeMillis()),
            )

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun addTransaction(amount: Double, type: TransactionType, category: String, note: String) {
        viewModelScope.launch {
            if (amount <= 0.0) return@launch
            repository.addTransaction(amount, type, category, note)
            if (autoSync.value) {
                syncNow()
            }
        }
    }

    fun syncNow() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            val result = repository.syncNow()
            _isSyncing.value = false
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch { repository.setDarkMode(enabled) }
    }

    fun toggleAutoSync(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoSync(enabled) }
    }

    fun retryTransaction(transactionId: String) {
        viewModelScope.launch {
            repository.retryTransaction(transactionId)
            syncNow()
        }
    }

    fun clearAll() {
        viewModelScope.launch { repository.clearAll() }
    }
}

private fun formatLastSync(lastSyncAtMillis: Long?, nowMillis: Long): String {
    if (lastSyncAtMillis == null) return "Never"
    val diff = (nowMillis - lastSyncAtMillis).coerceAtLeast(0L)
    return when {
        diff < 60_000L -> "Just now"
        diff < 3_600_000L -> "${diff / 60_000L}m ago"
        diff < 86_400_000L -> "${diff / 3_600_000L}h ago"
        else -> "${diff / 86_400_000L}d ago"
    }
}
