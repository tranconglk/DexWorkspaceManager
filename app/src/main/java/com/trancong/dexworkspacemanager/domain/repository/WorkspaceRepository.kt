package com.trancong.dexworkspacemanager.domain.repository

import com.trancong.dexworkspacemanager.domain.model.Workspace
import kotlinx.coroutines.flow.Flow

interface WorkspaceRepository {
    fun observeAll(): Flow<List<Workspace>>

    suspend fun getById(id: Long): Workspace?

    suspend fun save(workspace: Workspace): Long

    suspend fun deleteById(id: Long)

    suspend fun rename(workspaceId: Long, newName: String)

    suspend fun setFavorite(workspaceId: Long, isFavorite: Boolean)

    suspend fun duplicate(workspaceId: Long, newName: String): Long

    suspend fun mergeAll(workspaces: List<Workspace>): Int

    suspend fun replaceAll(workspaces: List<Workspace>): Int
}
