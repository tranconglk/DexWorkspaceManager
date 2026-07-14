package com.trancong.dexworkspacemanager.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.trancong.dexworkspacemanager.data.local.dao.WorkspaceDao
import com.trancong.dexworkspacemanager.data.local.entity.WorkspaceEntity

@Database(
    entities = [WorkspaceEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workspaceDao(): WorkspaceDao
}
