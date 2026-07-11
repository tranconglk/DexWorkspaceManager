package com.trancong.dexworkspacemanager.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.trancong.dexworkspacemanager.ui.theme.DexWorkspaceManagerTheme

@Composable
fun HomeScreen(
    onCreateLayoutClick: () -> Unit,
    onSavedLayoutsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "DeX Workspace Manager",
            textAlign = TextAlign.Center
        )
        Text(
            text = "Quản lý và khởi chạy bố cục ứng dụng trên Samsung DeX",
            modifier = Modifier.padding(top = 12.dp, bottom = 32.dp),
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onCreateLayoutClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Tạo bố cục mới")
        }
        OutlinedButton(
            onClick = onSavedLayoutsClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Text(text = "Bố cục đã lưu")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    DexWorkspaceManagerTheme {
        HomeScreen(
            onCreateLayoutClick = {},
            onSavedLayoutsClick = {}
        )
    }
}
