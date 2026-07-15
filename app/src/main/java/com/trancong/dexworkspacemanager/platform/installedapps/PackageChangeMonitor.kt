package com.trancong.dexworkspacemanager.platform.installedapps

import kotlinx.coroutines.flow.Flow

interface PackageChangeMonitor {
    val events: Flow<PackageChangeEvent>

    fun start()

    fun stop()
}
