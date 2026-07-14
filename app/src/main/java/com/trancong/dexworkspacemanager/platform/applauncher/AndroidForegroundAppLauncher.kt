package com.trancong.dexworkspacemanager.platform.applauncher

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

class AndroidForegroundAppLauncher : ForegroundAppLauncher {
    override fun launchFromActivity(
        activity: Activity,
        packageName: String,
        activityName: String
    ): AppLaunchResult {
        val packageManager = activity.packageManager
        try {
            verifyPackageExists(packageManager, packageName)
        } catch (exception: PackageManager.NameNotFoundException) {
            return AppLaunchResult.AppNotFound
        } catch (exception: SecurityException) {
            return AppLaunchResult.SecurityError
        } catch (exception: Exception) {
            return AppLaunchResult.UnknownError(exception.message)
        }

        val component = ComponentName(packageName, activityName)
        try {
            verifyActivityExists(packageManager, component)
        } catch (exception: PackageManager.NameNotFoundException) {
            return AppLaunchResult.ActivityNotFound
        } catch (exception: SecurityException) {
            return AppLaunchResult.SecurityError
        } catch (exception: Exception) {
            return AppLaunchResult.UnknownError(exception.message)
        }

        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                this.component = component
                addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
            activity.startActivity(intent)
            AppLaunchResult.Success
        } catch (exception: ActivityNotFoundException) {
            AppLaunchResult.ActivityNotFound
        } catch (exception: SecurityException) {
            AppLaunchResult.SecurityError
        } catch (exception: Exception) {
            AppLaunchResult.UnknownError(exception.message)
        }
    }

    private fun verifyPackageExists(packageManager: PackageManager, packageName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }
    }

    private fun verifyActivityExists(
        packageManager: PackageManager,
        component: ComponentName
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getActivityInfo(component, PackageManager.ComponentInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getActivityInfo(component, 0)
        }
    }
}
