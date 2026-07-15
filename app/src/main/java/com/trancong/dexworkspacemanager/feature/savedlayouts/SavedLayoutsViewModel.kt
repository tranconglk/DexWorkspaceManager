package com.trancong.dexworkspacemanager.feature.savedlayouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trancong.dexworkspacemanager.domain.model.Workspace
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

class SavedLayoutsViewModel(
    private val workspaceRepository: WorkspaceRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SavedLayoutsUiState())
    val uiState: StateFlow<SavedLayoutsUiState> = _uiState.asStateFlow()

    init {
        observeWorkspaces()
    }

    fun requestDelete(workspace: Workspace) {
        _uiState.update { currentState ->
            currentState.copy(workspacePendingDelete = workspace)
        }
    }

    fun cancelDelete() {
        _uiState.update { currentState ->
            currentState.copy(workspacePendingDelete = null)
        }
    }

    fun confirmDelete() {
        val workspace = _uiState.value.workspacePendingDelete ?: return

        viewModelScope.launch {
            try {
                workspaceRepository.deleteById(workspace.id)
                _uiState.update { currentState ->
                    currentState.copy(
                        workspacePendingDelete = null,
                        message = "Đã xóa workspace"
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        workspacePendingDelete = null,
                        error = "Không thể xóa workspace. Vui lòng thử lại"
                    )
                }
            }
        }
    }

    fun consumeMessage() {
        _uiState.update { currentState ->
            currentState.copy(message = null)
        }
    }

    fun consumeError() {
        _uiState.update { currentState ->
            currentState.copy(error = null)
        }
    }

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

    private suspend fun <T> runWorkspaceOperation(
        operation: suspend () -> T,
        successMessage: String?,
        errorMessage: String,
        onSuccess: (SavedLayoutsUiState) -> SavedLayoutsUiState
    ) {
        try {
            operation()
            _uiState.update { onSuccess(it).copy(message = successMessage, error = null) }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            _uiState.update { it.copy(error = errorMessage) }
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
                launchMessage = null,
                launchError = null
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
                    launchMessage = "Đã mở ${result.launchedCount} ứng dụng",
                    launchError = null
                )
                is WorkspaceLaunchResult.PartialSuccess -> state.copy(
                    launchingWorkspaceId = null,
                    launchProgress = stopped.copy(completedApps = total),
                    launchMessage = "Đã mở ${result.launchedCount}/$total ứng dụng",
                    launchError = "Không mở được: ${result.failures.failedAppSummary()}"
                )
                WorkspaceLaunchResult.NoAssignedApps -> state.copy(
                    launchingWorkspaceId = null,
                    launchProgress = stopped,
                    launchMessage = null,
                    launchError = "Workspace chưa được gán ứng dụng"
                )
                WorkspaceLaunchResult.NotRunningOnDex -> state.copy(
                    launchingWorkspaceId = null,
                    launchProgress = stopped,
                    launchMessage = null,
                    launchError = "Hãy mở DeX Workspace Manager trực tiếp trên màn hình DeX"
                )
                is WorkspaceLaunchResult.Cancelled -> state.copy(
                    launchingWorkspaceId = null,
                    launchProgress = stopped,
                    launchMessage = "Đã dừng sau khi mở ${result.launchedCount} ứng dụng",
                    launchError = null
                )
            }
        }
    }

    fun clearWorkspaceLaunchState() {
        _uiState.update {
            it.copy(
                launchingWorkspaceId = null,
                launchProgress = WorkspaceLaunchProgress(0, 0)
            )
        }
    }

    fun consumeLaunchMessage() {
        _uiState.update { it.copy(launchMessage = null) }
    }

    fun consumeLaunchError() {
        _uiState.update { it.copy(launchError = null) }
    }

    private fun observeWorkspaces() {
        viewModelScope.launch {
            try {
                workspaceRepository.observeAll().collect { workspaces ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            workspaces = workspaces.sortedWith(
                                compareByDescending<Workspace> { it.isFavorite }
                                    .thenByDescending { it.updatedAt }
                            ),
                            isLoading = false
                        )
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        error = "Không thể tải danh sách workspace. Vui lòng thử lại"
                    )
                }
            }
        }
    }
}

private fun List<WorkspaceLaunchFailure>.failedAppSummary(): String =
    map(WorkspaceLaunchFailure::appLabel).distinct().take(3).joinToString()
