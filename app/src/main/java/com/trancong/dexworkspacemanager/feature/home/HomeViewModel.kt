package com.trancong.dexworkspacemanager.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trancong.dexworkspacemanager.domain.repository.WorkspaceRepository
import com.trancong.dexworkspacemanager.feature.layouteditor.WorkspaceLaunchFailure
import com.trancong.dexworkspacemanager.feature.layouteditor.WorkspaceLaunchProgress
import com.trancong.dexworkspacemanager.feature.layouteditor.WorkspaceLaunchResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val workspaceRepository: WorkspaceRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                workspaceRepository.observeAll().collect { workspaces ->
                    _uiState.update {
                        it.copy(
                            workspaces = workspaces.sortedByDescending { workspace ->
                                workspace.updatedAt
                            }.take(RECENT_WORKSPACE_LIMIT),
                            isLoading = false
                        )
                    }
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

    private fun List<WorkspaceLaunchFailure>.failedAppSummary(): String =
        map(WorkspaceLaunchFailure::appLabel).distinct().take(3).joinToString()

    private companion object {
        const val RECENT_WORKSPACE_LIMIT = 4
    }
}
