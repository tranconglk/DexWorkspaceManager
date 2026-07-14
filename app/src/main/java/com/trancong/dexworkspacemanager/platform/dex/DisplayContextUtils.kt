package com.trancong.dexworkspacemanager.platform.dex

import android.app.Activity
import android.os.Build
import android.view.Display

@Suppress("DEPRECATION")
fun Activity.isRunningOnExternalDisplay(): Boolean {
    val displayId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        display?.displayId
    } else {
        windowManager.defaultDisplay.displayId
    }
    return displayId != null && displayId != Display.DEFAULT_DISPLAY
}
