package com.trancong.dexworkspacemanager.feature.layouteditor

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.SavedStateHandle
import com.trancong.dexworkspacemanager.DexWorkspaceManagerApplication
import com.trancong.dexworkspacemanager.domain.model.Workspace
import com.trancong.dexworkspacemanager.domain.model.WorkspaceAppAssignment
import com.trancong.dexworkspacemanager.navigation.AppRoute
import com.trancong.dexworkspacemanager.platform.dex.DexDisplayState
import com.trancong.dexworkspacemanager.platform.dex.ExternalDisplayInfo
import com.trancong.dexworkspacemanager.platform.dex.DexWorkArea
import com.trancong.dexworkspacemanager.platform.applauncher.AppLaunchResult
import com.trancong.dexworkspacemanager.platform.applauncher.LaunchBounds
import com.trancong.dexworkspacemanager.platform.applauncher.LayoutBoundsCalculator
import com.trancong.dexworkspacemanager.platform.applauncher.currentExternalDisplayWorkArea
import com.trancong.dexworkspacemanager.platform.applauncher.isRunningOnExternalDisplay
import com.trancong.dexworkspacemanager.ui.theme.DexWorkspaceManagerTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun LayoutEditorRoute(
    workspaceId: Long?,
    savedStateHandle: SavedStateHandle,
    onBackClick: () -> Unit,
    onZoneClick: (String) -> Unit
) {
    val currentContext = LocalContext.current
    val density = LocalDensity.current
    val application = currentContext.applicationContext as DexWorkspaceManagerApplication
    val routeScope = rememberCoroutineScope()
    var lastDiagnosticDetails by remember { mutableStateOf<String?>(null) }
    var workspaceLaunchJob by remember { mutableStateOf<Job?>(null) }
    val viewModelFactory = remember(application, workspaceId) {
        LayoutEditorViewModelFactory(
            workspaceRepository = application.container.workspaceRepository,
            appLauncher = application.container.appLauncher,
            dexDisplayProvider = application.container.dexDisplayProvider,
            workspaceId = workspaceId
        )
    }
    val viewModel: LayoutEditorViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    val resultZoneId by savedStateHandle
        .getStateFlow<String?>(AppRoute.RESULT_ZONE_ID, null)
        .collectAsState()
    val resultPackageName by savedStateHandle
        .getStateFlow<String?>(AppRoute.RESULT_PACKAGE_NAME, null)
        .collectAsState()
    val resultActivityName by savedStateHandle
        .getStateFlow<String?>(AppRoute.RESULT_ACTIVITY_NAME, null)
        .collectAsState()
    val resultAppLabel by savedStateHandle
        .getStateFlow<String?>(AppRoute.RESULT_APP_LABEL, null)
        .collectAsState()

    LaunchedEffect(resultZoneId, resultPackageName, resultActivityName, resultAppLabel) {
        val zoneId = resultZoneId
        val packageName = resultPackageName
        val activityName = resultActivityName
        val appLabel = resultAppLabel
        if (zoneId != null && packageName != null && activityName != null && appLabel != null) {
            viewModel.assignAppToZone(zoneId, packageName, activityName, appLabel)
            savedStateHandle.remove<String>(AppRoute.RESULT_ZONE_ID)
            savedStateHandle.remove<String>(AppRoute.RESULT_PACKAGE_NAME)
            savedStateHandle.remove<String>(AppRoute.RESULT_ACTIVITY_NAME)
            savedStateHandle.remove<String>(AppRoute.RESULT_APP_LABEL)
        }
    }

    val hostActivity = currentContext.findActivity()
    val sourceDisplayId = hostActivity?.currentDisplayId()
    val detectedExternalDisplayIds = uiState.dexDisplayState.externalDisplayIds()
    val isRunningOnDex = hostActivity?.isRunningOnExternalDisplay() == true
    val dexLaunchDescription = if (isRunningOnDex) {
        "Đang chạy trực tiếp trên màn hình DeX"
    } else if (detectedExternalDisplayIds.isNotEmpty()) {
        "Hãy mở Workspace Manager trực tiếp trên màn hình DeX"
    } else {
        "Chưa phát hiện DeX"
    }

    when {
        uiState.isLoadingWorkspace -> LayoutEditorLoadingState()
        uiState.loadError != null -> LayoutEditorLoadError(
            message = uiState.loadError.orEmpty(),
            onBackClick = onBackClick
        )
        else -> LayoutEditorScreen(
            isEditingWorkspace = uiState.workspaceId != null,
            selectedTemplate = uiState.selectedTemplate,
            onTemplateSelected = viewModel::selectTemplate,
            onResetLayout = viewModel::resetLayout,
            leftRatio = uiState.leftRatio,
            onLeftRatioChange = viewModel::updateLeftRatio,
            topRatio = uiState.topRatio,
            onTopRatioChange = viewModel::updateTopRatio,
            appAssignments = uiState.appAssignments,
            launchDelayMs = uiState.launchDelayMs,
            onLaunchDelayChange = viewModel::updateLaunchDelay,
            onMoveAssignmentUp = viewModel::moveAssignmentUp,
            onMoveAssignmentDown = viewModel::moveAssignmentDown,
            onZoneClick = onZoneClick,
            onRemoveAppFromZone = viewModel::removeAppFromZone,
            onLaunchAppForZone = viewModel::launchAssignedApp,
            dexDisplayState = uiState.dexDisplayState,
            selectedExternalDisplayId = uiState.selectedExternalDisplayId,
            canLaunchOnDex = isRunningOnDex,
            dexLaunchDescription = dexLaunchDescription,
            sourceDisplayId = sourceDisplayId,
            detectedExternalDisplayIds = detectedExternalDisplayIds,
            sourceWorkArea = hostActivity?.currentExternalDisplayWorkArea(),
            lastDiagnosticDetails = lastDiagnosticDetails,
            onRefreshDexDisplay = viewModel::refreshDexDisplayState,
            onSelectExternalDisplay = viewModel::selectExternalDisplay,
            onLaunchOnDex = { zoneId ->
                val assignment = uiState.appAssignments[zoneId]
                val zone = LayoutTemplates.zonesFor(
                    uiState.selectedTemplate,
                    uiState.leftRatio,
                    uiState.topRatio
                ).firstOrNull { it.id == zoneId }
                val workArea = hostActivity?.currentExternalDisplayWorkArea()
                when {
                    assignment == null -> viewModel.reportLaunchError(
                        "Vùng này chưa được gán ứng dụng"
                    )
                    hostActivity == null || !isRunningOnDex -> viewModel.reportLaunchError(
                        "Hãy mở Workspace Manager trực tiếp trên màn hình DeX"
                    )
                    zone == null || workArea == null -> viewModel.reportLaunchError(
                        "Không xác định được vùng làm việc DeX"
                    )
                    else -> {
                        val result = try {
                            val bounds = LayoutBoundsCalculator.calculate(
                                zone,
                                workArea.width,
                                workArea.usableHeight,
                                marginPx = with(density) { 8.dp.roundToPx() }
                            )
                            Log.d(
                                LAUNCH_LOG_TAG,
                                "sourceActivityDisplayId=$sourceDisplayId, " +
                                    "detectedExternalDisplayIds=$detectedExternalDisplayIds, " +
                                    "workArea=$workArea, " +
                                    "bounds=$bounds, packageName=${assignment.packageName}"
                            )
                            lastDiagnosticDetails = diagnosticDetails(
                                workArea,
                                bounds,
                                assignment
                            )
                            application.container.foregroundAppLauncher.launchInZone(
                                hostActivity,
                                assignment.packageName,
                                assignment.activityName,
                                bounds
                            )
                        } catch (exception: IllegalArgumentException) {
                            AppLaunchResult.InvalidBounds
                        }
                        viewModel.handleBoundsLaunchResult(result)
                    }
                }
            },
            workspaceLaunchProgress = uiState.workspaceLaunchProgress,
            workspaceLaunchMessage = uiState.workspaceLaunchMessage,
            workspaceLaunchError = uiState.workspaceLaunchError,
            onLaunchWorkspace = {
                if (workspaceLaunchJob?.isActive != true) {
                    val activity = hostActivity
                    val workspace = uiState.toWorkspaceSnapshot()
                    when {
                        activity == null || !activity.isRunningOnExternalDisplay() ->
                            viewModel.finishWorkspaceLaunch(
                                WorkspaceLaunchResult.NotRunningOnDex
                            )
                        workspace.appAssignments.isEmpty() -> viewModel.finishWorkspaceLaunch(
                            WorkspaceLaunchResult.NoAssignedApps
                        )
                        else -> {
                            val workArea = activity.currentExternalDisplayWorkArea()
                            viewModel.startWorkspaceLaunch(workspace.appAssignments.size)
                            if (workArea == null) {
                                viewModel.finishWorkspaceLaunch(
                                    WorkspaceLaunchResult.NotRunningOnDex
                                )
                            } else {
                                workspaceLaunchJob = routeScope.launch {
                                    val result = application.container.workspaceLaunchCoordinator
                                        .launch(activity, workspace, workArea) {
                                            completed, total, assignment ->
                                            viewModel.updateWorkspaceLaunchProgress(
                                                completedApps = completed,
                                                totalApps = total,
                                                currentZoneId = assignment?.zoneId,
                                                currentAppLabel = assignment?.appLabel
                                            )
                                        }
                                    viewModel.finishWorkspaceLaunch(result)
                                    workspaceLaunchJob = null
                                }
                            }
                        }
                    }
                }
            },
            onCancelWorkspaceLaunch = {
                workspaceLaunchJob?.cancel()
            },
            onWorkspaceLaunchMessageShown = viewModel::consumeWorkspaceLaunchMessage,
            onWorkspaceLaunchErrorShown = viewModel::consumeWorkspaceLaunchError,
            onTestTargetDisplay = { zoneId ->
                val assignment = uiState.appAssignments[zoneId]
                val zone = LayoutTemplates.zonesFor(
                    uiState.selectedTemplate,
                    uiState.leftRatio,
                    uiState.topRatio
                ).firstOrNull { it.id == zoneId }
                val targetDisplayId = uiState.selectedExternalDisplayId
                val targetDisplay = uiState.dexDisplayState.findDisplay(targetDisplayId)
                val workArea = targetDisplay?.let { DexWorkArea(it.width, it.height) }
                when {
                    assignment == null -> viewModel.reportLaunchError(
                        "Vùng này chưa được gán ứng dụng"
                    )
                    targetDisplayId == null -> viewModel.reportLaunchError(
                        "Chưa xác định được display DeX"
                    )
                    zone == null || workArea == null ->
                        viewModel.handleBoundsLaunchResult(AppLaunchResult.InvalidBounds)
                    else -> {
                        val result = try {
                            val bounds = LayoutBoundsCalculator.calculate(
                                zone,
                                workArea.width,
                                workArea.usableHeight,
                                marginPx = with(density) { 8.dp.roundToPx() }
                            )
                            Log.d(
                                LAUNCH_LOG_TAG,
                                "diagnostic targetDisplayId=$targetDisplayId, workArea=$workArea, " +
                                    "bounds=$bounds, packageName=${assignment.packageName}, " +
                                    "activityName=${assignment.activityName}"
                            )
                            lastDiagnosticDetails = diagnosticDetails(
                                workArea,
                                bounds,
                                assignment
                            )
                            application.container.appLauncher
                                .launchOnTargetDisplayForDiagnostics(
                                    assignment.packageName,
                                    assignment.activityName,
                                    targetDisplayId,
                                    bounds
                                )
                        } catch (exception: IllegalArgumentException) {
                            AppLaunchResult.InvalidBounds
                        }
                        viewModel.handleBoundsLaunchResult(result)
                    }
                }
            },
            onBackClick = onBackClick,
            workspaceName = uiState.workspaceName,
            onWorkspaceNameChange = viewModel::updateWorkspaceName,
            isNameDialogVisible = uiState.isNameDialogVisible,
            isSaving = uiState.isSaving,
            saveMessage = uiState.saveMessage,
            saveError = uiState.saveError,
            launchMessage = uiState.launchMessage,
            launchError = uiState.launchError,
            onSaveClick = viewModel::showSaveDialog,
            onConfirmSave = viewModel::saveWorkspace,
            onDismissSaveDialog = viewModel::hideSaveDialog,
            onSaveMessageShown = viewModel::consumeSaveMessage,
            onSaveErrorShown = viewModel::consumeSaveError,
            onLaunchMessageShown = viewModel::consumeLaunchMessage,
            onLaunchErrorShown = viewModel::consumeLaunchError
        )
    }
}

