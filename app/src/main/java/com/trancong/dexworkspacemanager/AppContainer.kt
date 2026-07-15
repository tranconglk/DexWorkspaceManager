package com.trancong.dexworkspacemanager

import android.content.Context
import androidx.room.Room
import com.trancong.dexworkspacemanager.data.local.database.AppDatabase
import com.trancong.dexworkspacemanager.data.local.database.MIGRATION_1_2
import com.trancong.dexworkspacemanager.data.local.database.MIGRATION_2_3
import com.trancong.dexworkspacemanager.data.local.database.MIGRATION_3_4
import com.trancong.dexworkspacemanager.data.repository.WorkspaceRepositoryImpl
import com.trancong.dexworkspacemanager.data.repository.AppPreferencesRepositoryImpl
import com.trancong.dexworkspacemanager.data.local.preferences.appPreferencesDataStore
import com.trancong.dexworkspacemanager.domain.repository.AppPreferencesRepository
import com.trancong.dexworkspacemanager.domain.repository.WorkspaceRepository
import com.trancong.dexworkspacemanager.platform.applauncher.AndroidAppLauncher
import com.trancong.dexworkspacemanager.platform.applauncher.AndroidForegroundAppLauncher
import com.trancong.dexworkspacemanager.platform.applauncher.AppLauncher
import com.trancong.dexworkspacemanager.platform.applauncher.ForegroundAppLauncher
import com.trancong.dexworkspacemanager.platform.applauncher.WorkspaceLaunchCoordinator
import com.trancong.dexworkspacemanager.platform.dex.AndroidDexDisplayProvider
import com.trancong.dexworkspacemanager.platform.dex.DexDisplayProvider
import com.trancong.dexworkspacemanager.platform.installedapps.AndroidInstalledAppsProvider
import com.trancong.dexworkspacemanager.platform.installedapps.AndroidPackageChangeMonitor
import com.trancong.dexworkspacemanager.platform.installedapps.InstalledAppsProvider
import com.trancong.dexworkspacemanager.platform.installedapps.PackageChangeMonitor
import com.trancong.dexworkspacemanager.platform.installedapps.WorkspaceAppsAvailabilityChecker
import com.trancong.dexworkspacemanager.platform.transfer.DefaultWorkspaceJsonSerializer
import com.trancong.dexworkspacemanager.platform.transfer.DefaultWorkspaceBackupJsonSerializer
import com.trancong.dexworkspacemanager.platform.transfer.WorkspaceBackupManager
import com.trancong.dexworkspacemanager.platform.transfer.WorkspaceTransferDirectory

interface AppContainer {
    val workspaceRepository: WorkspaceRepository
    val installedAppsProvider: InstalledAppsProvider
    val workspaceAppsAvailabilityChecker: WorkspaceAppsAvailabilityChecker
    val packageChangeMonitor: PackageChangeMonitor
    val appLauncher: AppLauncher
    val dexDisplayProvider: DexDisplayProvider
    val foregroundAppLauncher: ForegroundAppLauncher
    val workspaceLaunchCoordinator: WorkspaceLaunchCoordinator
    val workspaceTransferDirectory: WorkspaceTransferDirectory
    val workspaceBackupManager: WorkspaceBackupManager
    val appPreferencesRepository: AppPreferencesRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val appDatabase: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "dex_workspace_manager.db"
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()

    override val workspaceRepository: WorkspaceRepository =
        WorkspaceRepositoryImpl(appDatabase)

    override val appPreferencesRepository: AppPreferencesRepository =
        AppPreferencesRepositoryImpl(context.applicationContext.appPreferencesDataStore)

    override val installedAppsProvider: InstalledAppsProvider =
        AndroidInstalledAppsProvider(context)

    override val workspaceAppsAvailabilityChecker =
        WorkspaceAppsAvailabilityChecker(installedAppsProvider)

    override val packageChangeMonitor: PackageChangeMonitor =
        AndroidPackageChangeMonitor(context.applicationContext)

    override val appLauncher: AppLauncher = AndroidAppLauncher(context)

    override val dexDisplayProvider: DexDisplayProvider = AndroidDexDisplayProvider(context)

    override val foregroundAppLauncher: ForegroundAppLauncher = AndroidForegroundAppLauncher()

    override val workspaceLaunchCoordinator = WorkspaceLaunchCoordinator(foregroundAppLauncher)

    private val workspaceJsonSerializer = DefaultWorkspaceJsonSerializer()

    override val workspaceTransferDirectory = WorkspaceTransferDirectory(
        context.applicationContext,
        workspaceJsonSerializer
    )

    override val workspaceBackupManager = WorkspaceBackupManager(
        workspaceRepository,
        DefaultWorkspaceBackupJsonSerializer(workspaceJsonSerializer),
        workspaceTransferDirectory
    )
}
