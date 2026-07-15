package com.trancong.dexworkspacemanager.platform.transfer

import com.trancong.dexworkspacemanager.domain.model.Workspace
import org.json.JSONArray
import org.json.JSONObject

class DefaultWorkspaceBackupJsonSerializer(
    private val workspaceSerializer: WorkspaceJsonSerializer
) : WorkspaceBackupJsonSerializer {
    override fun serialize(workspaces: List<Workspace>, exportedAt: Long): String {
        val array = JSONArray()
        workspaces.forEach { workspace ->
            array.put(JSONObject(workspaceSerializer.serialize(workspace)).getJSONObject("workspace"))
        }
        return JSONObject()
            .put("backupSchemaVersion", WorkspaceTransferContract.BACKUP_SCHEMA_VERSION)
            .put("exportedAt", exportedAt)
            .put(
                "application",
                JSONObject()
                    .put("packageName", "com.trancong.dexworkspacemanager")
                    .put("format", "workspace-collection")
            )
            .put("workspaces", array)
            .toString(2)
    }

    override fun deserialize(json: String): WorkspaceBackup {
        val root = JSONObject(json)
        require(root.has("backupSchemaVersion")) { "Thiếu backupSchemaVersion" }
        val version = root.getInt("backupSchemaVersion")
        if (version != WorkspaceTransferContract.BACKUP_SCHEMA_VERSION) {
            throw UnsupportedBackupVersionException(version)
        }
        val application = root.getJSONObject("application")
        require(application.getString("format") == "workspace-collection") {
            "Format backup không hợp lệ"
        }
        val exportedAt = root.getLong("exportedAt")
        require(exportedAt >= 0) { "exportedAt không hợp lệ" }
        val array = root.getJSONArray("workspaces")
        val restoreTime = System.currentTimeMillis()
        val workspaces = buildList {
            repeat(array.length()) { index ->
                val source = array.getJSONObject(index)
                val wrapped = JSONObject()
                    .put("schemaVersion", WorkspaceTransferContract.CURRENT_SCHEMA_VERSION)
                    .put("workspace", source)
                val parsed = when (val result = workspaceSerializer.deserialize(wrapped.toString())) {
                    is WorkspaceTransferResult.Success -> result.workspace
                        ?: throw IllegalArgumentException("Workspace #${index + 1} không hợp lệ")
                    is WorkspaceTransferResult.InvalidData ->
                        throw IllegalArgumentException("Workspace #${index + 1}: ${result.message}")
                    is WorkspaceTransferResult.UnsupportedVersion ->
                        throw IllegalArgumentException("Workspace #${index + 1}: version không hỗ trợ")
                    is WorkspaceTransferResult.FileError ->
                        throw IllegalArgumentException("Workspace #${index + 1}: lỗi dữ liệu")
                    WorkspaceTransferResult.Cancelled ->
                        throw IllegalArgumentException("Workspace #${index + 1}: đã hủy")
                }
                add(
                    parsed.copy(
                        id = 0,
                        createdAt = restoreTime,
                        updatedAt = restoreTime + index,
                        isFavorite = source.optBoolean("isFavorite", false)
                    )
                )
            }
        }
        return WorkspaceBackup(version, exportedAt, workspaces)
    }
}

class UnsupportedBackupVersionException(val version: Int) : IllegalArgumentException()
