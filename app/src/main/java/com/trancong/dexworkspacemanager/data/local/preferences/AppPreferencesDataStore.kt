package com.trancong.dexworkspacemanager.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.appPreferencesDataStore by preferencesDataStore(name = "app_preferences")
