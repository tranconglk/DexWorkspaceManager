package com.trancong.dexworkspacemanager

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trancong.dexworkspacemanager.ui.theme.DexWorkspaceManagerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLaunchClick: () -> Unit,
    onNewLayoutClick: () -> Unit,
    onSavedLayoutsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.home_title))
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_title),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onNewLayoutClick,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.new_layout))
                }
                Button(onClick = onSavedLayoutsClick) {
                    Text(text = stringResource(R.string.saved_layouts))
                }
                Button(onClick = onSettingsClick) {
                    Text(text = stringResource(R.string.settings))
                }
                Button(onClick = onLaunchClick) {
                    Text(text = stringResource(R.string.launch))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    DexWorkspaceManagerTheme {
        HomeScreen(
            onLaunchClick = {},
            onNewLayoutClick = {},
            onSavedLayoutsClick = {},
            onSettingsClick = {}
        )
    }
}
