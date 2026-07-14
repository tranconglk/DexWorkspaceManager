package com.trancong.dexworkspacemanager.feature.savedlayouts

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

    private fun observeWorkspaces() {
        viewModelScope.launch {
            try {
                workspaceRepository.observeAll().collect { workspaces ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            workspaces = workspaces,
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
