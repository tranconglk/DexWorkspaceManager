package com.trancong.dexworkspacemanager.platform.transfer

import com.trancong.dexworkspacemanager.domain.model.Workspace

interface WorkspaceBackupJsonSerializer {
    fun serialize(workspaces: List<Workspace>, exportedAt: Long): String
    fun deserialize(json: String): WorkspaceBackup
}
