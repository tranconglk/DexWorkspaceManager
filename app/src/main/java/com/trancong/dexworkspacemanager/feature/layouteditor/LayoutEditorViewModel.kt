package com.trancong.dexworkspacemanager.feature.layouteditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trancong.dexworkspacemanager.data.mapper.toDomain
import com.trancong.dexworkspacemanager.data.mapper.toEditorAssignment
import com.trancong.dexworkspacemanager.domain.model.Workspace
import com.trancong.dexworkspacemanager.domain.repository.WorkspaceRepository
import com.trancong.dexworkspacemanager.platform.applauncher.AppLaunchResult
import com.trancong.dexworkspacemanager.platform.applauncher.AppLauncher
import com.trancong.dexworkspacemanager.platform.dex.DexDisplayProvider
import com.trancong.dexworkspacemanager.platform.dex.DexDisplayState
import com.trancong.dexworkspacemanager.platform.installedapps.InstalledAppAvailability
import com.trancong.dexworkspacemanager.platform.installedapps.WorkspaceAppsAvailabilityChecker
import com.trancong.dexworkspacemanager.platform.installedapps.PackageChangeMonitor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class LayoutEditorViewModel(
    private val workspaceRepository: WorkspaceRepository,
    private val appLauncher: AppLauncher,
    private val dexDisplayProvider: DexDisplayProvider,
    private val availabilityChecker: WorkspaceAppsAvailabilityChecker,
    packageChangeMonitor: PackageChangeMonitor,
    workspaceId: Long?
) : ViewModel() {
    private val _uiState = MutableStateFlow(LayoutEditorUiState())
    val uiState: StateFlow<LayoutEditorUiState> = _uiState.asStateFlow()
    private var availabilityJob: Job? = null
    private var packageRefreshJob: Job? = null

    init {
        viewModelScope.launch {
            packageChangeMonitor.events.collect { event ->
                if (_uiState.value.appAssignments.values.any {
                        it.packageName == event.packageName
                    }
                ) {
                    packageRefreshJob?.cancel()
                    packageRefreshJob = viewModelScope.launch {
                        delay(PACKAGE_CHANGE_DEBOUNCE_MS)
                        refreshAppAvailability()
                    }
                }
            }
        }
        refreshDexDisplayState()
        if (workspaceId != null) {
            loadWorkspace(workspaceId)
        }
    }

    fun selectTemplate(template: LayoutTemplate) {
        _uiState.update { currentState ->
            val validZoneIds = LayoutTemplates.zonesFor(
                template = template,
                leftRatio = currentState.leftRatio,
                topRatio = currentState.topRatio
            ).mapTo(mutableSetOf(), LayoutZone::id)
            currentState.copy(
                selectedTemplate = template,
                appAssignments = currentState.appAssignments
                    .filterKeys(validZoneIds::contains)
                    .normalizedLaunchOrder(),
                appAvailabilityByZoneId = currentState.appAvailabilityByZoneId
                    .filterKeys(validZoneIds::contains)
            )
        }
    }

    fun updateLeftRatio(ratio: Float) {
        _uiState.update { currentState ->
            currentState.copy(leftRatio = ratio.coerceIn(0.4f, 0.8f))
        }
    }

    fun updateTopRatio(ratio: Float) {
        _uiState.update { currentState ->
            currentState.copy(topRatio = ratio.coerceIn(0.25f, 0.75f))
        }
    }

    fun resetLayout() {
        _uiState.update { currentState ->
            currentState.copy(
                selectedTemplate = LayoutTemplate.EMPTY,
                appAssignments = emptyMap(),
                appAvailabilityByZoneId = emptyMap()
            )
        }
    }

    fun assignAppToZone(
        zoneId: String,
        packageName: String,
        activityName: String,
        appLabel: String
    ) {
        _uiState.update { currentState ->
            val launchOrder = currentState.appAssignments[zoneId]?.launchOrder
                ?: LaunchOrderNormalizer.nextOrder(currentState.appAssignments.values)
            val assignment = ZoneAppAssignment(
                zoneId = zoneId,
                packageName = packageName,
                activityName = activityName,
                appLabel = appLabel,
                launchOrder = launchOrder
            )
            currentState.copy(
                appAssignments = currentState.appAssignments + (zoneId to assignment),
                appAvailabilityByZoneId = currentState.appAvailabilityByZoneId +
                    (zoneId to InstalledAppAvailability.Available)
            )
        }
    }

    fun removeAppFromZone(zoneId: String) {
        _uiState.update { currentState ->
            currentState.copy(
                appAssignments = (currentState.appAssignments - zoneId).normalizedLaunchOrder(),
                appAvailabilityByZoneId = currentState.appAvailabilityByZoneId - zoneId
            )
        }
    }

    fun refreshAppAvailability() {
        val snapshot = _uiState.value.appAssignments
        availabilityJob?.cancel()
        availabilityJob = viewModelScope.launch {
            val result = availabilityChecker.check(snapshot.values.map { it.toDomain() })
            if (_uiState.value.appAssignments == snapshot) {
                _uiState.update { it.copy(appAvailabilityByZoneId = result) }
            }
        }
    }

    fun updateLaunchDelay(delayMs: Long) {
        _uiState.update { it.copy(launchDelayMs = delayMs.coerceIn(0L, 5_000L)) }
    }

    fun moveAssignmentUp(zoneId: String) {
        moveAssignment(zoneId, direction = -1)
    }

    fun moveAssignmentDown(zoneId: String) {
        moveAssignment(zoneId, direction = 1)
    }

    fun launchAssignedApp(zoneId: String) {
        val assignment = _uiState.value.appAssignments[zoneId]
        if (assignment == null) {
            _uiState.update {
                it.copy(
                    launchMessage = null,
                    launchError = "Vùng này chưa được gán ứng dụng"
                )
            }
            return
        }

        val result = appLauncher.launch(
            packageName = assignment.packageName,
            activityName = assignment.activityName
        )
        _uiState.update { currentState ->
            when (result) {
                AppLaunchResult.Success -> currentState.copy(
                    launchMessage = "Đã mở ${assignment.appLabel}",
                    launchError = null
                )
                AppLaunchResult.AppNotFound -> currentState.copy(
                    launchMessage = null,
                    launchError = "Ứng dụng không còn được cài đặt"
                )
                AppLaunchResult.ActivityNotFound -> currentState.copy(
                    launchMessage = null,
                    launchError = "Không tìm thấy màn hình khởi chạy của ứng dụng"
                )
                AppLaunchResult.SecurityError -> currentState.copy(
                    launchMessage = null,
                    launchError = "Không có quyền mở ứng dụng này"
                )
                AppLaunchResult.DisplayNotAvailable -> currentState.copy(
                    launchMessage = null,
                    launchError = "Màn hình ngoài không còn khả dụng"
                )
                AppLaunchResult.LaunchNotAllowedOnDisplay -> currentState.copy(
                    launchMessage = null,
                    launchError = "Hệ thống không cho phép mở ứng dụng trên màn hình này"
                )
                AppLaunchResult.MultiDisplayNotSupported -> currentState.copy(
                    launchMessage = null,
                    launchError = "Thiết bị hoặc ROM không hỗ trợ mở Activity trên màn hình phụ"
                )
                AppLaunchResult.BoundsNotSupported -> currentState.copy(
                    launchMessage = null,
                    launchError = "ROM không hỗ trợ đặt vị trí cửa sổ khi mở"
                )
                AppLaunchResult.InvalidBounds -> currentState.copy(
                    launchMessage = null,
                    launchError = "Kích thước vùng không hợp lệ"
                )
                is AppLaunchResult.UnknownError -> currentState.copy(
                    launchMessage = null,
                    launchError = "Không thể mở ứng dụng. Vui lòng thử lại"
                )
            }
        }
    }

    fun refreshDexDisplayState() {
        val state = dexDisplayProvider.getCurrentState()
        _uiState.update { currentState ->
            val selectedDisplayId = when (state) {
                is DexDisplayState.Connected -> state.display.id
                is DexDisplayState.MultipleDisplays -> {
                    val likelyDisplays = state.displays.filter { it.isLikelyDexDisplay }
                    when {
                        likelyDisplays.size == 1 -> likelyDisplays.first().id
                        state.displays.any { it.id == currentState.selectedExternalDisplayId } ->
                            currentState.selectedExternalDisplayId
                        else -> null
                    }
                }
                DexDisplayState.NotConnected,
                is DexDisplayState.Error -> null
            }
            currentState.copy(
                dexDisplayState = state,
                selectedExternalDisplayId = selectedDisplayId
            )
        }
    }

    fun selectExternalDisplay(displayId: Int) {
        _uiState.update { currentState ->
            val availableIds = when (val state = currentState.dexDisplayState) {
                is DexDisplayState.Connected -> setOf(state.display.id)
                is DexDisplayState.MultipleDisplays -> state.displays.map { it.id }.toSet()
                DexDisplayState.NotConnected,
                is DexDisplayState.Error -> emptySet()
            }
            if (displayId in availableIds) {
                currentState.copy(selectedExternalDisplayId = displayId)
            } else {
                currentState
            }
        }
    }

    fun handleDexLaunchResult(result: AppLaunchResult) {
        _uiState.update { latestState ->
            when (result) {
                AppLaunchResult.Success -> latestState.copy(
                    launchMessage = "Đã gửi ứng dụng lên màn hình ngoài",
                    launchError = null
                )
                AppLaunchResult.DisplayNotAvailable -> latestState.copy(
                    launchMessage = null,
                    launchError = "Màn hình ngoài không còn khả dụng"
                )
                AppLaunchResult.LaunchNotAllowedOnDisplay -> latestState.copy(
                    launchMessage = null,
                    launchError = "Hệ thống không cho phép mở ứng dụng trên màn hình này"
                )
                AppLaunchResult.MultiDisplayNotSupported -> latestState.copy(
                    launchMessage = null,
                    launchError = "Thiết bị hoặc ROM không hỗ trợ mở Activity trên màn hình phụ"
                )
                AppLaunchResult.BoundsNotSupported -> latestState.copy(
                    launchMessage = null,
                    launchError = "ROM không hỗ trợ đặt vị trí cửa sổ khi mở"
                )
                AppLaunchResult.InvalidBounds -> latestState.copy(
                    launchMessage = null,
                    launchError = "Kích thước vùng không hợp lệ"
                )
                AppLaunchResult.AppNotFound -> latestState.copy(
                    launchMessage = null,
                    launchError = "Ứng dụng không còn được cài đặt"
                )
                AppLaunchResult.ActivityNotFound -> latestState.copy(
                    launchMessage = null,
                    launchError = "Không tìm thấy màn hình khởi chạy của ứng dụng"
                )
                AppLaunchResult.SecurityError -> latestState.copy(
                    launchMessage = null,
                    launchError = "Không có quyền mở ứng dụng này"
                )
                is AppLaunchResult.UnknownError -> latestState.copy(
                    launchMessage = null,
                    launchError = "Không thể mở ứng dụng. Vui lòng thử lại"
                )
            }
        }
    }

    fun handleBoundsLaunchResult(result: AppLaunchResult) {
        if (result == AppLaunchResult.Success) {
            _uiState.update {
                it.copy(
                    launchMessage = "Đã yêu cầu mở ứng dụng trong vùng",
                    launchError = null
                )
            }
        } else {
            handleDexLaunchResult(result)
        }
    }

    fun reportLaunchError(message: String) {
        _uiState.update { it.copy(launchMessage = null, launchError = message) }
    }

    fun startWorkspaceLaunch(totalApps: Int) {
        _uiState.update {
            it.copy(
                workspaceLaunchProgress = WorkspaceLaunchProgress(
                    totalApps = totalApps.coerceAtLeast(0),
                    completedApps = 0,
                    isRunning = true
                ),
                workspaceLaunchMessage = null,
                workspaceLaunchError = null
            )
        }
    }

    fun updateWorkspaceLaunchProgress(
        completedApps: Int,
        totalApps: Int,
        currentZoneId: String?,
        currentAppLabel: String?
    ) {
        val safeTotal = totalApps.coerceAtLeast(0)
        _uiState.update {
            it.copy(
                workspaceLaunchProgress = it.workspaceLaunchProgress.copy(
                    totalApps = safeTotal,
                    completedApps = completedApps.coerceIn(0, safeTotal),
                    currentZoneId = currentZoneId,
                    currentAppLabel = currentAppLabel
                )
            )
        }
    }

    fun finishWorkspaceLaunch(result: WorkspaceLaunchResult) {
        _uiState.update { currentState ->
            val stoppedProgress = currentState.workspaceLaunchProgress.copy(
                isRunning = false,
                currentZoneId = null,
                currentAppLabel = null
            )
            when (result) {
                is WorkspaceLaunchResult.Success -> currentState.copy(
                    workspaceLaunchProgress = stoppedProgress.copy(
                        completedApps = stoppedProgress.totalApps
                    ),
                    workspaceLaunchMessage = "Đã mở ${result.launchedCount} ứng dụng",
                    workspaceLaunchError = null
                )
                is WorkspaceLaunchResult.PartialSuccess -> {
                    val failedApps = result.failures
                        .map(WorkspaceLaunchFailure::appLabel)
                        .distinct()
                        .take(MAX_FAILURE_LABELS_IN_MESSAGE)
                        .joinToString()
                    currentState.copy(
                        workspaceLaunchProgress = stoppedProgress.copy(
                            completedApps = stoppedProgress.totalApps
                        ),
                        workspaceLaunchMessage =
                            "Đã mở ${result.launchedCount}/${stoppedProgress.totalApps} ứng dụng",
                        workspaceLaunchError = "Không mở được: $failedApps"
                    )
                }
                WorkspaceLaunchResult.NoAssignedApps -> currentState.copy(
                    workspaceLaunchProgress = stoppedProgress,
                    workspaceLaunchMessage = null,
                    workspaceLaunchError = "Workspace chưa được gán ứng dụng"
                )
                WorkspaceLaunchResult.NotRunningOnDex -> currentState.copy(
                    workspaceLaunchProgress = stoppedProgress,
                    workspaceLaunchMessage = null,
                    workspaceLaunchError =
                        "Hãy mở DeX Workspace Manager trực tiếp trên màn hình DeX"
                )
                is WorkspaceLaunchResult.Cancelled -> currentState.copy(
                    workspaceLaunchProgress = stoppedProgress,
                    workspaceLaunchMessage =
                        "Đã dừng sau khi mở ${result.launchedCount} ứng dụng",
                    workspaceLaunchError = null
                )
            }
        }
    }

    fun cancelWorkspaceLaunchState() {
        _uiState.update {
            it.copy(
                workspaceLaunchProgress = it.workspaceLaunchProgress.copy(
                    isRunning = false,
                    currentZoneId = null,
                    currentAppLabel = null
                )
            )
        }
    }

    fun consumeWorkspaceLaunchMessage() {
        _uiState.update { it.copy(workspaceLaunchMessage = null) }
    }

    fun consumeWorkspaceLaunchError() {
        _uiState.update { it.copy(workspaceLaunchError = null) }
    }

    fun consumeLaunchMessage() {
        _uiState.update { it.copy(launchMessage = null) }
    }

    fun consumeLaunchError() {
        _uiState.update { it.copy(launchError = null) }
    }

    fun showSaveDialog() {
        _uiState.update { currentState ->
            currentState.copy(isNameDialogVisible = true)
        }
    }

    fun hideSaveDialog() {
        _uiState.update { currentState ->
            currentState.copy(isNameDialogVisible = false)
        }
    }

    fun updateWorkspaceName(name: String) {
        _uiState.update { currentState ->
            currentState.copy(workspaceName = name)
        }
    }

    fun saveWorkspace() {
        val currentState = _uiState.value
        if (currentState.isSaving) return

        val workspaceName = currentState.workspaceName.trim()
        if (workspaceName.isBlank()) {
            _uiState.update { it.copy(saveError = "Tên workspace không được để trống") }
            return
        }
        if (currentState.selectedTemplate == LayoutTemplate.EMPTY) {
            _uiState.update { it.copy(saveError = "Không thể lưu bố cục trống") }
            return
        }

        val isNewWorkspace = currentState.workspaceId == null
        val timestamp = System.currentTimeMillis()
        val createdAt = if (isNewWorkspace) {
            timestamp
        } else {
            currentState.originalCreatedAt ?: timestamp
        }
        val workspace = Workspace(
            id = currentState.workspaceId ?: 0,
            name = workspaceName,
            template = currentState.selectedTemplate,
            leftRatio = currentState.leftRatio,
            topRatio = currentState.topRatio,
            createdAt = createdAt,
            updatedAt = timestamp,
            appAssignments = currentState.appAssignments.values.map { it.toDomain() },
            launchDelayMs = currentState.launchDelayMs
        )

        _uiState.update {
            it.copy(
                workspaceName = workspaceName,
                isSaving = true,
                saveMessage = null,
                saveError = null
            )
        }

        viewModelScope.launch {
            try {
                val savedWorkspaceId = workspaceRepository.save(workspace)
                _uiState.update { latestState ->
                    latestState.copy(
                        workspaceId = if (isNewWorkspace) {
                            savedWorkspaceId
                        } else {
                            latestState.workspaceId
                        },
                        originalCreatedAt = if (isNewWorkspace) {
                            createdAt
                        } else {
                            latestState.originalCreatedAt
                        },
                        isSaving = false,
                        isNameDialogVisible = false,
                        saveMessage = if (isNewWorkspace) {
                            "Đã lưu workspace"
                        } else {
                            "Đã cập nhật workspace"
                        }
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveError = "Không thể lưu workspace. Vui lòng thử lại"
                    )
                }
            }
        }
    }

    fun consumeSaveMessage() {
        _uiState.update { currentState ->
            currentState.copy(saveMessage = null)
        }
    }

    fun consumeSaveError() {
        _uiState.update { currentState ->
            currentState.copy(saveError = null)
        }
    }

    private fun loadWorkspace(id: Long) {
        _uiState.update { currentState ->
            currentState.copy(isLoadingWorkspace = true, loadError = null)
        }

        viewModelScope.launch {
            try {
                val workspace = workspaceRepository.getById(id)
                if (workspace == null) {
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoadingWorkspace = false,
                            loadError = "Không tìm thấy workspace"
                        )
                    }
                } else {
                    _uiState.update { currentState ->
                        currentState.copy(
                            workspaceId = workspace.id,
                            originalCreatedAt = workspace.createdAt,
                            workspaceName = workspace.name,
                            selectedTemplate = workspace.template,
                            leftRatio = workspace.leftRatio,
                            topRatio = workspace.topRatio,
                            launchDelayMs = workspace.launchDelayMs,
                            appAssignments = workspace.appAssignments
                                .map { it.toEditorAssignment() }
                                .associateBy(ZoneAppAssignment::zoneId),
                            isLoadingWorkspace = false,
                            loadError = null
                        )
                    }
                    refreshAppAvailability()
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoadingWorkspace = false,
                        loadError = "Không thể tải workspace. Vui lòng thử lại"
                    )
                }
            }
        }
    }

    private companion object {
        const val MAX_FAILURE_LABELS_IN_MESSAGE = 3
        const val PACKAGE_CHANGE_DEBOUNCE_MS = 200L
    }

    private fun moveAssignment(zoneId: String, direction: Int) {
        _uiState.update { currentState ->
            val ordered = currentState.appAssignments.values
                .sortedWith(assignmentOrderComparator)
                .toMutableList()
            val currentIndex = ordered.indexOfFirst { it.zoneId == zoneId }
            val targetIndex = currentIndex + direction
            if (currentIndex == -1 || targetIndex !in ordered.indices) {
                currentState
            } else {
                val moved = ordered.removeAt(currentIndex)
                ordered.add(targetIndex, moved)
                currentState.copy(
                    appAssignments = ordered.mapIndexed { index, assignment ->
                        assignment.copy(launchOrder = index)
                    }.associateBy(ZoneAppAssignment::zoneId)
                )
            }
        }
    }
}

private val assignmentOrderComparator = compareBy<ZoneAppAssignment> { it.launchOrder }
    .thenBy { it.zoneId }

private fun Map<String, ZoneAppAssignment>.normalizedLaunchOrder():
    Map<String, ZoneAppAssignment> = LaunchOrderNormalizer.normalize(values)
