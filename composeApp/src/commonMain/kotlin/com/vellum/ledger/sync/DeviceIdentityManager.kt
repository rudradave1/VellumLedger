package com.vellum.ledger.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vellum.ledger.data.newLedgerId
import kotlinx.coroutines.flow.first

data class SavedSession(
    val token: String,
    val userId: String,
)

class DeviceIdentityManager(private val dataStore: DataStore<Preferences>) {

    private val deviceIdKey = stringPreferencesKey("device_id")
    private val deviceTokenKey = stringPreferencesKey("device_token")
    private val deviceUserIdKey = stringPreferencesKey("device_user_id")

    suspend fun getOrCreateDeviceId(): String {
        val prefs = dataStore.data.first()
        return prefs[deviceIdKey] ?: run {
            val newId = newLedgerId()
            dataStore.edit { it[deviceIdKey] = newId }
            newId
        }
    }

    suspend fun saveSession(token: String, userId: String) {
        dataStore.edit {
            it[deviceTokenKey] = token
            it[deviceUserIdKey] = userId
        }
    }

    suspend fun getSavedSession(): SavedSession? {
        val prefs = dataStore.data.first()
        val token = prefs[deviceTokenKey] ?: return null
        val userId = prefs[deviceUserIdKey] ?: return null
        return SavedSession(token = token, userId = userId)
    }
}
