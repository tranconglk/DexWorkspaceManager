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

    override fun launchFromActivityWithBounds(
        activity: Activity,
        packageName: String,
        activityName: String,
        bounds: LaunchBounds,
        strategy: LaunchStrategy
    ): AppLaunchResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return AppLaunchResult.BoundsNotSupported
        }
        if (bounds.width <= 0 || bounds.height <= 0) {
            return AppLaunchResult.InvalidBounds
        }

        val packageManager = activity.packageManager
        val reportsFreeformSupport = packageManager.hasSystemFeature(
            PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT
        )
        Log.d(LOG_TAG, "Freeform feature reported: $reportsFreeformSupport")

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

        if (
            strategy == LaunchStrategy.CLEAR_TASK_BOUNDS &&
            !isLauncherActivity(packageManager, packageName, activityName)
        ) {
            return AppLaunchResult.ActivityNotFound
        }

        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                this.component = component
                when (strategy) {
                    LaunchStrategy.STANDARD_BOUNDS -> {
                        addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    }
                    LaunchStrategy.NEW_TASK_BOUNDS -> {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    }
                    LaunchStrategy.CLEAR_TASK_BOUNDS -> {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    LaunchStrategy.LEGACY_DEX_BOUNDS -> {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    }
                }
            }
            val options = ActivityOptions.makeBasic().apply {
                launchBounds = Rect(bounds.left, bounds.top, bounds.right, bounds.bottom)
            }
            Log.d(
                STRATEGY_LOG_TAG,
                "packageName=$packageName, activityName=$activityName, " +
                    "strategy=$strategy, bounds=$bounds"
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

    private fun isLauncherActivity(
        packageManager: PackageManager,
        packageName: String,
        activityName: String
    ): Boolean {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage(packageName)
        }
        val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(0L)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, 0)
        }
        return activities.any { resolveInfo ->
            resolveInfo.activityInfo.packageName == packageName &&
                resolveInfo.activityInfo.name == activityName
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
        const val LOG_TAG = "DexLaunchBounds"
        const val STRATEGY_LOG_TAG = "DexLaunchStrategy"
    }
}
