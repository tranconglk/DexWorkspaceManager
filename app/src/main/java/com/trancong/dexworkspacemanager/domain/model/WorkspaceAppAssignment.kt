package com.trancong.dexworkspacemanager.domain.model

data class WorkspaceAppAssignment(
    val zoneId: String,
    val packageName: String,
    val activityName: String,
    val appLabel: String,
    val launchOrder: Int
) {
    init {
        require(zoneId.isNotBlank()) { "zoneId must not be blank." }
        require(packageName.isNotBlank()) { "packageName must not be blank." }
        require(activityName.isNotBlank()) { "activityName must not be blank." }
        require(appLabel.isNotBlank()) { "appLabel must not be blank." }
        require(launchOrder >= 0) { "launchOrder must not be negative." }
    }
}
