package com.trancong.dexworkspacemanager

import android.os.Bundle
import android.widget.Toast
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
                    onCreateLayoutClick = {
                        Toast.makeText(this, "Tạo bố cục mới", Toast.LENGTH_SHORT).show()
                    },
                    onSavedLayoutsClick = {
                        Toast.makeText(this, "Bố cục đã lưu", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}
