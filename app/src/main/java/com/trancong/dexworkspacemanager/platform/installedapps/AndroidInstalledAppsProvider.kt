package com.trancong.dexworkspacemanager.platform.installedapps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidInstalledAppsProvider(context: Context) : InstalledAppsProvider {
    private val applicationContext = context.applicationContext
    private val packageManager = applicationContext.packageManager

    override suspend fun getLaunchableApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(0L)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, 0)
        }

        resolveInfos
            .asSequence()
            .map { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo
                InstalledApp(
                    packageName = activityInfo.packageName,
                    activityName = activityInfo.name,
                    label = resolveInfo.loadLabel(packageManager).toString()
                )
            }
            .filterNot { app -> app.packageName == applicationContext.packageName }
            .distinctBy { app -> app.packageName to app.activityName }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { app -> app.label })
            .toList()
    }
}
