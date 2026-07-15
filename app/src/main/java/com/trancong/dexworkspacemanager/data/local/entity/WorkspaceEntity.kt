package com.trancong.dexworkspacemanager.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workspaces")
data class WorkspaceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val template: String,
    val leftRatio: Float,
    val topRatio: Float,
    val createdAt: Long,
    val updatedAt: Long,
    @ColumnInfo(defaultValue = "400")
    val launchDelayMs: Long = 400L,
    @ColumnInfo(defaultValue = "0")
    val isFavorite: Boolean = false
)
