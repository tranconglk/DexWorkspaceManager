package com.trancong.dexworkspacemanager.feature.layouteditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trancong.dexworkspacemanager.domain.model.Workspace
import com.trancong.dexworkspacemanager.domain.repository.WorkspaceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LayoutEditorViewModel(
    private val workspaceRepository: WorkspaceRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(LayoutEditorUiState())
    val uiState: StateFlow<LayoutEditorUiState> = _uiState.asStateFlow()

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

        val timestamp = System.currentTimeMillis()
        val workspace = Workspace(
            id = 0,
            name = workspaceName,
            template = currentState.selectedTemplate,
            leftRatio = currentState.leftRatio,
            topRatio = currentState.topRatio,
            createdAt = timestamp,
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
                workspaceRepository.save(workspace)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isNameDialogVisible = false,
                        saveMessage = "Đã lưu workspace"
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
}
