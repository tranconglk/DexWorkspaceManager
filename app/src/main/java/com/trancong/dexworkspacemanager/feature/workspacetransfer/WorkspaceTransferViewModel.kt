package com.trancong.dexworkspacemanager.feature.workspacetransfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trancong.dexworkspacemanager.domain.repository.WorkspaceRepository
import com.trancong.dexworkspacemanager.platform.transfer.WorkspaceTransferDirectory
import com.trancong.dexworkspacemanager.platform.transfer.WorkspaceTransferResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WorkspaceTransferViewModel(
    private val repository: WorkspaceRepository,
    private val transferDirectory: WorkspaceTransferDirectory
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkspaceTransferUiState())
    val uiState: StateFlow<WorkspaceTransferUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                val files = transferDirectory.listImportFiles()
                _uiState.update { it.copy(importFiles = files, isLoading = false) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Không thể đọc thư mục imports") }
            }
        }
    }

    fun selectFile(fileName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedFileName = fileName, previewWorkspace = null) }
            when (val result = transferDirectory.readImport(fileName)) {
                is WorkspaceTransferResult.Success -> _uiState.update {
                    it.copy(previewWorkspace = result.workspace, error = null)
                }
                is WorkspaceTransferResult.InvalidData -> reportError(result.message)
                is WorkspaceTransferResult.UnsupportedVersion ->
                    reportError("Không hỗ trợ schemaVersion ${result.version}")
                is WorkspaceTransferResult.FileError ->
                    reportError(result.message ?: "Không thể đọc file")
                WorkspaceTransferResult.Cancelled -> cancelPreview()
            }
        }
    }

    fun confirmImport() {
        val workspace = _uiState.value.previewWorkspace ?: return
        viewModelScope.launch {
            try {
                repository.save(workspace.copy(id = 0, isFavorite = false))
                _uiState.update {
                    it.copy(
                        selectedFileName = null,
                        previewWorkspace = null,
                        message = "Đã import workspace",
                        error = null
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                reportError("Không thể import workspace. Vui lòng thử lại")
            }
        }
    }

    fun cancelPreview() {
        _uiState.update { it.copy(selectedFileName = null, previewWorkspace = null) }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }
    fun consumeError() = _uiState.update { it.copy(error = null) }

    private fun reportError(message: String) {
        _uiState.update { it.copy(error = message, previewWorkspace = null) }
    }
}
