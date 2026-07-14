package com.trancong.dexworkspacemanager.feature.savedlayouts

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trancong.dexworkspacemanager.DexWorkspaceManagerApplication
import com.trancong.dexworkspacemanager.domain.model.Workspace
import com.trancong.dexworkspacemanager.feature.layouteditor.LayoutTemplate
import com.trancong.dexworkspacemanager.platform.applauncher.currentExternalDisplayWorkArea
import com.trancong.dexworkspacemanager.platform.applauncher.isRunningOnExternalDisplay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun SavedLayoutsRoute(
    onBackClick: () -> Unit,
    onWorkspaceClick: (Workspace) -> Unit = {}
) {
    val application = LocalContext.current.applicationContext as DexWorkspaceManagerApplication
    val viewModelFactory = remember(application) {
        SavedLayoutsViewModelFactory(application.container.workspaceRepository)
    }
    val viewModel: SavedLayoutsViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val routeScope = rememberCoroutineScope()
    val hostActivity = LocalContext.current.findActivity()
    var launchJob by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(Unit) {
        onDispose { launchJob?.cancel() }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.consumeError()
        }
    }

    LaunchedEffect(uiState.launchMessage) {
        uiState.launchMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeLaunchMessage()
        }
    }

    LaunchedEffect(uiState.launchError) {
        uiState.launchError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeLaunchError()
        }
    }

    SavedLayoutsScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onWorkspaceClick = onWorkspaceClick,
        onDeleteClick = viewModel::requestDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onCancelDelete = viewModel::cancelDelete,
        onLaunchWorkspace = { workspace ->
            if (launchJob?.isActive != true) {
                val activity = hostActivity
                val workArea = activity?.currentExternalDisplayWorkArea()
                when {
                    activity == null || !activity.isRunningOnExternalDisplay() || workArea == null ->
                        viewModel.finishWorkspaceLaunch(
                            workspace.id,
                            com.trancong.dexworkspacemanager.feature.layouteditor.WorkspaceLaunchResult.NotRunningOnDex
                        )
                    else -> {
                        viewModel.startWorkspaceLaunch(workspace.id, workspace.appAssignments.size)
                        launchJob = routeScope.launch {
                            val result = application.container.workspaceLaunchCoordinator.launch(
                                activity = activity,
                                workspace = workspace,
                                workArea = workArea
                            ) { completed, total, assignment ->
                                viewModel.updateWorkspaceLaunchProgress(
                                    workspaceId = workspace.id,
                                    completedApps = completed,
                                    totalApps = total,
                                    currentZoneId = assignment?.zoneId,
                                    currentAppLabel = assignment?.appLabel
                                )
                            }
                            viewModel.finishWorkspaceLaunch(workspace.id, result)
                            launchJob = null
                        }
                    }
                }
            }
        },
        onCancelWorkspaceLaunch = { launchJob?.cancel() },
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedLayoutsScreen(
    uiState: SavedLayoutsUiState,
    onBackClick: () -> Unit,
    onWorkspaceClick: (Workspace) -> Unit = {},
    onDeleteClick: (Workspace) -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onLaunchWorkspace: (Workspace) -> Unit,
    onCancelWorkspaceLaunch: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    uiState.workspacePendingDelete?.let { workspace ->
        AlertDialog(
            onDismissRequest = onCancelDelete,
            title = { Text(text = "Xóa workspace?") },
            text = { Text(text = "Bạn có muốn xóa workspace \"${workspace.name}\" không?") },
            confirmButton = {
                TextButton(onClick = onConfirmDelete) {
                    Text(text = "Xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelDelete) {
                    Text(text = "Hủy")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = "Bố cục đã lưu") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )

            uiState.workspaces.isEmpty() -> EmptyContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = uiState.workspaces,
                    key = { workspace -> workspace.id }
                ) { workspace ->
                    WorkspaceCard(
                        workspace = workspace,
                        launchingWorkspaceId = uiState.launchingWorkspaceId,
                        launchProgress = uiState.launchProgress,
                        onClick = { onWorkspaceClick(workspace) },
                        onDeleteClick = { onDeleteClick(workspace) },
                        onLaunchClick = { onLaunchWorkspace(workspace) },
                        onCancelLaunch = onCancelWorkspaceLaunch
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Chưa có workspace nào",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Hãy tạo và lưu một bố cục mới.",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun WorkspaceCard(
    workspace: Workspace,
    launchingWorkspaceId: Long?,
    launchProgress: com.trancong.dexworkspacemanager.feature.layouteditor.WorkspaceLaunchProgress,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onLaunchClick: () -> Unit,
    onCancelLaunch: () -> Unit
) {
    val isLaunching = launchingWorkspaceId == workspace.id
    val anotherWorkspaceIsLaunching = launchingWorkspaceId != null && !isLaunching
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLaunching, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = workspace.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(text = "Mẫu: ${workspace.template.displayName()}")
                Text(text = "Tỷ lệ trái: ${workspace.leftRatio.toPercent()}%")
                if (workspace.template == LayoutTemplate.THREE_ZONES) {
                    Text(text = "Tỷ lệ trên: ${workspace.topRatio.toPercent()}%")
                }
                Text(
                    text = "Cập nhật: ${formatTimestamp(workspace.updatedAt)}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (isLaunching) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.width(20.dp))
                        Text(
                            text = launchProgress.currentAppLabel?.let { "Đang mở $it" }
                                ?: "Đang chuẩn bị",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text("${launchProgress.completedApps}/${launchProgress.totalApps}")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = onLaunchClick,
                        enabled = !isLaunching && !anotherWorkspaceIsLaunching &&
                            workspace.appAssignments.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Khởi chạy ${workspace.name}"
                        )
                        Text("Khởi chạy")
                    }
                    if (isLaunching) {
                        TextButton(onClick = onCancelLaunch) { Text("Dừng") }
                    }
                }
            }
            IconButton(onClick = onDeleteClick, enabled = !isLaunching) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Xóa ${workspace.name}"
                )
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun LayoutTemplate.displayName(): String = when (this) {
    LayoutTemplate.TWO_ZONES -> "2 vùng"
    LayoutTemplate.THREE_ZONES -> "3 vùng"
    LayoutTemplate.EMPTY -> "Trống"
}

private fun Float.toPercent(): Int = (this * 100).toInt()

private fun formatTimestamp(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))
