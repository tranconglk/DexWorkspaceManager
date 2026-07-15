package com.trancong.dexworkspacemanager.feature.workspacetransfer

import com.trancong.dexworkspacemanager.domain.model.Workspace

data class WorkspaceTransferUiState(
    val importFiles: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val selectedFileName: String? = null,
    val previewWorkspace: Workspace? = null,
    val message: String? = null,
    val error: String? = null
)
