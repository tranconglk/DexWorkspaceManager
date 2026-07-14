package com.trancong.dexworkspacemanager.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.trancong.dexworkspacemanager.data.local.entity.WorkspaceAppAssignmentEntity
import com.trancong.dexworkspacemanager.data.local.entity.WorkspaceEntity

data class WorkspaceWithAssignments(
    @Embedded
    val workspace: WorkspaceEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "workspaceId"
    )
    val assignments: List<WorkspaceAppAssignmentEntity>
)
