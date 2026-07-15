package com.trancong.dexworkspacemanager.platform.transfer

import com.trancong.dexworkspacemanager.domain.repository.WorkspaceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
import org.json.JSONException

class WorkspaceBackupManager(
    private val repository: WorkspaceRepository,
    private val serializer: WorkspaceBackupJsonSerializer,
    private val directory: WorkspaceTransferDirectory
) {
    suspend fun createBackup(): WorkspaceBackupResult = try {
        val workspaces = repository.observeAll().first()
        val exportedAt = System.currentTimeMillis()
        val json = serializer.serialize(workspaces, exportedAt)
        directory.writeBackup(json, exportedAt).fold(
            onSuccess = { WorkspaceBackupResult.BackupSuccess(it, workspaces.size) },
            onFailure = { WorkspaceBackupResult.FileError(it.message) }
        )
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        WorkspaceBackupResult.FileError(exception.message)
    }

    suspend fun listBackupFiles(): List<String> = directory.listBackupFiles()

    suspend fun previewBackup(fileName: String): Result<WorkspaceBackupPreview> = try {
        val backup = readAndValidate(fileName)
        Result.success(
            WorkspaceBackupPreview(
                fileName = fileName,
                exportedAt = backup.exportedAt,
                workspaceCount = backup.workspaces.size,
                favoriteCount = backup.workspaces.count { it.isFavorite },
                totalAssignments = backup.workspaces.sumOf { it.appAssignments.size },
                workspaceNames = backup.workspaces.take(6).map { it.name }
            )
        )
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Result.failure(exception)
    }

    suspend fun restoreBackup(
        fileName: String,
        mode: WorkspaceRestoreMode
    ): WorkspaceBackupResult = try {
        val backup = readAndValidate(fileName)
        val count = when (mode) {
            WorkspaceRestoreMode.MERGE -> repository.mergeAll(backup.workspaces)
            WorkspaceRestoreMode.REPLACE_ALL -> repository.replaceAll(backup.workspaces)
        }
        WorkspaceBackupResult.RestoreSuccess(count, skippedCount = 0, mode)
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: UnsupportedBackupVersionException) {
        WorkspaceBackupResult.UnsupportedVersion(exception.version)
    } catch (exception: JSONException) {
        WorkspaceBackupResult.InvalidBackup(exception.message ?: "JSON backup không hợp lệ")
    } catch (exception: IllegalArgumentException) {
        WorkspaceBackupResult.InvalidBackup(exception.message ?: "Backup không hợp lệ")
    } catch (exception: Exception) {
        WorkspaceBackupResult.FileError(exception.message)
    }

    private suspend fun readAndValidate(fileName: String): WorkspaceBackup {
        val json = directory.readImportText(fileName).getOrElse { throw it }
        return serializer.deserialize(json)
    }
}
