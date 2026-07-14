package com.trancong.dexworkspacemanager.feature.apppicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trancong.dexworkspacemanager.DexWorkspaceManagerApplication
import com.trancong.dexworkspacemanager.platform.installedapps.InstalledApp

@Composable
fun AppPickerRoute(
    zoneId: String,
    onBackClick: () -> Unit,
    onAppSelected: (zoneId: String, app: InstalledApp) -> Unit
) {
    val application = LocalContext.current.applicationContext as DexWorkspaceManagerApplication
    val factory = remember(application) {
        AppPickerViewModelFactory(application.container.installedAppsProvider)
    }
    val viewModel: AppPickerViewModel = viewModel(factory = factory)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleAwareState = remember(viewModel, lifecycle) {
        viewModel.uiState.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
    }
    val uiState by lifecycleAwareState.collectAsState(initial = viewModel.uiState.value)

    AppPickerScreen(
        uiState = uiState,
        onSearchQueryChange = viewModel::updateSearchQuery,
        onBackClick = onBackClick,
        onRetryClick = viewModel::loadApps,
        onAppClick = { app -> onAppSelected(zoneId, app) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerScreen(
    uiState: AppPickerUiState,
    onSearchQueryChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onAppClick: (InstalledApp) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Chọn ứng dụng") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Tìm ứng dụng") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            when {
                uiState.isLoading -> CenteredContent { CircularProgressIndicator() }
                uiState.error != null -> CenteredContent {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.error)
                        TextButton(onClick = onRetryClick) { Text("Thử lại") }
                    }
                }
                uiState.filteredApps.isEmpty() -> CenteredContent {
                    Text("Không tìm thấy ứng dụng")
                }
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(
                        items = uiState.filteredApps,
                        key = { app -> "${app.packageName}/${app.activityName}" }
                    ) { app ->
                        ListItem(
                            headlineContent = { Text(app.label) },
                            supportingContent = {
                                Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                            },
                            modifier = Modifier.clickable { onAppClick(app) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CenteredContent(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
