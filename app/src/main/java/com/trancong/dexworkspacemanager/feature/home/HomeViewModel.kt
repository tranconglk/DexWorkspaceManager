package com.trancong.dexworkspacemanager.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trancong.dexworkspacemanager.domain.model.Workspace
import com.trancong.dexworkspacemanager.domain.repository.WorkspaceRepository
import com.trancong.dexworkspacemanager.domain.repository.AppPreferencesRepository
import com.trancong.dexworkspacemanager.feature.layouteditor.WorkspaceLaunchFailure
import com.trancong.dexworkspacemanager.feature.layouteditor.WorkspaceLaunchProgress
import com.trancong.dexworkspacemanager.feature.layouteditor.WorkspaceLaunchResult
import com.trancong.dexworkspacemanager.platform.installedapps.WorkspaceAppsAvailabilityChecker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class HomeViewModel(
    private val workspaceRepository: WorkspaceRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val availabilityChecker: WorkspaceAppsAvailabilityChecker
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private var latestWorkspaces: List<Workspace> = emptyList()
    private var availabilityJob: Job? = null

    init {
        viewModelScope.launch {
            try {
                combine(
                    workspaceRepository.observeAll(),
                    appPreferencesRepository.quickLaunchWorkspaceId,
                    appPreferencesRepository.showDexPinHint
                ) { workspaces, quickLaunchId, showDexPinHint ->
                    Triple(workspaces, quickLaunchId, showDexPinHint)
                }.collect { (workspaces, savedQuickLaunchId, showDexPinHint) ->
                    latestWorkspaces = workspaces
                    _uiState.update { currentState ->
                        val allFavorites = workspaces
                            .filter(Workspace::isFavorite)
                            .sortedByDescending(Workspace::updatedAt)
                        val favorites = allFavorites.take(HOME_WORKSPACE_LIMIT)
                        val favoriteIds = allFavorites.mapTo(mutableSetOf(), Workspace::id)
                        val recent = workspaces
                            .filterNot { it.id in favoriteIds }
                            .sortedByDescending(Workspace::updatedAt)
                            .take(HOME_WORKSPACE_LIMIT)
                        val quickLaunchWorkspace = allFavorites.firstOrNull {
                            it.id == savedQuickLaunchId
                        } ?: allFavorites.firstOrNull()
                        currentState.copy(
                            favoriteWorkspaces = favorites,
                            recentWorkspaces = recent,
                            quickLaunchWorkspace = quickLaunchWorkspace,
                            showDexPinHint = showDexPinHint,
                            isLoading = false
                        )
                    }
                    val validQuickLaunchId = workspaces
                        .filter(Workspace::isFavorite)
                        .sortedByDescending(Workspace::updatedAt)
                        .firstOrNull { it.id == savedQuickLaunchId }
                        ?.id
                        ?: workspaces.filter(Workspace::isFavorite)
                            .maxByOrNull(Workspace::updatedAt)?.id
                    if (validQuickLaunchId != savedQuickLaunchId) {
                        appPreferencesRepository.setQuickLaunchWorkspaceId(validQuickLaunchId)
                    }
                    refreshAppAvailability()
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Không thể tải danh sách workspace. Vui lòng thử lại"
                    )
                }
            }
        }
    }

    fun refreshAppAvailability() {
        val snapshot = latestWorkspaces
        availabilityJob?.cancel()
        availabilityJob = viewModelScope.launch {
            val result = buildMap {
                snapshot.forEach { workspace ->
                    put(workspace.id, availabilityChecker.check(workspace.appAssignments))
                }
            }
            if (latestWorkspaces === snapshot) {
                _uiState.update { it.copy(appAvailabilityByWorkspaceId = result) }
            }
        }
    }

    fun startWorkspaceLaunch(workspaceId: Long, totalApps: Int) {
        if (_uiState.value.launchingWorkspaceId != null) return
        _uiState.update {
            it.copy(
                launchingWorkspaceId = workspaceId,
                launchProgress = WorkspaceLaunchProgress(
                    totalApps = totalApps.coerceAtLeast(0),
                    completedApps = 0,
                    isRunning = true
                ),
                message = null,
                error = null
            )
        }
    }

    fun updateWorkspaceLaunchProgress(
        workspaceId: Long,
        completedApps: Int,
        totalApps: Int,
        currentZoneId: String?,
        currentAppLabel: String?
    ) {
        if (_uiState.value.launchingWorkspaceId != workspaceId) return
        val safeTotal = totalApps.coerceAtLeast(0)
        _uiState.update {
            it.copy(
                launchProgress = it.launchProgress.copy(
                    totalApps = safeTotal,
                    completedApps = completedApps.coerceIn(0, safeTotal),
                    currentZoneId = currentZoneId,
                    currentAppLabel = currentAppLabel
                )
            )
        }
    }

    fun finishWorkspaceLaunch(workspaceId: Long, result: WorkspaceLaunchResult) {
        if (_uiState.value.launchingWorkspaceId != workspaceId &&
            result !is WorkspaceLaunchResult.NotRunningOnDex &&
            result !is WorkspaceLaunchResult.NoAssignedApps
        ) return

        _uiState.update { state ->
            val total = state.launchProgress.totalApps
            val stopped = state.launchProgress.copy(
                isRunning = false,
                currentZoneId = null,
                currentAppLabel = null
            )
            when (result) {
                is WorkspaceLaunchResult.Success -> state.copy(
                    launchingWorkspaceId = null,
                    launchProgress = stopped.copy(completedApps = total),
                    message = "Đã mở ${result.launchedCount} ứng dụng",
                    error = null
                )
                is WorkspaceLaunchResult.PartialSuccess -> state.copy(
                    launchingWorkspaceId = null,
                    launchProgress = stopped.copy(completedApps = total),
                    message = "Đã mở ${result.launchedCount}/$total ứng dụng",
                    error = "Không mở được: ${result.failures.failedAppSummary()}"
                )
                WorkspaceLaunchResult.NoAssignedApps -> state.copy(
                    launchingWorkspaceId = null,
                    launchProgress = stopped,
                    message = null,
                    error = "Workspace chưa được gán ứng dụng"
                )
                WorkspaceLaunchResult.NotRunningOnDex -> state.copy(
                    launchingWorkspaceId = null,
                    launchProgress = stopped,
                    message = null,
                    error = "DeX Workspace Manager chỉ hoạt động trên màn hình DeX."
                )
                is WorkspaceLaunchResult.Cancelled -> state.copy(
                    launchingWorkspaceId = null,
                    launchProgress = stopped,
                    message = "Đã dừng sau khi mở ${result.launchedCount} ứng dụng",
                    error = null
                )
            }
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }

    fun consumeError() = _uiState.update { it.copy(error = null) }

    fun requestRename(workspace: Workspace) {
        _uiState.update { it.copy(workspacePendingRename = workspace) }
    }

    fun cancelRename() {
        _uiState.update { it.copy(workspacePendingRename = null) }
    }

    fun confirmRename(newName: String) {
        val workspace = _uiState.value.workspacePendingRename ?: return
        viewModelScope.launch {
            runWorkspaceOperation(
                operation = { workspaceRepository.rename(workspace.id, newName) },
                successMessage = "Đã đổi tên workspace",
                errorMessage = "Không thể đổi tên workspace. Vui lòng thử lại"
            ) { it.copy(workspacePendingRename = null) }
        }
    }

    fun requestDuplicate(workspace: Workspace) {
        _uiState.update { it.copy(workspacePendingDuplicate = workspace) }
    }

    fun cancelDuplicate() {
        _uiState.update { it.copy(workspacePendingDuplicate = null) }
    }

    fun confirmDuplicate(newName: String) {
        val workspace = _uiState.value.workspacePendingDuplicate ?: return
        viewModelScope.launch {
            runWorkspaceOperation(
                operation = { workspaceRepository.duplicate(workspace.id, newName) },
                successMessage = "Đã sao chép workspace",
                errorMessage = "Không thể sao chép workspace. Vui lòng thử lại"
            ) { it.copy(workspacePendingDuplicate = null) }
        }
    }

    fun toggleFavorite(workspace: Workspace) {
        viewModelScope.launch {
            runWorkspaceOperation(
                operation = { workspaceRepository.setFavorite(workspace.id, !workspace.isFavorite) },
                successMessage = null,
                errorMessage = "Không thể cập nhật yêu thích. Vui lòng thử lại"
            ) { it }
        }
    }

    fun selectQuickLaunchWorkspace(workspaceId: Long) {
        if (_uiState.value.favoriteWorkspaces.none { it.id == workspaceId }) return
        viewModelScope.launch {
            appPreferencesRepository.setQuickLaunchWorkspaceId(workspaceId)
        }
    }

    fun dismissDexPinHint() {
        viewModelScope.launch {
            appPreferencesRepository.setShowDexPinHint(false)
        }
    }

    fun consumeOperationMessage() = consumeMessage()

    fun consumeOperationError() = consumeError()

    private suspend fun <T> runWorkspaceOperation(
        operation: suspend () -> T,
        successMessage: String?,
        errorMessage: String,
        onSuccess: (HomeUiState) -> HomeUiState
    ) {
        try {
            operation()
            _uiState.update {
                onSuccess(it).copy(message = successMessage, error = null)
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            _uiState.update { it.copy(error = errorMessage) }
        }
    }

    private fun List<WorkspaceLaunchFailure>.failedAppSummary(): String =
        map(WorkspaceLaunchFailure::appLabel).distinct().take(3).joinToString()

    private companion object {
        const val HOME_WORKSPACE_LIMIT = 4
    }
}
