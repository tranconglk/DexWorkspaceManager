package com.trancong.dexworkspacemanager

import android.content.Context
import androidx.room.Room
import com.trancong.dexworkspacemanager.data.local.database.AppDatabase
import com.trancong.dexworkspacemanager.data.repository.WorkspaceRepositoryImpl
import com.trancong.dexworkspacemanager.domain.repository.WorkspaceRepository
import com.trancong.dexworkspacemanager.platform.applauncher.AndroidAppLauncher
import com.trancong.dexworkspacemanager.platform.applauncher.AppLauncher
import com.trancong.dexworkspacemanager.platform.installedapps.AndroidInstalledAppsProvider
import com.trancong.dexworkspacemanager.platform.installedapps.InstalledAppsProvider

interface AppContainer {
    val workspaceRepository: WorkspaceRepository
    val installedAppsProvider: InstalledAppsProvider
    val appLauncher: AppLauncher
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val appDatabase: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "dex_workspace_manager.db"
    ).build()

    override val workspaceRepository: WorkspaceRepository =
        WorkspaceRepositoryImpl(appDatabase.workspaceDao())

    override val installedAppsProvider: InstalledAppsProvider =
        AndroidInstalledAppsProvider(context)

    override val appLauncher: AppLauncher = AndroidAppLauncher(context)
}
