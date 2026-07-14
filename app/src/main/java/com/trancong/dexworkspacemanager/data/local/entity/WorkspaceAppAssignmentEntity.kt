package com.trancong.dexworkspacemanager.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workspace_app_assignments",
    foreignKeys = [
        ForeignKey(
            entity = WorkspaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["workspaceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["workspaceId"]),
        Index(value = ["workspaceId", "zoneId"], unique = true)
    ]
)
data class WorkspaceAppAssignmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workspaceId: Long,
    val zoneId: String,
    val packageName: String,
    val activityName: String,
    val appLabel: String,
    @ColumnInfo(defaultValue = "0")
    val launchOrder: Int
)
