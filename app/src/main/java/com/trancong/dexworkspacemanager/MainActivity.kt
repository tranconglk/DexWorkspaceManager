package com.trancong.dexworkspacemanager

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.trancong.dexworkspacemanager.navigation.AppNavHost
import com.trancong.dexworkspacemanager.platform.dex.isRunningOnExternalDisplay
import com.trancong.dexworkspacemanager.ui.theme.DexWorkspaceManagerTheme

class MainActivity : ComponentActivity() {
    private var packageMonitorStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isRunningOnExternalDisplay()) {
            finish()
            return
        }

        applicationContainer.packageChangeMonitor.start()
        packageMonitorStarted = true

        enableEdgeToEdge()
        setContent {
            DexWorkspaceManagerTheme {
                AppNavHost()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (!isRunningOnExternalDisplay()) {
            finish()
        }
    }

    override fun onDestroy() {
        if (packageMonitorStarted) {
            applicationContainer.packageChangeMonitor.stop()
            packageMonitorStarted = false
        }
        super.onDestroy()
    }

    private val applicationContainer: AppContainer
        get() = (application as DexWorkspaceManagerApplication).container
}
