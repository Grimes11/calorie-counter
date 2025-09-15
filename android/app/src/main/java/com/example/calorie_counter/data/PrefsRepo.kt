package com.example.calorie_counter.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DS_NAME = "settings_prefs"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = DS_NAME)

/** Simple app preferences (offline, no photos; only booleans we need for MVP). */
class PrefsRepo(private val context: Context) {

    companion object {
        private val KEY_SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
        private val KEY_SHARE_CORRECTIONS = booleanPreferencesKey("share_corrections")
    }

    /** Flows expose current values; collectAsState() in Compose. Defaults are false. */
    val syncEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_SYNC_ENABLED] ?: false }

    val shareCorrections: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_SHARE_CORRECTIONS] ?: false }

    suspend fun setSyncEnabled(value: Boolean) {
        context.dataStore.edit { it[KEY_SYNC_ENABLED] = value }
    }

    suspend fun setShareCorrections(value: Boolean) {
        context.dataStore.edit { it[KEY_SHARE_CORRECTIONS] = value }
    }
}
