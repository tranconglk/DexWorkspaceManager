package com.trancong.dexworkspacemanager.platform.applauncher

interface AppLauncher {
    fun launch(
        packageName: String,
        activityName: String
    ): AppLaunchResult
}
