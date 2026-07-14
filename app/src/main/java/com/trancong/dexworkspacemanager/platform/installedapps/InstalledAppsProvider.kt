package com.trancong.dexworkspacemanager.platform.installedapps

interface InstalledAppsProvider {
    suspend fun getLaunchableApps(): List<InstalledApp>
}
