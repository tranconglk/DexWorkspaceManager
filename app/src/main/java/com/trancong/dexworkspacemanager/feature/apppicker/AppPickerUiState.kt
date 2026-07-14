package com.trancong.dexworkspacemanager.feature.apppicker

import com.trancong.dexworkspacemanager.platform.installedapps.InstalledApp

data class AppPickerUiState(
    val apps: List<InstalledApp> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val filteredApps: List<InstalledApp>
        get() {
            val query = searchQuery.trim()
            if (query.isEmpty()) return apps
            return apps.filter { app ->
                app.label.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)
            }
        }
}
