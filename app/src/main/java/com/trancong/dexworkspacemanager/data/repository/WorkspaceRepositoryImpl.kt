package com.trancong.dexworkspacemanager.data.repository

import androidx.room.withTransaction
import com.trancong.dexworkspacemanager.data.local.database.AppDatabase
import com.trancong.dexworkspacemanager.data.mapper.toDomain
import com.trancong.dexworkspacemanager.data.mapper.toEntity
import com.trancong.dexworkspacemanager.domain.model.Workspace
import com.trancong.dexworkspacemanager.domain.repository.WorkspaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkspaceRepositoryImpl(
    private val database: AppDatabase
) : WorkspaceRepository {
    private val workspaceDao = database.workspaceDao()
    private val assignmentDao = database.workspaceAppAssignmentDao()

    override fun observeAll(): Flow<List<Workspace>> =
        workspaceDao.observeAllWithAssignments().map { relations ->
            relations.map { it.toDomain() }
        }

    override suspend fun getById(id: Long): Workspace? =
        workspaceDao.getByIdWithAssignments(id)?.toDomain()

    override suspend fun save(workspace: Workspace): Long = database.withTransaction {
        saveInTransaction(workspace)
    }

    private suspend fun saveInTransaction(workspace: Workspace): Long {
        val workspaceId = workspaceDao.upsert(workspace.toEntity())
        assignmentDao.deleteByWorkspaceId(workspaceId)
        if (workspace.appAssignments.isNotEmpty()) {
            assignmentDao.insertAll(
                workspace.appAssignments.map { it.toEntity(workspaceId) }
            )
        }
        return workspaceId
    }

    override suspend fun deleteById(id: Long) {
        workspaceDao.deleteById(id)
    }

    override suspend fun rename(workspaceId: Long, newName: String) {
        val trimmedName = newName.trim()
        require(trimmedName.isNotEmpty()) { "Workspace name must not be blank." }
        check(workspaceDao.rename(workspaceId, trimmedName, System.currentTimeMillis()) > 0) {
            "Workspace $workspaceId was not found."
        }
    }

    override suspend fun setFavorite(workspaceId: Long, isFavorite: Boolean) {
        check(workspaceDao.setFavorite(workspaceId, isFavorite) > 0) {
            "Workspace $workspaceId was not found."
        }
    }

    override suspend fun duplicate(workspaceId: Long, newName: String): Long {
        val trimmedName = newName.trim()
        require(trimmedName.isNotEmpty()) { "Workspace name must not be blank." }
        val source = getById(workspaceId)
            ?: error("Workspace $workspaceId was not found.")
        val now = System.currentTimeMillis()
        return save(
            source.copy(
                id = 0,
                name = trimmedName,
                createdAt = now,
                updatedAt = now,
                isFavorite = false
            )
        )
    }

    override suspend fun mergeAll(workspaces: List<Workspace>): Int = database.withTransaction {
        workspaces.forEach { saveInTransaction(it.copy(id = 0)) }
        workspaces.size
    }

    override suspend fun replaceAll(workspaces: List<Workspace>): Int = database.withTransaction {
        workspaceDao.deleteAll()
        workspaces.forEach { saveInTransaction(it.copy(id = 0)) }
        workspaces.size
    }
}
