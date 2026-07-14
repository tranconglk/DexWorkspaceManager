package com.trancong.dexworkspacemanager.feature.layouteditor

data class WorkspaceLaunchProgress(
    val totalApps: Int,
    val completedApps: Int,
    val currentZoneId: String? = null,
    val currentAppLabel: String? = null,
    val isRunning: Boolean = false
) {
    val progressFraction: Float
        get() = if (totalApps <= 0) {
            0f
        } else {
            (completedApps.toFloat() / totalApps).coerceIn(0f, 1f)
        }
}
