package com.trancong.dexworkspacemanager.feature.workspacetransfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trancong.dexworkspacemanager.domain.repository.WorkspaceRepository
import com.trancong.dexworkspacemanager.platform.transfer.WorkspaceTransferDirectory
import com.trancong.dexworkspacemanager.platform.transfer.WorkspaceTransferResult
import com.trancong.dexworkspacemanager.platform.transfer.WorkspaceBackupManager
import com.trancong.dexworkspacemanager.platform.transfer.WorkspaceBackupResult
import com.trancong.dexworkspacemanager.platform.transfer.WorkspaceRestoreMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WorkspaceTransferViewModel(
    private val repository: WorkspaceRepository,
    private val transferDirectory: WorkspaceTransferDirectory,
    private val backupManager: WorkspaceBackupManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkspaceTransferUiState())
    val uiState: StateFlow<WorkspaceTransferUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                val files = transferDirectory.listImportFiles()
                val backupFiles = backupManager.listBackupFiles()
                _uiState.update {
                    it.copy(importFiles = files, backupFiles = backupFiles, isLoading = false)
                }
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

    fun createBackup() {
        if (_uiState.value.isCreatingBackup || _uiState.value.isRestoring) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingBackup = true, error = null) }
            when (val result = backupManager.createBackup()) {
                is WorkspaceBackupResult.BackupSuccess -> _uiState.update {
                    it.copy(
                        isCreatingBackup = false,
                        message = "Đã backup ${result.workspaceCount} workspace: ${result.fileName}"
                    )
                }
                else -> _uiState.update {
                    it.copy(isCreatingBackup = false, error = result.errorMessage())
                }
            }
        }
    }

    fun selectBackup(fileName: String) {
        viewModelScope.launch {
            backupManager.previewBackup(fileName).fold(
                onSuccess = { preview ->
                    _uiState.update { it.copy(selectedBackupPreview = preview, error = null) }
                },
                onFailure = { exception ->
                    reportError(exception.message ?: "Backup không hợp lệ")
                }
            )
        }
    }

    fun selectRestoreMode(mode: WorkspaceRestoreMode) {
        _uiState.update { it.copy(selectedRestoreMode = mode) }
    }

    fun confirmRestore() {
        val preview = _uiState.value.selectedBackupPreview ?: return
        if (_uiState.value.isRestoring || _uiState.value.isCreatingBackup) return
        val mode = _uiState.value.selectedRestoreMode
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true, error = null) }
            when (val result = backupManager.restoreBackup(preview.fileName, mode)) {
                is WorkspaceBackupResult.RestoreSuccess -> _uiState.update {
                    it.copy(
                        isRestoring = false,
                        selectedBackupPreview = null,
                        message = "Đã khôi phục ${result.importedCount} workspace"
                    )
                }
                else -> _uiState.update {
                    it.copy(isRestoring = false, error = result.errorMessage())
                }
            }
        }
    }

    fun cancelBackupPreview() {
        _uiState.update { it.copy(selectedBackupPreview = null) }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }
    fun consumeError() = _uiState.update { it.copy(error = null) }

    private fun reportError(message: String) {
        _uiState.update { it.copy(error = message, previewWorkspace = null) }
    }
}

private fun WorkspaceBackupResult.errorMessage(): String = when (this) {
    is WorkspaceBackupResult.InvalidBackup -> message
    is WorkspaceBackupResult.UnsupportedVersion -> "Không hỗ trợ backup version $version"
    is WorkspaceBackupResult.FileError -> message ?: "Không thể xử lý backup"
    WorkspaceBackupResult.Cancelled -> "Đã hủy"
    is WorkspaceBackupResult.BackupSuccess,
    is WorkspaceBackupResult.RestoreSuccess -> "Không thể xử lý backup"
}
