package com.trancong.dexworkspacemanager.feature.layouteditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trancong.dexworkspacemanager.domain.model.Workspace
import com.trancong.dexworkspacemanager.domain.repository.WorkspaceRepository
import com.trancong.dexworkspacemanager.platform.applauncher.AppLaunchResult
import com.trancong.dexworkspacemanager.platform.applauncher.AppLauncher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LayoutEditorViewModel(
    private val workspaceRepository: WorkspaceRepository,
    private val appLauncher: AppLauncher,
    workspaceId: Long?
) : ViewModel() {
    private val _uiState = MutableStateFlow(LayoutEditorUiState())
    val uiState: StateFlow<LayoutEditorUiState> = _uiState.asStateFlow()

    init {
        if (workspaceId != null) {
            loadWorkspace(workspaceId)
        }
    }

    fun selectTemplate(template: LayoutTemplate) {
        _uiState.update { currentState ->
            currentState.copy(selectedTemplate = template)
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
            currentState.copy(selectedTemplate = LayoutTemplate.EMPTY)
        }
    }

    fun assignAppToZone(
        zoneId: String,
        packageName: String,
        activityName: String,
        appLabel: String
    ) {
        val assignment = ZoneAppAssignment(zoneId, packageName, activityName, appLabel)
        _uiState.update { currentState ->
            currentState.copy(
                appAssignments = currentState.appAssignments + (zoneId to assignment)
            )
        }
    }

    fun removeAppFromZone(zoneId: String) {
        _uiState.update { currentState ->
            currentState.copy(appAssignments = currentState.appAssignments - zoneId)
        }
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
                is AppLaunchResult.UnknownError -> currentState.copy(
                    launchMessage = null,
                    launchError = "Không thể mở ứng dụng. Vui lòng thử lại"
                )
            }
        }
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
            updatedAt = timestamp
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
                            isLoadingWorkspace = false,
                            loadError = null
                        )
                    }
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
}
