package com.trancong.dexworkspacemanager.feature.home

import com.trancong.dexworkspacemanager.domain.model.Workspace
import com.trancong.dexworkspacemanager.feature.layouteditor.WorkspaceLaunchProgress

data class HomeUiState(
    val favoriteWorkspaces: List<Workspace> = emptyList(),
    val recentWorkspaces: List<Workspace> = emptyList(),
    val quickLaunchWorkspace: Workspace? = null,
    val isLoading: Boolean = true,
    val launchProgress: WorkspaceLaunchProgress = WorkspaceLaunchProgress(0, 0),
    val launchingWorkspaceId: Long? = null,
    val workspacePendingRename: Workspace? = null,
    val workspacePendingDuplicate: Workspace? = null,
    val message: String? = null,
    val error: String? = null
)
