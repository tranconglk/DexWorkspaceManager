package com.trancong.dexworkspacemanager.platform.transfer

sealed interface WorkspaceBackupResult {
    data class BackupSuccess(val fileName: String, val workspaceCount: Int) : WorkspaceBackupResult
    data class RestoreSuccess(
        val importedCount: Int,
        val skippedCount: Int,
        val mode: WorkspaceRestoreMode
    ) : WorkspaceBackupResult
    data class InvalidBackup(val message: String) : WorkspaceBackupResult
    data class UnsupportedVersion(val version: Int) : WorkspaceBackupResult
    data class FileError(val message: String? = null) : WorkspaceBackupResult
    data object Cancelled : WorkspaceBackupResult
}
