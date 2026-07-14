package com.trancong.dexworkspacemanager

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.trancong.dexworkspacemanager.navigation.AppNavHost
import com.trancong.dexworkspacemanager.platform.dex.isRunningOnExternalDisplay
import com.trancong.dexworkspacemanager.ui.theme.DexWorkspaceManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        val displayId = windowManager.defaultDisplay.displayId
        Log.d(DEX_ONLY_LOG_TAG, "MainActivity created on displayId=$displayId")
        if (!isRunningOnExternalDisplay()) {
            Log.d(DEX_ONLY_LOG_TAG, "Closing phone-display activity in DeX-only mode")
            Log.d(DEX_ONLY_LOG_TAG, "Rejected default display Activity")
            finish()
            Log.d(DEX_ONLY_LOG_TAG, "Activity finished before setContent")
            return
        }

        Log.d(DEX_ONLY_LOG_TAG, "Accepted external display Activity")
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
            Log.d(DEX_ONLY_LOG_TAG, "Rejected default display Activity in onNewIntent")
            finish()
        }
    }
}

private const val DEX_ONLY_LOG_TAG = "DexOnlyMode"
