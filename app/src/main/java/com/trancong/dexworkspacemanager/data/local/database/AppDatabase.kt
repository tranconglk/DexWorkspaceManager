package com.trancong.dexworkspacemanager.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.trancong.dexworkspacemanager.data.local.dao.WorkspaceDao
import com.trancong.dexworkspacemanager.data.local.dao.WorkspaceAppAssignmentDao
import com.trancong.dexworkspacemanager.data.local.entity.WorkspaceAppAssignmentEntity
import com.trancong.dexworkspacemanager.data.local.entity.WorkspaceEntity

@Database(
    entities = [WorkspaceEntity::class, WorkspaceAppAssignmentEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun workspaceAppAssignmentDao(): WorkspaceAppAssignmentDao
}
