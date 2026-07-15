package com.trancong.dexworkspacemanager.feature.home

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trancong.dexworkspacemanager.DexWorkspaceManagerApplication
import com.trancong.dexworkspacemanager.domain.model.Workspace
import com.trancong.dexworkspacemanager.feature.layouteditor.WorkspaceLaunchResult
import com.trancong.dexworkspacemanager.platform.applauncher.currentExternalDisplayWorkArea
import com.trancong.dexworkspacemanager.platform.dex.isRunningOnExternalDisplay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(
    onCreateLayoutClick: () -> Unit,
    onSavedLayoutsClick: () -> Unit,
    onEditWorkspace: (Long) -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as DexWorkspaceManagerApplication
    val activity = context.findActivity()
    val workspaces by application.container.workspaceRepository.observeAll()
        .collectAsState(initial = emptyList())
    val recentWorkspaces = workspaces.sortedByDescending(Workspace::updatedAt).take(4)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var launchJob by remember { mutableStateOf<Job?>(null) }
    var showQuickAccessGuide by rememberSaveable { mutableStateOf(true) }

    DisposableEffect(Unit) {
        onDispose { launchJob?.cancel() }
    }

    HomeScreen(
        recentWorkspaces = recentWorkspaces,
        showQuickAccessGuide = showQuickAccessGuide,
        onDismissQuickAccessGuide = { showQuickAccessGuide = false },
        onCreateLayoutClick = onCreateLayoutClick,
        onSavedLayoutsClick = onSavedLayoutsClick,
        onEditWorkspace = onEditWorkspace,
        onLaunchWorkspace = { workspace ->
            if (launchJob?.isActive != true) {
                val workArea = activity?.currentExternalDisplayWorkArea()
                if (activity == null || !activity.isRunningOnExternalDisplay() || workArea == null) {
                    Log.w(
                        DEX_ONLY_LOG_TAG,
                        "Workspace launch unavailable because the DeX display is not available"
                    )
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "Hãy mở DeX Workspace Manager trực tiếp trên màn hình DeX"
                        )
                    }
                } else {
                    launchJob = scope.launch {
                        val result = application.container.workspaceLaunchCoordinator.launch(
                            activity = activity,
                            workspace = workspace,
                            workArea = workArea,
                            onProgress = { _, _, _ -> }
                        )
                        snackbarHostState.showSnackbar(result.message(workspace.appAssignments.size))
                        launchJob = null
                    }
                }
            }
        },
        snackbarHostState = snackbarHostState
    )
}

@Composable
fun HomeScreen(
    recentWorkspaces: List<Workspace>,
    showQuickAccessGuide: Boolean,
    onDismissQuickAccessGuide: () -> Unit,
    onCreateLayoutClick: () -> Unit,
    onSavedLayoutsClick: () -> Unit,
    onEditWorkspace: (Long) -> Unit,
    onLaunchWorkspace: (Workspace) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        SnackbarHost(hostState = snackbarHostState)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "DeX Workspace Manager",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (showQuickAccessGuide) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Để mở nhanh:\nKéo biểu tượng DeX Workspace Manager " +
                                    "ra desktop hoặc taskbar DeX."
                            )
                            TextButton(onClick = onDismissQuickAccessGuide) { Text("Đóng") }
                        }
                    }
                }
            }
            item {
                Button(onClick = onCreateLayoutClick, modifier = Modifier.fillMaxWidth()) {
                    Text("Tạo bố cục mới")
                }
            }
            item {
                Text("Workspace gần đây", style = MaterialTheme.typography.titleLarge)
            }
            items(recentWorkspaces, key = Workspace::id) { workspace ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(workspace.name, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onLaunchWorkspace(workspace) },
                                enabled = workspace.appAssignments.isNotEmpty(),
                                modifier = Modifier.weight(1f)
                            ) { Text("Khởi chạy") }
                            OutlinedButton(
                                onClick = { onEditWorkspace(workspace.id) },
                                modifier = Modifier.weight(1f)
                            ) { Text("Chỉnh sửa") }
                        }
                    }
                }
            }
            item {
                OutlinedButton(onClick = onSavedLayoutsClick, modifier = Modifier.fillMaxWidth()) {
                    Text("Xem tất cả")
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun WorkspaceLaunchResult.message(total: Int): String = when (this) {
    is WorkspaceLaunchResult.Success -> "Đã mở $launchedCount ứng dụng"
    is WorkspaceLaunchResult.PartialSuccess -> "Đã mở $launchedCount/$total ứng dụng"
    WorkspaceLaunchResult.NoAssignedApps -> "Workspace chưa được gán ứng dụng"
    WorkspaceLaunchResult.NotRunningOnDex -> "Workspace chỉ có thể khởi chạy trên DeX"
    is WorkspaceLaunchResult.Cancelled -> "Đã dừng sau khi mở $launchedCount ứng dụng"
}

private const val DEX_ONLY_LOG_TAG = "DexOnlyMode"
