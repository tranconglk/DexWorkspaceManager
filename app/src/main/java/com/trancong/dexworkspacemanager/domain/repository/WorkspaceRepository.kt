package com.trancong.dexworkspacemanager.domain.repository

import com.trancong.dexworkspacemanager.domain.model.Workspace
import kotlinx.coroutines.flow.Flow

interface WorkspaceRepository {
    fun observeAll(): Flow<List<Workspace>>

    suspend fun getById(id: Long): Workspace?

    suspend fun save(workspace: Workspace): Long

    suspend fun deleteById(id: Long)
}
