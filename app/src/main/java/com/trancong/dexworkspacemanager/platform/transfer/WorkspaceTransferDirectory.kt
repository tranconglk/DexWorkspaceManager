package com.trancong.dexworkspacemanager.platform.transfer

import android.content.Context
import com.trancong.dexworkspacemanager.domain.model.Workspace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject

class WorkspaceTransferDirectory(
    context: Context,
    private val serializer: WorkspaceJsonSerializer
) {
    private val exportsDirectory = context.applicationContext.getExternalFilesDir(EXPORTS)
    private val importsDirectory = context.applicationContext.getExternalFilesDir(IMPORTS)

    suspend fun export(workspace: Workspace): WorkspaceTransferResult = withContext(Dispatchers.IO) {
        try {
            val directory = exportsDirectory ?: return@withContext WorkspaceTransferResult.FileError(
                "Không mở được thư mục exports"
            )
            directory.mkdirs()
            val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            val baseName = sanitizeFileName(workspace.name)
            var file = File(directory, "dex-workspace-$baseName-$timestamp.json")
            var suffix = 2
            while (file.exists()) {
                file = File(directory, "dex-workspace-$baseName-$timestamp-$suffix.json")
                suffix += 1
            }
            file.writeText(serializer.serialize(workspace), Charsets.UTF_8)
            WorkspaceTransferResult.Success(file.name)
        } catch (exception: IOException) {
            WorkspaceTransferResult.FileError(exception.message)
        }
    }

    suspend fun listImportFiles(): List<String> = withContext(Dispatchers.IO) {
        importsDirectory?.apply { mkdirs() }
            ?.listFiles { file ->
                file.isFile && file.extension.equals("json", true) && runCatching {
                    !JSONObject(file.readText(Charsets.UTF_8)).has("backupSchemaVersion")
                }.getOrDefault(true)
            }
            ?.sortedByDescending(File::lastModified)
            ?.map(File::getName)
            .orEmpty()
    }

    suspend fun listBackupFiles(): List<String> = withContext(Dispatchers.IO) {
        importsDirectory?.apply { mkdirs() }
            ?.listFiles { file ->
                file.isFile && file.extension.equals("json", true) && runCatching {
                    JSONObject(file.readText(Charsets.UTF_8)).has("backupSchemaVersion")
                }.getOrDefault(false)
            }
            ?.sortedByDescending(File::lastModified)
            ?.map(File::getName)
            .orEmpty()
    }

    suspend fun writeBackup(json: String, exportedAt: Long): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val directory = requireNotNull(exportsDirectory) { "Không mở được thư mục exports" }
                directory.mkdirs()
                val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
                    .format(Date(exportedAt))
                var file = File(
                    directory,
                    "${WorkspaceTransferContract.BACKUP_FILE_PREFIX}-$timestamp.json"
                )
                var suffix = 2
                while (file.exists()) {
                    file = File(
                        directory,
                        "${WorkspaceTransferContract.BACKUP_FILE_PREFIX}-$timestamp-$suffix.json"
                    )
                    suffix += 1
                }
                file.writeText(json, Charsets.UTF_8)
                file.name
            }
        }

    suspend fun readImportText(fileName: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val safeName = File(fileName).name
            require(safeName == fileName && safeName.endsWith(".json", true)) {
                "Tên file không hợp lệ"
            }
            val directory = requireNotNull(importsDirectory) { "Không mở được thư mục imports" }
            val file = File(directory, safeName)
            require(file.isFile) { "Không tìm thấy file" }
            file.readText(Charsets.UTF_8)
        }
    }

    suspend fun readImport(fileName: String): WorkspaceTransferResult = withContext(Dispatchers.IO) {
        try {
            val safeName = File(fileName).name
            if (safeName != fileName || !safeName.endsWith(".json", ignoreCase = true)) {
                return@withContext WorkspaceTransferResult.InvalidData("Tên file không hợp lệ")
            }
            val directory = importsDirectory
                ?: return@withContext WorkspaceTransferResult.FileError("Không mở được thư mục imports")
            val file = File(directory, safeName)
            if (!file.isFile) return@withContext WorkspaceTransferResult.FileError("Không tìm thấy file")
            when (val result = serializer.deserialize(file.readText(Charsets.UTF_8))) {
                is WorkspaceTransferResult.Success -> result.copy(fileName = safeName)
                else -> result
            }
        } catch (exception: IOException) {
            WorkspaceTransferResult.FileError(exception.message)
        }
    }

    private fun sanitizeFileName(name: String): String {
        val safe = name.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(60)
        return safe.ifEmpty { "workspace" }
    }

    private companion object {
        const val EXPORTS = "exports"
        const val IMPORTS = "imports"
    }
}
