package com.trancong.dexworkspacemanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.trancong.dexworkspacemanager.data.local.entity.WorkspaceEntity
import com.trancong.dexworkspacemanager.data.local.model.WorkspaceWithAssignments
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {
    @Transaction
    @Query("SELECT * FROM workspaces ORDER BY updatedAt DESC")
    fun observeAllWithAssignments(): Flow<List<WorkspaceWithAssignments>>

    @Transaction
    @Query("SELECT * FROM workspaces WHERE id = :id LIMIT 1")
    suspend fun getByIdWithAssignments(id: Long): WorkspaceWithAssignments?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(workspace: WorkspaceEntity): Long

    @Query("DELETE FROM workspaces WHERE id = :id")
    suspend fun deleteById(id: Long)
}
