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
        val workspaceId = workspaceDao.upsert(workspace.toEntity())
        assignmentDao.deleteByWorkspaceId(workspaceId)
        if (workspace.appAssignments.isNotEmpty()) {
            assignmentDao.insertAll(
                workspace.appAssignments.map { it.toEntity(workspaceId) }
            )
        }
        workspaceId
    }

    override suspend fun deleteById(id: Long) {
        workspaceDao.deleteById(id)
    }
}
