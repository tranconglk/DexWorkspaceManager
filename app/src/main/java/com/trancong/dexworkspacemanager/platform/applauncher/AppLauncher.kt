package com.trancong.dexworkspacemanager.platform.applauncher

interface AppLauncher {
    fun launch(
        packageName: String,
        activityName: String
    ): AppLaunchResult

    fun launchOnTargetDisplayForDiagnostics(
        packageName: String,
        activityName: String,
        displayId: Int,
        bounds: LaunchBounds
    ): AppLaunchResult
}
