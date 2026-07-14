package com.trancong.dexworkspacemanager.platform.applauncher

import android.app.Activity

interface ForegroundAppLauncher {
    fun launchInZone(
        activity: Activity,
        packageName: String,
        activityName: String,
        bounds: LaunchBounds
    ): AppLaunchResult
}
