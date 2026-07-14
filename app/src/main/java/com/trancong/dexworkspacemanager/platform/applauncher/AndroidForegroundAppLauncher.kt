package com.trancong.dexworkspacemanager.platform.applauncher

import android.app.Activity
import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.graphics.Rect
import android.util.Log

class AndroidForegroundAppLauncher : ForegroundAppLauncher {
    override fun launchInZone(
        activity: Activity,
        packageName: String,
        activityName: String,
        bounds: LaunchBounds
    ): AppLaunchResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return AppLaunchResult.BoundsNotSupported
        }
        if (bounds.width <= 0 || bounds.height <= 0) {
            return AppLaunchResult.InvalidBounds
        }

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
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }
            val options = ActivityOptions.makeBasic().apply {
                launchBounds = Rect(bounds.left, bounds.top, bounds.right, bounds.bottom)
            }
            Log.d(
                LOG_TAG,
                "launchInZone packageName=$packageName, activityName=$activityName, bounds=$bounds"
            )
            activity.startActivity(intent, options.toBundle())
            AppLaunchResult.Success
        } catch (exception: IllegalArgumentException) {
            AppLaunchResult.InvalidBounds
        } catch (exception: ActivityNotFoundException) {
            AppLaunchResult.ActivityNotFound
        } catch (exception: SecurityException) {
            AppLaunchResult.SecurityError
        } catch (exception: UnsupportedOperationException) {
            AppLaunchResult.BoundsNotSupported
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

    private companion object {
        const val LOG_TAG = "DexLaunch"
    }
}
