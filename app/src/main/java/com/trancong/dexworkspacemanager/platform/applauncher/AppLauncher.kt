package com.trancong.dexworkspacemanager.platform.applauncher

import com.trancong.dexworkspacemanager.platform.dex.DexLaunchMode

interface AppLauncher {
    fun launch(
        packageName: String,
        activityName: String
    ): AppLaunchResult

    fun launchOnDisplay(
        packageName: String,
        activityName: String,
        displayId: Int
    ): AppLaunchResult

    fun launchOnDisplayWithBounds(
        packageName: String,
        activityName: String,
        displayId: Int,
        bounds: LaunchBounds
    ): AppLaunchResult

    fun launchWithMode(
        packageName: String,
        activityName: String,
        mode: DexLaunchMode,
        displayId: Int? = null
    ): AppLaunchResult
}
