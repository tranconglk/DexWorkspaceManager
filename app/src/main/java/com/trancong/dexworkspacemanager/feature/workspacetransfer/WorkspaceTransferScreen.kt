package com.trancong.dexworkspacemanager.feature.workspacetransfer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import com.trancong.dexworkspacemanager.platform.transfer.WorkspaceRestoreMode
import java.text.DateFormat
import java.util.Date

@Composable
fun WorkspaceTransferRoute(onBackClick: () -> Unit) {
    val application = LocalContext.current.applicationContext as DexWorkspaceManagerApplication
    val factory = remember(application) {
        WorkspaceTransferViewModelFactory(
            application.container.workspaceRepository,
            application.container.workspaceTransferDirectory,
            application.container.workspaceBackupManager
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
        onCreateBackup = viewModel::createBackup,
        onBackupFileClick = viewModel::selectBackup,
        onRestoreModeSelected = viewModel::selectRestoreMode,
        onConfirmRestore = viewModel::confirmRestore,
        onCancelBackupPreview = viewModel::cancelBackupPreview,
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
    onCreateBackup: () -> Unit,
    onBackupFileClick: (String) -> Unit,
    onRestoreModeSelected: (WorkspaceRestoreMode) -> Unit,
    onConfirmRestore: () -> Unit,
    onCancelBackupPreview: () -> Unit,
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
    uiState.selectedBackupPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = onCancelBackupPreview,
            title = {
                Text(
                    if (uiState.selectedRestoreMode == WorkspaceRestoreMode.REPLACE_ALL) {
                        "Thay thế toàn bộ workspace?"
                    } else {
                        "Khôi phục backup"
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (uiState.selectedRestoreMode == WorkspaceRestoreMode.REPLACE_ALL) {
                        Text("Tất cả workspace hiện tại sẽ bị thay thế bằng dữ liệu trong bản backup.")
                    }
                    Text(preview.fileName, fontWeight = FontWeight.SemiBold)
                    Text("Ngày xuất: ${formatTimestamp(preview.exportedAt)}")
                    Text("Số workspace: ${preview.workspaceCount}")
                    Text("Yêu thích: ${preview.favoriteCount}")
                    Text("Tổng app assignments: ${preview.totalAssignments}")
                    preview.workspaceNames.forEach { Text("• $it") }
                    RestoreModeOption(
                        selected = uiState.selectedRestoreMode,
                        onSelected = onRestoreModeSelected
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirmRestore, enabled = !uiState.isRestoring) {
                    Text(
                        if (uiState.selectedRestoreMode == WorkspaceRestoreMode.REPLACE_ALL) {
                            "Thay thế"
                        } else {
                            "Thêm ${preview.workspaceCount} workspace"
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelBackupPreview, enabled = !uiState.isRestoring) {
                    Text("Hủy")
                }
            }
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
        if (uiState.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Backup toàn bộ", style = MaterialTheme.typography.titleLarge)
                }
                item {
                    Button(
                        onClick = onCreateBackup,
                        enabled = !uiState.isCreatingBackup && !uiState.isRestoring,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isCreatingBackup) CircularProgressIndicator()
                        else Text("Tạo bản backup")
                    }
                }
                item {
                    Text("Khôi phục backup", style = MaterialTheme.typography.titleLarge)
                }
                if (uiState.backupFiles.isEmpty()) {
                    item { Text("Chưa có file backup collection trong thư mục imports") }
                } else {
                    items(uiState.backupFiles, key = { "backup:$it" }) { fileName ->
                        Card(Modifier.fillMaxWidth().clickable { onBackupFileClick(fileName) }) {
                            Text(fileName, Modifier.padding(16.dp))
                        }
                    }
                }
                item {
                    Text("Import một workspace", style = MaterialTheme.typography.titleLarge)
                }
                if (uiState.importFiles.isEmpty()) {
                    item {
                        Text("Chưa có file JSON trong thư mục imports")
                    }
                }
                items(uiState.importFiles, key = { it }) { fileName ->
                    Card(Modifier.fillMaxWidth().clickable { onFileClick(fileName) }) {
                        Text(fileName, Modifier.padding(16.dp))
                    }
                }
                item {
                    Text(
                        "Sao chép file vào Android/data/com.trancong.dexworkspacemanager/files/imports",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun RestoreModeOption(
    selected: WorkspaceRestoreMode,
    onSelected: (WorkspaceRestoreMode) -> Unit
) {
    Column {
        WorkspaceRestoreMode.entries.forEach { mode ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected == mode, onClick = { onSelected(mode) })
                Text(
                    if (mode == WorkspaceRestoreMode.MERGE) {
                        "Thêm vào dữ liệu hiện có"
                    } else {
                        "Thay thế toàn bộ dữ liệu hiện có"
                    }
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))
