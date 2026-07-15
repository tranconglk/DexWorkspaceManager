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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isRunningOnExternalDisplay()) {
            finish()
            return
        }

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
}