@Composable
private fun LayoutEditorLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun LayoutEditorLoadError(
    message: String,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge
        )
        Button(
            onClick = onBackClick,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(text = "Quay lại")
        }
    }
}

@Composable
fun LayoutEditorScreen(
    isEditingWorkspace: Boolean,
    selectedTemplate: LayoutTemplate,
    onTemplateSelected: (LayoutTemplate) -> Unit,
    onResetLayout: () -> Unit,
    leftRatio: Float,
    onLeftRatioChange: (Float) -> Unit,
    topRatio: Float,
    onTopRatioChange: (Float) -> Unit,
    appAssignments: Map<String, ZoneAppAssignment>,
    launchDelayMs: Long,
    onLaunchDelayChange: (Long) -> Unit,
    onMoveAssignmentUp: (String) -> Unit,
    onMoveAssignmentDown: (String) -> Unit,
    onZoneClick: (String) -> Unit,
    onRemoveAppFromZone: (String) -> Unit,
    onLaunchAppForZone: (String) -> Unit,
    dexDisplayState: DexDisplayState,
    selectedExternalDisplayId: Int?,
    canLaunchOnDex: Boolean,
    dexLaunchDescription: String,
    sourceDisplayId: Int?,
    detectedExternalDisplayIds: List<Int>,
    sourceWorkArea: DexWorkArea?,
    lastDiagnosticDetails: String?,
    onRefreshDexDisplay: () -> Unit,
    onSelectExternalDisplay: (Int) -> Unit,
    onLaunchOnDex: (String) -> Unit,
    workspaceLaunchProgress: WorkspaceLaunchProgress,
    workspaceLaunchMessage: String?,
    workspaceLaunchError: String?,
    onLaunchWorkspace: () -> Unit,
    onCancelWorkspaceLaunch: () -> Unit,
    onWorkspaceLaunchMessageShown: () -> Unit,
    onWorkspaceLaunchErrorShown: () -> Unit,
    onTestTargetDisplay: (String) -> Unit,
    onBackClick: () -> Unit,
    workspaceName: String,
    onWorkspaceNameChange: (String) -> Unit,
    isNameDialogVisible: Boolean,
    isSaving: Boolean,
    saveMessage: String?,
    saveError: String?,
    launchMessage: String?,
    launchError: String?,
    onSaveClick: () -> Unit,
    onConfirmSave: () -> Unit,
    onDismissSaveDialog: () -> Unit,
    onSaveMessageShown: () -> Unit,
    onSaveErrorShown: () -> Unit,
    onLaunchMessageShown: () -> Unit,
    onLaunchErrorShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var isDiagnosticsVisible by remember { mutableStateOf(false) }
    val zones = LayoutTemplates.zonesFor(
        template = selectedTemplate,
        leftRatio = leftRatio,
        topRatio = topRatio
    )

    LaunchedEffect(saveMessage) {
        saveMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onSaveMessageShown()
        }
    }

    LaunchedEffect(saveError) {
        saveError?.let { error ->
            snackbarHostState.showSnackbar(error)
            onSaveErrorShown()
        }
    }

    LaunchedEffect(launchMessage) {
        launchMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onLaunchMessageShown()
        }
    }

    LaunchedEffect(launchError) {
        launchError?.let { error ->
            snackbarHostState.showSnackbar(error)
            onLaunchErrorShown()
        }
    }
    LaunchedEffect(workspaceLaunchMessage) {
        workspaceLaunchMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onWorkspaceLaunchMessageShown()
        }
    }
    LaunchedEffect(workspaceLaunchError) {
        workspaceLaunchError?.let { error ->
            snackbarHostState.showSnackbar(error)
            onWorkspaceLaunchErrorShown()
        }
    }
    if (isDiagnosticsVisible) {
        AlertDialog(
            onDismissRequest = { isDiagnosticsVisible = false },
            title = { Text("Chẩn đoán DeX") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(dexLaunchDescription, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Source display: $sourceDisplayId • Target display: " +
                            "$selectedExternalDisplayId",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "External displays: $detectedExternalDisplayIds",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text("Source work area: $sourceWorkArea", style = MaterialTheme.typography.bodySmall)
                    lastDiagnosticDetails?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                    if (
                        dexDisplayState is DexDisplayState.Connected ||
                        dexDisplayState is DexDisplayState.MultipleDisplays
                    ) {
                        TargetDisplayDiagnosticPanel(
                            zones = zones.filter { appAssignments.containsKey(it.id) },
                            appAssignments = appAssignments,
                            onTestZone = onTestTargetDisplay
                        )
                    } else {
                        Text(
                            "Chưa có màn hình DeX để kiểm tra tương thích.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isDiagnosticsVisible = false }) { Text("Đóng") }
            }
        )
    }

    if (isNameDialogVisible) {
        AlertDialog(
            onDismissRequest = {
                if (!isSaving) onDismissSaveDialog()
            },
            title = { Text(text = "Lưu workspace") },
            text = {
                OutlinedTextField(
                    value = workspaceName,
                    onValueChange = onWorkspaceNameChange,
                    label = { Text(text = "Tên workspace") },
                    singleLine = true,
                    enabled = !isSaving
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmSave,
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(20.dp).height(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = "Lưu")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissSaveDialog,
                    enabled = !isSaving
                ) {
                    Text(text = "Hủy")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            EditorTopBar(
                isEditingWorkspace = isEditingWorkspace,
                isSaveEnabled = !workspaceLaunchProgress.isRunning,
                onBackClick = onBackClick,
                onSaveClick = onSaveClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DexStatusCard(
                state = dexDisplayState,
                selectedDisplayId = selectedExternalDisplayId,
                launchDescription = dexLaunchDescription,
                onRefresh = onRefreshDexDisplay,
                onSelectDisplay = onSelectExternalDisplay,
                onDiagnosticsClick = { isDiagnosticsVisible = true }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 900.dp),
                contentAlignment = Alignment.Center
            ) {
                LayoutPreview(
                    zones = zones,
                    selectedTemplate = selectedTemplate,
                    leftRatio = leftRatio,
                    onLeftRatioChange = onLeftRatioChange,
                    topRatio = topRatio,
                    onTopRatioChange = onTopRatioChange,
                    appAssignments = appAssignments,
                    onZoneClick = onZoneClick,
                    onRemoveAppFromZone = onRemoveAppFromZone,
                    onLaunchAppForZone = onLaunchAppForZone,
                    canLaunchOnDex = canLaunchOnDex && !workspaceLaunchProgress.isRunning,
                    isEditingEnabled = !workspaceLaunchProgress.isRunning,
                    onLaunchOnDex = onLaunchOnDex,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                )
            }

            LaunchConfigurationSection(
                zones = zones,
                assignments = appAssignments.values.sortedWith(
                    compareBy<ZoneAppAssignment> { it.launchOrder }.thenBy { it.zoneId }
                ),
                launchDelayMs = launchDelayMs,
                enabled = !workspaceLaunchProgress.isRunning,
                onMoveUp = onMoveAssignmentUp,
                onMoveDown = onMoveAssignmentDown,
                onDelayChange = onLaunchDelayChange
            )

            Button(
                onClick = onLaunchWorkspace,
                enabled = appAssignments.isNotEmpty() &&
                    canLaunchOnDex &&
                    !workspaceLaunchProgress.isRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Khởi chạy workspace")
            }

            if (workspaceLaunchProgress.isRunning) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(
                        progress = { workspaceLaunchProgress.progressFraction },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        workspaceLaunchProgress.currentAppLabel?.let { "Đang mở $it" }
                            ?: "Đang chuẩn bị khởi chạy"
                    )
                    Text(
                        "${workspaceLaunchProgress.completedApps}/" +
                            "${workspaceLaunchProgress.totalApps}"
                    )
                    TextButton(onClick = onCancelWorkspaceLaunch) {
                        Text("Dừng")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onTemplateSelected(LayoutTemplate.TWO_ZONES) },
                    enabled = !workspaceLaunchProgress.isRunning,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Mẫu 2 vùng")
                }
                Button(
                    onClick = { onTemplateSelected(LayoutTemplate.THREE_ZONES) },
                    enabled = !workspaceLaunchProgress.isRunning,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Mẫu 3 vùng")
                }
                Button(
                    onClick = onResetLayout,
                    enabled = !workspaceLaunchProgress.isRunning,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Xóa bố cục")
                }
            }
        }
    }
}

@Composable
private fun LaunchConfigurationSection(
    zones: List<LayoutZone>,
    assignments: List<ZoneAppAssignment>,
    launchDelayMs: Long,
    enabled: Boolean,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
    onDelayChange: (Long) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Thứ tự khởi chạy", style = MaterialTheme.typography.titleMedium)
            if (assignments.isEmpty()) {
                Text("Chưa có ứng dụng được gán", style = MaterialTheme.typography.bodySmall)
            } else {
                assignments.forEachIndexed { index, assignment ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${index + 1}.", modifier = Modifier.width(28.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(assignment.appLabel)
                            Text(
                                zones.firstOrNull { it.id == assignment.zoneId }?.label
                                    ?: assignment.zoneId,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        IconButton(
                            onClick = { onMoveUp(assignment.zoneId) },
                            enabled = enabled && index > 0
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowUp,
                                contentDescription = "Đưa ${assignment.appLabel} lên"
                            )
                        }
                        IconButton(
                            onClick = { onMoveDown(assignment.zoneId) },
                            enabled = enabled && index < assignments.lastIndex
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Đưa ${assignment.appLabel} xuống"
                            )
                        }
                    }
                }
            }

            Text(
                "Thời gian chờ giữa các ứng dụng",
                style = MaterialTheme.typography.titleMedium
            )
            Text("$launchDelayMs ms")
            Slider(
                value = launchDelayMs.coerceIn(0L, MAX_DELAY_SLIDER_MS).toFloat(),
                onValueChange = { value ->
                    val steppedValue =
                        (value / DELAY_SLIDER_STEP_MS).roundToInt() * DELAY_SLIDER_STEP_MS
                    onDelayChange(steppedValue.toLong())
                },
                enabled = enabled,
                valueRange = 0f..MAX_DELAY_SLIDER_MS.toFloat(),
                steps = DELAY_SLIDER_STEPS
            )
        }
    }
}

