package com.trancong.dexworkspacemanager.feature.savedlayouts

import com.trancong.dexworkspacemanager.domain.model.Workspace
import com.trancong.dexworkspacemanager.feature.layouteditor.WorkspaceLaunchProgress

data class SavedLayoutsUiState(
    val workspaces: List<Workspace> = emptyList(),
    val isLoading: Boolean = true,
    val workspacePendingDelete: Workspace? = null,
    val message: String? = null,
    val error: String? = null,
    val launchingWorkspaceId: Long? = null,
    val launchProgress: WorkspaceLaunchProgress = WorkspaceLaunchProgress(0, 0),
    val launchMessage: String? = null,
    val launchError: String? = null
)
