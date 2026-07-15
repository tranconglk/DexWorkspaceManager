package com.trancong.dexworkspacemanager.feature.home

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trancong.dexworkspacemanager.DexWorkspaceManagerApplication
import com.trancong.dexworkspacemanager.domain.model.Workspace
import com.trancong.dexworkspacemanager.feature.layouteditor.LayoutTemplate
import com.trancong.dexworkspacemanager.feature.layouteditor.WorkspaceLaunchProgress
import com.trancong.dexworkspacemanager.feature.layouteditor.WorkspaceLaunchResult
import com.trancong.dexworkspacemanager.platform.applauncher.currentExternalDisplayWorkArea
import com.trancong.dexworkspacemanager.platform.applauncher.isRunningOnExternalDisplay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(
    onCreateWorkspace: () -> Unit,
    onViewAllWorkspaces: () -> Unit,
    onEditWorkspace: (Long) -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as DexWorkspaceManagerApplication
    val factory = remember(application) {
        HomeViewModelFactory(application.container.workspaceRepository)
    }
    val viewModel: HomeViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()
    val activity = context.findActivity()
    val snackbarHostState = remember { SnackbarHostState() }
    val routeScope = rememberCoroutineScope()
    var launchJob by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(Unit) {
        onDispose { launchJob?.cancel() }
    }
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeError()
        }
    }

    HomeScreen(
        uiState = uiState,
        onCreateWorkspace = onCreateWorkspace,
        onViewAllWorkspaces = onViewAllWorkspaces,
        onEditWorkspace = onEditWorkspace,
        onToggleFavorite = viewModel::toggleFavorite,
        onRenameWorkspace = viewModel::requestRename,
        onDuplicateWorkspace = viewModel::requestDuplicate,
        onConfirmRename = viewModel::confirmRename,
        onCancelRename = viewModel::cancelRename,
        onConfirmDuplicate = viewModel::confirmDuplicate,
        onCancelDuplicate = viewModel::cancelDuplicate,
        onLaunchWorkspace = { workspace ->
            if (launchJob?.isActive != true) {
                val workArea = activity?.currentExternalDisplayWorkArea()
                if (activity == null || !activity.isRunningOnExternalDisplay() || workArea == null) {
                    viewModel.finishWorkspaceLaunch(
                        workspace.id,
                        WorkspaceLaunchResult.NotRunningOnDex
                    )
                } else {
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
        },
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onCreateWorkspace: () -> Unit,
    onViewAllWorkspaces: () -> Unit,
    onEditWorkspace: (Long) -> Unit,
    onLaunchWorkspace: (Workspace) -> Unit,
    onToggleFavorite: (Workspace) -> Unit,
    onRenameWorkspace: (Workspace) -> Unit,
    onDuplicateWorkspace: (Workspace) -> Unit,
    onConfirmRename: (String) -> Unit,
    onCancelRename: () -> Unit,
    onConfirmDuplicate: (String) -> Unit,
    onCancelDuplicate: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    uiState.workspacePendingRename?.let { workspace ->
        WorkspaceNameDialog(
            workspace = workspace,
            title = "Đổi tên workspace",
            confirmLabel = "Lưu",
            initialName = workspace.name,
            onConfirm = onConfirmRename,
            onCancel = onCancelRename
        )
    }
    uiState.workspacePendingDuplicate?.let { workspace ->
        WorkspaceNameDialog(
            workspace = workspace,
            title = "Sao chép workspace",
            confirmLabel = "Sao chép",
            initialName = "${workspace.name} - Bản sao",
            onConfirm = onConfirmDuplicate,
            onCancel = onCancelDuplicate
        )
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("DeX Workspace Manager") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            val columns = if (maxWidth >= 840.dp) 2 else 1
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Workspace gần đây",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                when {
                    uiState.isLoading -> item {
                        Box(Modifier.fillMaxWidth().padding(48.dp), Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    uiState.workspaces.isEmpty() -> item {
                        EmptyWorkspaceContent(onCreateWorkspace)
                    }
                    else -> items(uiState.workspaces.chunked(columns)) { rowWorkspaces ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            rowWorkspaces.forEach { workspace ->
                                WorkspaceProfileCard(
                                    workspace = workspace,
                                    isLaunching = uiState.launchingWorkspaceId == workspace.id,
                                    launchProgress = uiState.launchProgress,
                                    onEdit = { onEditWorkspace(workspace.id) },
                                    onLaunch = { onLaunchWorkspace(workspace) },
                                    onToggleFavorite = { onToggleFavorite(workspace) },
                                    onRename = { onRenameWorkspace(workspace) },
                                    onDuplicate = { onDuplicateWorkspace(workspace) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowWorkspaces.size < columns) Box(Modifier.weight(1f))
                        }
                    }
                }
                if (!uiState.isLoading && uiState.workspaces.isNotEmpty()) {
                    item {
                        OutlinedButton(onClick = onCreateWorkspace, Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text("  Tạo workspace mới")
                        }
                    }
                }
                item {
                    OutlinedButton(onClick = onViewAllWorkspaces, Modifier.fillMaxWidth()) {
                        Text("Xem tất cả workspace")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyWorkspaceContent(onCreateWorkspace: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Chưa có workspace nào", style = MaterialTheme.typography.titleLarge)
            Text("Tạo workspace đầu tiên để mở nhiều ứng dụng đúng vị trí trên DeX.")
            Button(onClick = onCreateWorkspace, modifier = Modifier.fillMaxWidth()) {
                Text("Tạo workspace đầu tiên")
            }
        }
    }
}

@Composable
private fun WorkspaceProfileCard(
    workspace: Workspace,
    isLaunching: Boolean,
    launchProgress: WorkspaceLaunchProgress,
    onEdit: () -> Unit,
    onLaunch: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(modifier = modifier.clickable(onClick = onEdit)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    workspace.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (workspace.isFavorite) {
                            Icons.Default.Favorite
                        } else {
                            Icons.Default.FavoriteBorder
                        },
                        contentDescription = if (workspace.isFavorite) {
                            "Bỏ yêu thích ${workspace.name}"
                        } else {
                            "Đánh dấu yêu thích ${workspace.name}"
                        }
                    )
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Thao tác ${workspace.name}")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Đổi tên") },
                            onClick = { menuExpanded = false; onRename() }
                        )
                        DropdownMenuItem(
                            text = { Text("Sao chép") },
                            onClick = { menuExpanded = false; onDuplicate() }
                        )
                    }
                }
            }
            val assignments = workspace.appAssignments.sortedBy { it.launchOrder }
            assignments.take(3).forEach { assignment ->
                Text("• ${assignment.appLabel}", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (assignments.size > 3) Text("+${assignments.size - 3} ứng dụng")
            if (assignments.isEmpty()) Text("Chưa gán ứng dụng")
            Text("${workspace.zoneCount()} vùng  •  Chờ ${workspace.launchDelayMs} ms")
            if (isLaunching) {
                LinearProgressIndicator(
                    progress = { launchProgress.progressFraction },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    launchProgress.currentAppLabel?.let { "Đang mở $it" }
                        ?: "Đang khởi chạy…"
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onLaunch,
                    enabled = !isLaunching && assignments.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Text(" Khởi chạy")
                }
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Text(" Chỉnh sửa")
                }
            }
        }
    }
}

@Composable
private fun WorkspaceNameDialog(
    workspace: Workspace,
    title: String,
    confirmLabel: String,
    initialName: String,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit
) {
    var name by rememberSaveable(workspace.id, title) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tên workspace") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(confirmLabel)
            }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Hủy") } }
    )
}

private fun Workspace.zoneCount(): Int = when (template) {
    LayoutTemplate.TWO_ZONES -> 2
    LayoutTemplate.THREE_ZONES -> 3
    LayoutTemplate.EMPTY -> 0
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
