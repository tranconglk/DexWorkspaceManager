package com.trancong.dexworkspacemanager

import android.content.Context
import androidx.room.Room
import com.trancong.dexworkspacemanager.data.local.database.AppDatabase
import com.trancong.dexworkspacemanager.data.local.database.MIGRATION_1_2
import com.trancong.dexworkspacemanager.data.local.database.MIGRATION_2_3
import com.trancong.dexworkspacemanager.data.local.database.MIGRATION_3_4
import com.trancong.dexworkspacemanager.data.repository.WorkspaceRepositoryImpl
import com.trancong.dexworkspacemanager.domain.repository.WorkspaceRepository
import com.trancong.dexworkspacemanager.platform.applauncher.AndroidAppLauncher
import com.trancong.dexworkspacemanager.platform.applauncher.AndroidForegroundAppLauncher
import com.trancong.dexworkspacemanager.platform.applauncher.AppLauncher
import com.trancong.dexworkspacemanager.platform.applauncher.ForegroundAppLauncher
import com.trancong.dexworkspacemanager.platform.applauncher.WorkspaceLaunchCoordinator
import com.trancong.dexworkspacemanager.platform.dex.AndroidDexDisplayProvider
import com.trancong.dexworkspacemanager.platform.dex.DexDisplayProvider
import com.trancong.dexworkspacemanager.platform.installedapps.AndroidInstalledAppsProvider
import com.trancong.dexworkspacemanager.platform.installedapps.InstalledAppsProvider
import com.trancong.dexworkspacemanager.platform.transfer.DefaultWorkspaceJsonSerializer
import com.trancong.dexworkspacemanager.platform.transfer.WorkspaceTransferDirectory

interface AppContainer {
    val workspaceRepository: WorkspaceRepository
    val installedAppsProvider: InstalledAppsProvider
    val appLauncher: AppLauncher
    val dexDisplayProvider: DexDisplayProvider
    val foregroundAppLauncher: ForegroundAppLauncher
    val workspaceLaunchCoordinator: WorkspaceLaunchCoordinator
    val workspaceTransferDirectory: WorkspaceTransferDirectory
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val appDatabase: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "dex_workspace_manager.db"
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()

    override val workspaceRepository: WorkspaceRepository =
        WorkspaceRepositoryImpl(appDatabase)

    override val installedAppsProvider: InstalledAppsProvider =
        AndroidInstalledAppsProvider(context)

    override val appLauncher: AppLauncher = AndroidAppLauncher(context)

    override val dexDisplayProvider: DexDisplayProvider = AndroidDexDisplayProvider(context)

    override val foregroundAppLauncher: ForegroundAppLauncher = AndroidForegroundAppLauncher()

    override val workspaceLaunchCoordinator = WorkspaceLaunchCoordinator(foregroundAppLauncher)

    override val workspaceTransferDirectory = WorkspaceTransferDirectory(
        context.applicationContext,
        DefaultWorkspaceJsonSerializer()
    )
}
