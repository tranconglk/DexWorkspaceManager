package com.trancong.dexworkspacemanager.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import com.trancong.dexworkspacemanager.domain.repository.AppPreferencesRepository
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class AppPreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : AppPreferencesRepository {
    private val preferences = dataStore.data.catch { exception ->
        if (exception is IOException) emit(emptyPreferences()) else throw exception
    }

    override val quickLaunchWorkspaceId: Flow<Long?> = preferences.map {
        it[QUICK_LAUNCH_WORKSPACE_ID]
    }

    override val showDexPinHint: Flow<Boolean> = preferences.map {
        it[SHOW_DEX_PIN_HINT] ?: true
    }

    override suspend fun setQuickLaunchWorkspaceId(workspaceId: Long?) {
        dataStore.edit { preferences ->
            if (workspaceId == null) preferences.remove(QUICK_LAUNCH_WORKSPACE_ID)
            else preferences[QUICK_LAUNCH_WORKSPACE_ID] = workspaceId
        }
    }

    override suspend fun setShowDexPinHint(show: Boolean) {
        dataStore.edit { it[SHOW_DEX_PIN_HINT] = show }
    }

    private companion object {
        val QUICK_LAUNCH_WORKSPACE_ID = longPreferencesKey("quick_launch_workspace_id")
        val SHOW_DEX_PIN_HINT = booleanPreferencesKey("show_dex_pin_hint")
    }
}
