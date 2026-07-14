package com.trancong.dexworkspacemanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.trancong.dexworkspacemanager.data.local.entity.WorkspaceAppAssignmentEntity

@Dao
interface WorkspaceAppAssignmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(assignments: List<WorkspaceAppAssignmentEntity>)

    @Query("DELETE FROM workspace_app_assignments WHERE workspaceId = :workspaceId")
    suspend fun deleteByWorkspaceId(workspaceId: Long)
}