@Composable
private fun DexStatusCard(
    state: DexDisplayState,
    selectedDisplayId: Int?,
    launchDescription: String,
    onRefresh: () -> Unit,
    onSelectDisplay: (Int) -> Unit,
    onDiagnosticsClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text("Màn hình DeX", style = MaterialTheme.typography.titleMedium)
            when (state) {
                DexDisplayState.NotConnected -> Text("Chưa kết nối")
                is DexDisplayState.Connected -> {
                    Text("Đã kết nối")
                    DisplaySummary(state.display)
                }
                is DexDisplayState.MultipleDisplays -> {
                    Text("Có nhiều màn hình")
                    state.displays.forEach { display ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectDisplay(display.id) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = display.id == selectedDisplayId,
                                onClick = { onSelectDisplay(display.id) }
                            )
                            DisplaySummary(display)
                        }
                    }
                }
                is DexDisplayState.Error -> {
                    Text("Có lỗi")
                    Text(state.message)
                }
            }
            Text(
                launchDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onRefresh) {
                Text("Làm mới")
            }
            TextButton(onClick = onDiagnosticsClick) { Text("Chẩn đoán") }
        }
    }
}

@Composable
private fun DisplaySummary(display: ExternalDisplayInfo) {
    Column {
        Text(display.name, style = MaterialTheme.typography.titleSmall)
        Text(
            text = "${display.width} × ${display.height} • ID ${display.id}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun TargetDisplayDiagnosticPanel(
    zones: List<LayoutZone>,
    appAssignments: Map<String, ZoneAppAssignment>,
    onTestZone: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("Thử API màn hình đích", style = MaterialTheme.typography.titleSmall)
            zones.forEach { zone ->
                val assignment = appAssignments[zone.id] ?: return@forEach
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${zone.label}: ${assignment.appLabel}")
                    TextButton(onClick = { onTestZone(zone.id) }) {
                        Text("Thử API đích")
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorTopBar(
    isEditingWorkspace: Boolean,
    isSaveEnabled: Boolean,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Surface(shadowElevation = 4.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay lại"
                )
            }
            Text(
                text = if (isEditingWorkspace) {
                    "Chỉnh sửa workspace"
                } else {
                    "Trình chỉnh sửa bố cục"
                },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge
            )
            TextButton(onClick = onSaveClick, enabled = isSaveEnabled) {
                Text(text = "Lưu")
            }
        }
    }
}

@Composable
private fun LayoutPreview(
    zones: List<LayoutZone>,
    selectedTemplate: LayoutTemplate,
    leftRatio: Float,
    onLeftRatioChange: (Float) -> Unit,
    topRatio: Float,
    onTopRatioChange: (Float) -> Unit,
    appAssignments: Map<String, ZoneAppAssignment>,
    onZoneClick: (String) -> Unit,
    onRemoveAppFromZone: (String) -> Unit,
    onLaunchAppForZone: (String) -> Unit,
    canLaunchOnDex: Boolean,
    isEditingEnabled: Boolean,
    onLaunchOnDex: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = MaterialTheme.colorScheme.outline
    val shape = RoundedCornerShape(8.dp)

    Surface(
        modifier = modifier
            .border(1.dp, borderColor, shape)
            .clip(shape),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        if (zones.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Chưa có vùng nào")
            }
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val previewWidthPx = constraints.maxWidth.toFloat()
                val previewHeightPx = constraints.maxHeight.toFloat()

                zones.forEach { zone ->
                    PreviewZone(
                        zone = zone,
                        assignment = appAssignments[zone.id],
                        isEditingEnabled = isEditingEnabled,
                        onClick = { onZoneClick(zone.id) },
                        onRemoveClick = { onRemoveAppFromZone(zone.id) },
                        onLaunchClick = { onLaunchAppForZone(zone.id) },
                        isDexLaunchEnabled = canLaunchOnDex,
                        onLaunchOnDexClick = { onLaunchOnDex(zone.id) },
                        modifier = Modifier
                            .offset(
                                x = maxWidth * zone.x,
                                y = maxHeight * zone.y
                            )
                            .width(maxWidth * zone.width)
                            .height(maxHeight * zone.height)
                    )
                }

                if (selectedTemplate == LayoutTemplate.THREE_ZONES) {
                    val verticalDividerState = rememberDraggableState { deltaPx ->
                        val ratioDelta = deltaPx / previewWidthPx
                        val newRatio = (leftRatio + ratioDelta).coerceIn(0.4f, 0.8f)
                        onLeftRatioChange(newRatio)
                    }
                    val horizontalDividerState = rememberDraggableState { deltaPx ->
                        val ratioDelta = deltaPx / previewHeightPx
                        val newRatio = (topRatio + ratioDelta).coerceIn(0.25f, 0.75f)
                        onTopRatioChange(newRatio)
                    }

                    Box(
                        modifier = Modifier
                            .offset(x = maxWidth * leftRatio - 12.dp)
                            .width(24.dp)
                            .fillMaxHeight()
                            .zIndex(1f)
                            .draggable(
                                state = verticalDividerState,
                                orientation = Orientation.Horizontal,
                                enabled = isEditingEnabled
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .fillMaxHeight()
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .offset(
                                x = maxWidth * leftRatio,
                                y = maxHeight * topRatio - 12.dp
                            )
                            .width(maxWidth * (1f - leftRatio))
                            .height(24.dp)
                            .zIndex(1f)
                            .draggable(
                                state = horizontalDividerState,
                                orientation = Orientation.Vertical,
                                enabled = isEditingEnabled
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .padding(horizontal = 8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewZone(
    zone: LayoutZone,
    assignment: ZoneAppAssignment?,
    isEditingEnabled: Boolean,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onLaunchClick: () -> Unit,
    isDexLaunchEnabled: Boolean,
    onLaunchOnDexClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clickable(enabled = isEditingEnabled, onClick = onClick)
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outline
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (assignment == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = zone.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Chạm để chọn ứng dụng",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = assignment.appLabel, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = assignment.packageName,
                    style = MaterialTheme.typography.bodySmall
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onLaunchClick, enabled = isEditingEnabled) {
                        Text("Mở thử")
                    }
                    TextButton(
                        onClick = onLaunchOnDexClick,
                        enabled = isDexLaunchEnabled
                    ) {
                        Text("Mở trên DeX")
                    }
                    IconButton(onClick = onRemoveClick, enabled = isEditingEnabled) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Xóa ứng dụng khỏi ${zone.label}"
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LayoutEditorScreenPreview() {
    DexWorkspaceManagerTheme {
        LayoutEditorScreen(
            isEditingWorkspace = false,
            selectedTemplate = LayoutTemplate.THREE_ZONES,
            onTemplateSelected = {},
            onResetLayout = {},
            leftRatio = 0.65f,
            onLeftRatioChange = {},
            topRatio = 0.5f,
            onTopRatioChange = {},
            appAssignments = emptyMap(),
            launchDelayMs = 400L,
            onLaunchDelayChange = {},
            onMoveAssignmentUp = {},
            onMoveAssignmentDown = {},
            onZoneClick = {},
            onRemoveAppFromZone = {},
            onLaunchAppForZone = {},
            dexDisplayState = DexDisplayState.NotConnected,
            selectedExternalDisplayId = null,
            canLaunchOnDex = false,
            dexLaunchDescription = "Chưa phát hiện DeX",
            sourceDisplayId = null,
            detectedExternalDisplayIds = emptyList(),
            sourceWorkArea = null,
            lastDiagnosticDetails = null,
            onRefreshDexDisplay = {},
            onSelectExternalDisplay = {},
            onLaunchOnDex = {},
            workspaceLaunchProgress = WorkspaceLaunchProgress(0, 0),
            workspaceLaunchMessage = null,
            workspaceLaunchError = null,
            onLaunchWorkspace = {},
            onCancelWorkspaceLaunch = {},
            onWorkspaceLaunchMessageShown = {},
            onWorkspaceLaunchErrorShown = {},
            onTestTargetDisplay = {},
            onBackClick = {},
            workspaceName = "Workspace của tôi",
            onWorkspaceNameChange = {},
            isNameDialogVisible = false,
            isSaving = false,
            saveMessage = null,
            saveError = null,
            launchMessage = null,
            launchError = null,
            onSaveClick = {},
            onConfirmSave = {},
            onDismissSaveDialog = {},
            onSaveMessageShown = {},
            onSaveErrorShown = {},
            onLaunchMessageShown = {},
            onLaunchErrorShown = {}
        )
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun LayoutEditorUiState.toWorkspaceSnapshot(): Workspace = Workspace(
    id = workspaceId ?: 0L,
    name = workspaceName.ifBlank { "Workspace hiện tại" },
    template = selectedTemplate,
    leftRatio = leftRatio,
    topRatio = topRatio,
    createdAt = originalCreatedAt ?: 0L,
    updatedAt = 0L,
    appAssignments = appAssignments.values.map { assignment ->
        WorkspaceAppAssignment(
            zoneId = assignment.zoneId,
            packageName = assignment.packageName,
            activityName = assignment.activityName,
            appLabel = assignment.appLabel,
            launchOrder = assignment.launchOrder
        )
    },
    launchDelayMs = launchDelayMs
)

@Suppress("DEPRECATION")
private fun Activity.currentDisplayId(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    display?.displayId ?: windowManager.defaultDisplay.displayId
} else {
    windowManager.defaultDisplay.displayId
}

private fun DexDisplayState.externalDisplayIds(): List<Int> = when (this) {
    is DexDisplayState.Connected -> listOf(display.id)
    is DexDisplayState.MultipleDisplays -> displays.map(ExternalDisplayInfo::id)
    DexDisplayState.NotConnected,
    is DexDisplayState.Error -> emptyList()
}

private fun DexDisplayState.findDisplay(displayId: Int?): ExternalDisplayInfo? = when (this) {
    is DexDisplayState.Connected -> display.takeIf { it.id == displayId }
    is DexDisplayState.MultipleDisplays -> displays.firstOrNull { it.id == displayId }
    DexDisplayState.NotConnected,
    is DexDisplayState.Error -> null
}

private fun diagnosticDetails(
    workArea: DexWorkArea,
    bounds: LaunchBounds,
    assignment: ZoneAppAssignment
): String = "workArea=$workArea\nbounds=$bounds\n" +
    "package=${assignment.packageName}\nactivity=${assignment.activityName}"

private const val LAUNCH_LOG_TAG = "DexLaunch"
private const val MAX_DELAY_SLIDER_MS = 2_000L
private const val DELAY_SLIDER_STEP_MS = 100
private const val DELAY_SLIDER_STEPS = 19
