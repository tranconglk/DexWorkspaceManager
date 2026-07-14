package com.trancong.dexworkspacemanager.feature.layouteditor

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
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
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.trancong.dexworkspacemanager.navigation.AppRoute
import com.trancong.dexworkspacemanager.platform.dex.DexDisplayState
import com.trancong.dexworkspacemanager.platform.dex.ExternalDisplayInfo
import com.trancong.dexworkspacemanager.platform.dex.DexLaunchMode
import com.trancong.dexworkspacemanager.platform.dex.DexWorkArea
import com.trancong.dexworkspacemanager.platform.dex.DexWindowLaunchStrategy
import com.trancong.dexworkspacemanager.platform.applauncher.AppLaunchResult
import com.trancong.dexworkspacemanager.platform.applauncher.LayoutBoundsCalculator
import com.trancong.dexworkspacemanager.platform.applauncher.KnownAppLaunchProfiles
import com.trancong.dexworkspacemanager.platform.applauncher.LaunchStrategy
import com.trancong.dexworkspacemanager.ui.theme.DexWorkspaceManagerTheme

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
    val manualLaunchStrategies = remember { mutableStateMapOf<String, LaunchStrategy>() }
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
    val selectedLaunchMode = when {
        hostActivity?.isRunningOnExternalDisplay() == true ->
            DexLaunchMode.CURRENT_DEX_ACTIVITY_NEW_TASK_BOUNDS
        uiState.selectedExternalDisplayId?.let { it in detectedExternalDisplayIds } == true ->
            DexLaunchMode.TARGET_DISPLAY_API
        else -> DexLaunchMode.DEFAULT_ACTIVITY
    }
    val dexLaunchDescription = when (selectedLaunchMode) {
        DexLaunchMode.CURRENT_DEX_ACTIVITY_NEW_TASK_BOUNDS ->
            "Đang chạy trực tiếp trên màn hình DeX"
        DexLaunchMode.TARGET_DISPLAY_API ->
            "Đang chạy trên điện thoại, sẽ gửi sang display DeX"
        DexLaunchMode.DEFAULT_ACTIVITY -> "Chưa phát hiện DeX"
    }
    val resolvedLaunchStrategies = uiState.appAssignments.values
        .map(ZoneAppAssignment::packageName)
        .distinct()
        .associateWith { packageName ->
            manualLaunchStrategies[packageName]
                ?: KnownAppLaunchProfiles.strategyFor(packageName)
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
            onZoneClick = onZoneClick,
            onRemoveAppFromZone = viewModel::removeAppFromZone,
            onLaunchAppForZone = viewModel::launchAssignedApp,
            dexDisplayState = uiState.dexDisplayState,
            selectedExternalDisplayId = uiState.selectedExternalDisplayId,
            canLaunchOnDex = selectedLaunchMode != DexLaunchMode.DEFAULT_ACTIVITY,
            dexLaunchDescription = dexLaunchDescription,
            sourceDisplayId = sourceDisplayId,
            onRefreshDexDisplay = viewModel::refreshDexDisplayState,
            onSelectExternalDisplay = viewModel::selectExternalDisplay,
            onLaunchAppOnDex = { zoneId ->
                val assignment = uiState.appAssignments[zoneId]
                val zone = LayoutTemplates.zonesFor(
                    uiState.selectedTemplate,
                    uiState.leftRatio,
                    uiState.topRatio
                ).firstOrNull { it.id == zoneId }
                val targetDisplay = uiState.dexDisplayState.findDisplay(
                    uiState.selectedExternalDisplayId
                )
                val workArea = when (selectedLaunchMode) {
                    DexLaunchMode.CURRENT_DEX_ACTIVITY_NEW_TASK_BOUNDS ->
                        hostActivity?.currentExternalDisplayWorkArea()
                    DexLaunchMode.TARGET_DISPLAY_API -> targetDisplay?.let {
                        DexWorkArea(it.width, it.height)
                    }
                    DexLaunchMode.DEFAULT_ACTIVITY -> null
                }
                when {
                    assignment == null -> viewModel.reportLaunchError(
                        "Vùng này chưa được gán ứng dụng"
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
                                LAUNCH_SELECTION_LOG_TAG,
                                "sourceActivityDisplayId=$sourceDisplayId, " +
                                    "detectedExternalDisplayIds=$detectedExternalDisplayIds, " +
                                    "selectedStrategy=$selectedLaunchMode, workArea=$workArea, " +
                                    "bounds=$bounds, packageName=${assignment.packageName}"
                            )
                            when (selectedLaunchMode) {
                                DexLaunchMode.CURRENT_DEX_ACTIVITY_NEW_TASK_BOUNDS ->
                                    application.container.foregroundAppLauncher
                                        .launchFromActivityWithBounds(
                                            hostActivity!!,
                                            assignment.packageName,
                                            assignment.activityName,
                                            bounds,
                                            LaunchStrategy.NEW_TASK_BOUNDS
                                        )
                                DexLaunchMode.TARGET_DISPLAY_API ->
                                    application.container.appLauncher.launchOnDisplayWithBounds(
                                        assignment.packageName,
                                        assignment.activityName,
                                        uiState.selectedExternalDisplayId!!,
                                        bounds
                                    )
                                DexLaunchMode.DEFAULT_ACTIVITY ->
                                    AppLaunchResult.DisplayNotAvailable
                            }
                        } catch (exception: IllegalArgumentException) {
                            AppLaunchResult.InvalidBounds
                        }
                        viewModel.handleBoundsLaunchResult(result)
                    }
                }
            },
            launchStrategies = resolvedLaunchStrategies,
            onLaunchStrategySelected = { packageName, strategy ->
                manualLaunchStrategies[packageName] = strategy
            },
            onDiagnosticLaunchInZone = { zoneId ->
                val assignment = uiState.appAssignments[zoneId]
                val activity = currentContext.findActivity()
                val zone = LayoutTemplates.zonesFor(
                    uiState.selectedTemplate,
                    uiState.leftRatio,
                    uiState.topRatio
                ).firstOrNull { it.id == zoneId }
                val workArea = activity?.currentExternalDisplayWorkArea()
                when {
                    assignment == null -> viewModel.reportLaunchError(
                        "Vùng này chưa được gán ứng dụng"
                    )
                    activity == null -> viewModel.reportLaunchError(
                        "Không tìm thấy cửa sổ hiện tại để mở ứng dụng"
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
                            val strategy = resolvedLaunchStrategies[assignment.packageName]
                                ?: LaunchStrategy.STANDARD_BOUNDS
                            Log.d(
                                DEX_BOUNDS_LOG_TAG,
                                "packageName=${assignment.packageName}, strategy=$strategy, " +
                                    "bounds=$bounds, activityName=${assignment.activityName}"
                            )
                            application.container.foregroundAppLauncher
                                .launchFromActivityWithBounds(
                                    activity,
                                    assignment.packageName,
                                    assignment.activityName,
                                    bounds,
                                    strategy
                                )
                        } catch (exception: IllegalArgumentException) {
                            AppLaunchResult.InvalidBounds
                        }
                        viewModel.handleBoundsLaunchResult(result)
                    }
                }
            },
            selectedTestStrategy = uiState.selectedTestStrategy,
            onSelectTestStrategy = viewModel::selectTestStrategy,
            onTestLaunchInZone = { zoneId ->
                val assignment = uiState.appAssignments[zoneId]
                val activity = currentContext.findActivity()
                val zone = LayoutTemplates.zonesFor(
                    uiState.selectedTemplate,
                    uiState.leftRatio,
                    uiState.topRatio
                ).firstOrNull { it.id == zoneId }
                val sourceDisplayId = activity?.currentDisplayId()
                val targetDisplayId = uiState.selectedExternalDisplayId
                val targetDisplay = uiState.dexDisplayState.findDisplay(targetDisplayId)
                val workArea = when (uiState.selectedTestStrategy) {
                    DexWindowLaunchStrategy.MODERN_DISPLAY_AND_BOUNDS -> targetDisplay?.let {
                        DexWorkArea(it.width, it.height)
                    }
                    DexWindowLaunchStrategy.LEGACY_NEW_TASK_AND_BOUNDS ->
                        activity?.currentExternalDisplayWorkArea()
                }
                when {
                    assignment == null || zone == null ->
                        viewModel.reportCompatibilityError("Vùng chưa có ứng dụng hợp lệ")
                    uiState.selectedTestStrategy ==
                        DexWindowLaunchStrategy.MODERN_DISPLAY_AND_BOUNDS &&
                        targetDisplayId == null ->
                        viewModel.reportCompatibilityError("Chưa xác định được display DeX")
                    uiState.selectedTestStrategy ==
                        DexWindowLaunchStrategy.LEGACY_NEW_TASK_AND_BOUNDS &&
                        (activity == null || sourceDisplayId == Display.DEFAULT_DISPLAY) ->
                        viewModel.reportCompatibilityError(
                            "Hãy mở DeX Workspace Manager trực tiếp trên màn hình DeX"
                        )
                    workArea == null ->
                        viewModel.reportCompatibilityError("Không xác định được vùng làm việc DeX")
                    else -> {
                        val result = try {
                            val bounds = LayoutBoundsCalculator.calculate(
                                zone,
                                workArea.width,
                                workArea.usableHeight,
                                marginPx = with(density) { 8.dp.roundToPx() }
                            )
                            val strategy = uiState.selectedTestStrategy
                            Log.d(
                                COMPATIBILITY_LOG_TAG,
                                "manufacturer=${Build.MANUFACTURER}, model=${Build.MODEL}, " +
                                    "sdk=${Build.VERSION.SDK_INT}, strategy=$strategy, " +
                                    "sourceDisplayId=$sourceDisplayId, " +
                                    "targetDisplayId=$targetDisplayId, " +
                                    "packageName=${assignment.packageName}, " +
                                    "activityName=${assignment.activityName}, bounds=$bounds"
                            )
                            when (strategy) {
                                DexWindowLaunchStrategy.MODERN_DISPLAY_AND_BOUNDS ->
                                    application.container.appLauncher.launchOnDisplayWithBounds(
                                        assignment.packageName,
                                        assignment.activityName,
                                        targetDisplayId!!,
                                        bounds
                                    )
                                DexWindowLaunchStrategy.LEGACY_NEW_TASK_AND_BOUNDS ->
                                    application.container.foregroundAppLauncher
                                        .launchFromActivityWithBounds(
                                            activity!!,
                                            assignment.packageName,
                                            assignment.activityName,
                                            bounds,
                                            LaunchStrategy.NEW_TASK_BOUNDS
                                        )
                            }
                        } catch (exception: IllegalArgumentException) {
                            AppLaunchResult.InvalidBounds
                        }
                        Log.d(COMPATIBILITY_LOG_TAG, "result=$result")
                        viewModel.handleCompatibilityResult(uiState.selectedTestStrategy, result)
                    }
                }
            },
            lastCompatibilityMessage = uiState.lastCompatibilityMessage,
            lastCompatibilityError = uiState.lastCompatibilityError,
            onCompatibilityMessageShown = viewModel::consumeCompatibilityMessage,
            onCompatibilityErrorShown = viewModel::consumeCompatibilityError,
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
    onZoneClick: (String) -> Unit,
    onRemoveAppFromZone: (String) -> Unit,
    onLaunchAppForZone: (String) -> Unit,
    dexDisplayState: DexDisplayState,
    selectedExternalDisplayId: Int?,
    canLaunchOnDex: Boolean,
    dexLaunchDescription: String,
    sourceDisplayId: Int?,
    onRefreshDexDisplay: () -> Unit,
    onSelectExternalDisplay: (Int) -> Unit,
    onLaunchAppOnDex: (String) -> Unit,
    launchStrategies: Map<String, LaunchStrategy>,
    onLaunchStrategySelected: (String, LaunchStrategy) -> Unit,
    onDiagnosticLaunchInZone: (String) -> Unit,
    selectedTestStrategy: DexWindowLaunchStrategy,
    onSelectTestStrategy: (DexWindowLaunchStrategy) -> Unit,
    onTestLaunchInZone: (String) -> Unit,
    lastCompatibilityMessage: String?,
    lastCompatibilityError: String?,
    onCompatibilityMessageShown: () -> Unit,
    onCompatibilityErrorShown: () -> Unit,
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
    LaunchedEffect(lastCompatibilityMessage) {
        lastCompatibilityMessage?.let {
            snackbarHostState.showSnackbar(it)
            onCompatibilityMessageShown()
        }
    }
    LaunchedEffect(lastCompatibilityError) {
        lastCompatibilityError?.let {
            snackbarHostState.showSnackbar(it)
            onCompatibilityErrorShown()
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
                        "SDK ${Build.VERSION.SDK_INT} • ${Build.MODEL}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Source display: $sourceDisplayId • Target display: " +
                            "$selectedExternalDisplayId",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (
                        dexDisplayState is DexDisplayState.Connected ||
                        dexDisplayState is DexDisplayState.MultipleDisplays
                    ) {
                        CompatibilityTestPanel(
                            strategy = selectedTestStrategy,
                            zones = zones.filter { appAssignments.containsKey(it.id) },
                            appAssignments = appAssignments,
                            launchStrategies = launchStrategies,
                            onSelectStrategy = onSelectTestStrategy,
                            onLaunchStrategySelected = onLaunchStrategySelected,
                            onDiagnosticLaunchInZone = onDiagnosticLaunchInZone,
                            onTestZone = onTestLaunchInZone
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
                    canLaunchOnDex = canLaunchOnDex,
                    onLaunchAppOnDex = onLaunchAppOnDex,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onTemplateSelected(LayoutTemplate.TWO_ZONES) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Mẫu 2 vùng")
                }
                Button(
                    onClick = { onTemplateSelected(LayoutTemplate.THREE_ZONES) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Mẫu 3 vùng")
                }
                Button(
                    onClick = onResetLayout,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Xóa bố cục")
                }
            }
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
private fun CompatibilityTestPanel(
    strategy: DexWindowLaunchStrategy,
    zones: List<LayoutZone>,
    appAssignments: Map<String, ZoneAppAssignment>,
    launchStrategies: Map<String, LaunchStrategy>,
    onSelectStrategy: (DexWindowLaunchStrategy) -> Unit,
    onLaunchStrategySelected: (String, LaunchStrategy) -> Unit,
    onDiagnosticLaunchInZone: (String) -> Unit,
    onTestZone: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("Kiểm tra tương thích", style = MaterialTheme.typography.titleSmall)
            DexWindowLaunchStrategy.entries.forEach { option ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = strategy == option,
                        onClick = { onSelectStrategy(option) }
                    )
                    Text(option.testLabel())
                }
            }
            Text("Đang chọn: ${strategy.testLabel()}", style = MaterialTheme.typography.bodySmall)
            zones.forEach { zone ->
                val assignment = appAssignments[zone.id] ?: return@forEach
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${zone.label}: ${assignment.appLabel}")
                        LaunchStrategyMenu(
                            selectedStrategy = assignmentStrategy(assignment, launchStrategies),
                            onStrategySelected = { selected ->
                                onLaunchStrategySelected(assignment.packageName, selected)
                            }
                        )
                    }
                    Column {
                        TextButton(onClick = { onDiagnosticLaunchInZone(zone.id) }) {
                            Text("Mở theo cấu hình")
                        }
                        TextButton(onClick = { onTestZone(zone.id) }) {
                            Text("Mở thử ${zone.label}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorTopBar(
    isEditingWorkspace: Boolean,
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
            TextButton(onClick = onSaveClick) {
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
    onLaunchAppOnDex: (String) -> Unit,
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
                        onClick = { onZoneClick(zone.id) },
                        onRemoveClick = { onRemoveAppFromZone(zone.id) },
                        onLaunchClick = { onLaunchAppForZone(zone.id) },
                        isDexLaunchEnabled = canLaunchOnDex,
                        onLaunchOnDexClick = { onLaunchAppOnDex(zone.id) },
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
                                orientation = Orientation.Horizontal
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
                                orientation = Orientation.Vertical
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
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onLaunchClick: () -> Unit,
    isDexLaunchEnabled: Boolean,
    onLaunchOnDexClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
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
                    TextButton(onClick = onLaunchClick) {
                        Text("Mở thử")
                    }
                    TextButton(
                        onClick = onLaunchOnDexClick,
                        enabled = isDexLaunchEnabled
                    ) {
                        Text("Mở trên DeX")
                    }
                    IconButton(onClick = onRemoveClick) {
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
            onZoneClick = {},
            onRemoveAppFromZone = {},
            onLaunchAppForZone = {},
            dexDisplayState = DexDisplayState.NotConnected,
            selectedExternalDisplayId = null,
            canLaunchOnDex = false,
            dexLaunchDescription = "Cơ chế mở: Mặc định",
            sourceDisplayId = null,
            onRefreshDexDisplay = {},
            onSelectExternalDisplay = {},
            onLaunchAppOnDex = {},
            launchStrategies = emptyMap(),
            onLaunchStrategySelected = { _, _ -> },
            onDiagnosticLaunchInZone = {},
            selectedTestStrategy = DexWindowLaunchStrategy.MODERN_DISPLAY_AND_BOUNDS,
            onSelectTestStrategy = {},
            onTestLaunchInZone = {},
            lastCompatibilityMessage = null,
            lastCompatibilityError = null,
            onCompatibilityMessageShown = {},
            onCompatibilityErrorShown = {},
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

@Suppress("DEPRECATION")
private fun Activity.currentExternalDisplayWorkArea(): DexWorkArea? {
    val display = windowManager.defaultDisplay
    if (display.displayId == Display.DEFAULT_DISPLAY) return null
    val metrics = DisplayMetrics()
    display.getRealMetrics(metrics)
    if (metrics.widthPixels <= 0 || metrics.heightPixels <= 0) return null
    return DexWorkArea(
        width = metrics.widthPixels,
        height = metrics.heightPixels,
        bottomInset = 0
    )
}

@Suppress("DEPRECATION")
private fun Activity.currentDisplayId(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    display?.displayId ?: windowManager.defaultDisplay.displayId
} else {
    windowManager.defaultDisplay.displayId
}

fun Activity.isRunningOnExternalDisplay(): Boolean {
    val displayId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        display?.displayId
    } else {
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.displayId
    }
    return displayId != null && displayId != Display.DEFAULT_DISPLAY
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

private fun DexWindowLaunchStrategy.testLabel(): String = when (this) {
    DexWindowLaunchStrategy.MODERN_DISPLAY_AND_BOUNDS -> "API màn hình đích"
    DexWindowLaunchStrategy.LEGACY_NEW_TASK_AND_BOUNDS -> "Task mới từ DeX"
}

private const val DEX_BOUNDS_LOG_TAG = "DexLaunchBounds"
private const val COMPATIBILITY_LOG_TAG = "DexCompatibilityTest"
private const val LAUNCH_SELECTION_LOG_TAG = "DexLaunchSelection"

private fun assignmentStrategy(
    assignment: ZoneAppAssignment?,
    launchStrategies: Map<String, LaunchStrategy>
): LaunchStrategy = assignment?.let { launchStrategies[it.packageName] }
    ?: LaunchStrategy.STANDARD_BOUNDS

@Composable
private fun LaunchStrategyMenu(
    selectedStrategy: LaunchStrategy,
    onStrategySelected: (LaunchStrategy) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(selectedStrategy.label())
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            MANUAL_LAUNCH_STRATEGIES.forEach { strategy ->
                DropdownMenuItem(
                    text = { Text(strategy.label()) },
                    onClick = {
                        onStrategySelected(strategy)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun LaunchStrategy.label(): String = when (this) {
    LaunchStrategy.STANDARD_BOUNDS -> "Mở chuẩn"
    LaunchStrategy.NEW_TASK_BOUNDS -> "Mở task mới"
    LaunchStrategy.CLEAR_TASK_BOUNDS -> "Xóa task và mở"
    LaunchStrategy.LEGACY_DEX_BOUNDS -> "Mở DeX tương thích"
}

private val MANUAL_LAUNCH_STRATEGIES = listOf(
    LaunchStrategy.STANDARD_BOUNDS,
    LaunchStrategy.NEW_TASK_BOUNDS,
    LaunchStrategy.LEGACY_DEX_BOUNDS
)
