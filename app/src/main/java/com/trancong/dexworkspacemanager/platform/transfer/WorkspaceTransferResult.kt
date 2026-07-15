package com.trancong.dexworkspacemanager.platform.transfer

import com.trancong.dexworkspacemanager.domain.model.Workspace

sealed interface WorkspaceTransferResult {
    data class Success(
        val fileName: String,
        val workspace: Workspace? = null
    ) : WorkspaceTransferResult

    data class InvalidData(val message: String) : WorkspaceTransferResult
    data class UnsupportedVersion(val version: Int) : WorkspaceTransferResult
    data class FileError(val message: String? = null) : WorkspaceTransferResult
    data object Cancelled : WorkspaceTransferResult
}
