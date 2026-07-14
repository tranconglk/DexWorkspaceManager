package com.trancong.dexworkspacemanager.data.repository

import com.trancong.dexworkspacemanager.data.local.dao.WorkspaceDao
import com.trancong.dexworkspacemanager.data.mapper.toDomain
import com.trancong.dexworkspacemanager.data.mapper.toEntity
import com.trancong.dexworkspacemanager.domain.model.Workspace
import com.trancong.dexworkspacemanager.domain.repository.WorkspaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkspaceRepositoryImpl(
    private val workspaceDao: WorkspaceDao
) : WorkspaceRepository {
    override fun observeAll(): Flow<List<Workspace>> =
        workspaceDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getById(id: Long): Workspace? =
        workspaceDao.getById(id)?.toDomain()

    override suspend fun save(workspace: Workspace): Long =
        workspaceDao.upsert(workspace.toEntity())

    override suspend fun deleteById(id: Long) {
        workspaceDao.deleteById(id)
    }
}
