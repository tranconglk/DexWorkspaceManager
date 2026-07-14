package com.trancong.dexworkspacemanager

import android.content.Context
import androidx.room.Room
import com.trancong.dexworkspacemanager.data.local.database.AppDatabase
import com.trancong.dexworkspacemanager.data.repository.WorkspaceRepositoryImpl
import com.trancong.dexworkspacemanager.domain.repository.WorkspaceRepository

interface AppContainer {
    val workspaceRepository: WorkspaceRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val appDatabase: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "dex_workspace_manager.db"
    ).build()

    override val workspaceRepository: WorkspaceRepository =
        WorkspaceRepositoryImpl(appDatabase.workspaceDao())
}
