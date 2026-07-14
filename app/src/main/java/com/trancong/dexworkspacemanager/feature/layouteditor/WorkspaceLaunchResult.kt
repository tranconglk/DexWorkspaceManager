package com.trancong.dexworkspacemanager.feature.layouteditor

sealed interface WorkspaceLaunchResult {
    data class Success(val launchedCount: Int) : WorkspaceLaunchResult

    data class PartialSuccess(
        val launchedCount: Int,
        val failedCount: Int,
        val failures: List<WorkspaceLaunchFailure>
    ) : WorkspaceLaunchResult

    data object NoAssignedApps : WorkspaceLaunchResult
    data object NotRunningOnDex : WorkspaceLaunchResult
    data class Cancelled(val launchedCount: Int) : WorkspaceLaunchResult
}

data class WorkspaceLaunchFailure(
    val zoneId: String,
    val appLabel: String,
    val reason: String
)
