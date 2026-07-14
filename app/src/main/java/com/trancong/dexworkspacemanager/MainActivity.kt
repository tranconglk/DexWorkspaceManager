package com.trancong.dexworkspacemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.trancong.dexworkspacemanager.feature.home.HomeScreen
import com.trancong.dexworkspacemanager.ui.theme.DexWorkspaceManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DexWorkspaceManagerTheme {
                HomeScreen(
                    onCreateLayoutClick = {},
                    onSavedLayoutsClick = {}
                )
            }
        }
    }
}
