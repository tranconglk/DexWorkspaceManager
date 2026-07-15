package com.trancong.dexworkspacemanager.feature.workspacetransfer

import com.trancong.dexworkspacemanager.domain.model.Workspace
import com.trancong.dexworkspacemanager.platform.transfer.WorkspaceBackupPreview
import com.trancong.dexworkspacemanager.platform.transfer.WorkspaceRestoreMode

data class WorkspaceTransferUiState(
    val importFiles: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val selectedFileName: String? = null,
    val previewWorkspace: Workspace? = null,
    val backupFiles: List<String> = emptyList(),
    val selectedBackupPreview: WorkspaceBackupPreview? = null,
    val selectedRestoreMode: WorkspaceRestoreMode = WorkspaceRestoreMode.MERGE,
    val isCreatingBackup: Boolean = false,
    val isRestoring: Boolean = false,
    val message: String? = null,
    val error: String? = null
)
