package com.trancong.dexworkspacemanager.feature.workspacetransfer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trancong.dexworkspacemanager.DexWorkspaceManagerApplication

@Composable
fun WorkspaceTransferRoute(onBackClick: () -> Unit) {
    val application = LocalContext.current.applicationContext as DexWorkspaceManagerApplication
    val factory = remember(application) {
        WorkspaceTransferViewModelFactory(
            application.container.workspaceRepository,
            application.container.workspaceTransferDirectory
        )
    }
    val viewModel: WorkspaceTransferViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.message) {
        uiState.message?.let { snackbarHostState.showSnackbar(it); viewModel.consumeMessage() }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it); viewModel.consumeError() }
    }
    WorkspaceTransferScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onRefresh = viewModel::refresh,
        onFileClick = viewModel::selectFile,
        onConfirmImport = viewModel::confirmImport,
        onCancelImport = viewModel::cancelPreview,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceTransferScreen(
    uiState: WorkspaceTransferUiState,
    onBackClick: () -> Unit,
    onRefresh: () -> Unit,
    onFileClick: (String) -> Unit,
    onConfirmImport: () -> Unit,
    onCancelImport: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    uiState.previewWorkspace?.let { workspace ->
        AlertDialog(
            onDismissRequest = onCancelImport,
            title = { Text("Import workspace") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(workspace.name, fontWeight = FontWeight.SemiBold)
                    Text("Template: ${workspace.template.name}")
                    Text("Số ứng dụng: ${workspace.appAssignments.size}")
                    Text("Delay: ${workspace.launchDelayMs} ms")
                }
            },
            confirmButton = { TextButton(onClick = onConfirmImport) { Text("Import") } },
            dismissButton = { TextButton(onClick = onCancelImport) { Text("Hủy") } }
        )
    }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Import workspace") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Làm mới")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            uiState.importFiles.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Chưa có file JSON trong thư mục imports")
                Text(
                    "Sao chép file vào Android/data/com.trancong.dexworkspacemanager/files/imports",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.importFiles, key = { it }) { fileName ->
                    Card(Modifier.fillMaxWidth().clickable { onFileClick(fileName) }) {
                        Text(fileName, Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}
