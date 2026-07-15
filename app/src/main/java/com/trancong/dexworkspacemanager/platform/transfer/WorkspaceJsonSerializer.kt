package com.trancong.dexworkspacemanager.platform.transfer

import com.trancong.dexworkspacemanager.domain.model.Workspace

interface WorkspaceJsonSerializer {
    fun serialize(workspace: Workspace): String
    fun deserialize(json: String): WorkspaceTransferResult
}
