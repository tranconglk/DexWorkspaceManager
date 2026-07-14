package com.trancong.dexworkspacemanager.platform.applauncher

import android.app.Activity

interface ForegroundAppLauncher {
    fun launchFromActivity(
        activity: Activity,
        packageName: String,
        activityName: String
    ): AppLaunchResult
}
