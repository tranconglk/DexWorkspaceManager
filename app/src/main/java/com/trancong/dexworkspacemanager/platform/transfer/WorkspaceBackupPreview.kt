package com.trancong.dexworkspacemanager.platform.transfer

data class WorkspaceBackupPreview(
    val fileName: String,
    val exportedAt: Long,
    val workspaceCount: Int,
    val favoriteCount: Int,
    val totalAssignments: Int,
    val workspaceNames: List<String>
)
