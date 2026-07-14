package com.trancong.dexworkspacemanager.feature.savedlayouts

import com.trancong.dexworkspacemanager.domain.model.Workspace

data class SavedLayoutsUiState(
    val workspaces: List<Workspace> = emptyList(),
    val isLoading: Boolean = true,
    val workspacePendingDelete: Workspace? = null,
    val message: String? = null,
    val error: String? = null
)
