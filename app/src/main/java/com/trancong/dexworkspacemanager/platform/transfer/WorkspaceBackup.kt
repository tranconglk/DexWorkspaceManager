package com.trancong.dexworkspacemanager.platform.transfer

import com.trancong.dexworkspacemanager.domain.model.Workspace

data class WorkspaceBackup(
    val schemaVersion: Int,
    val exportedAt: Long,
    val workspaces: List<Workspace>
)
