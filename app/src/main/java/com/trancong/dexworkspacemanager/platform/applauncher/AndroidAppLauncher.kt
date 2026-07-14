package com.trancong.dexworkspacemanager.platform.applauncher

import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.Build
import com.trancong.dexworkspacemanager.platform.dex.DexLaunchMode

class AndroidAppLauncher(context: Context) : AppLauncher {
    private val applicationContext = context.applicationContext
    private val packageManager = applicationContext.packageManager

    override fun launch(
        packageName: String,
        activityName: String
    ): AppLaunchResult {
        try {
            verifyPackageExists(packageName)
        } catch (exception: PackageManager.NameNotFoundException) {
            return AppLaunchResult.AppNotFound
        } catch (exception: SecurityException) {
            return AppLaunchResult.SecurityError
        } catch (exception: Exception) {
            return AppLaunchResult.UnknownError(exception.message)
        }

        val component = ComponentName(packageName, activityName)
        try {
            verifyActivityExists(component)
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
                addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
            applicationContext.startActivity(intent)
            AppLaunchResult.Success
        } catch (exception: ActivityNotFoundException) {
            AppLaunchResult.ActivityNotFound
        } catch (exception: SecurityException) {
            AppLaunchResult.SecurityError
        } catch (exception: Exception) {
            AppLaunchResult.UnknownError(exception.message)
        }
    }

    override fun launchOnDisplay(
        packageName: String,
        activityName: String,
        displayId: Int
    ): AppLaunchResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return AppLaunchResult.MultiDisplayNotSupported
        }
        if (!packageManager.hasSystemFeature(
                PackageManager.FEATURE_ACTIVITIES_ON_SECONDARY_DISPLAYS
            )
        ) {
            return AppLaunchResult.MultiDisplayNotSupported
        }

        val displayManager = applicationContext.getSystemService(Context.DISPLAY_SERVICE)
            as DisplayManager
        if (displayManager.getDisplay(displayId) == null) {
            return AppLaunchResult.DisplayNotAvailable
        }

        try {
            verifyPackageExists(packageName)
        } catch (exception: PackageManager.NameNotFoundException) {
            return AppLaunchResult.AppNotFound
        } catch (exception: SecurityException) {
            return AppLaunchResult.SecurityError
        } catch (exception: Exception) {
            return AppLaunchResult.UnknownError(exception.message)
        }

        val component = ComponentName(packageName, activityName)
        try {
            verifyActivityExists(component)
        } catch (exception: PackageManager.NameNotFoundException) {
            return AppLaunchResult.ActivityNotFound
        } catch (exception: SecurityException) {
            return AppLaunchResult.SecurityError
        } catch (exception: Exception) {
            return AppLaunchResult.UnknownError(exception.message)
        }

        return try {
            val intent = createLaunchIntent(component)
            val options = ActivityOptions.makeBasic().apply {
                launchDisplayId = displayId
            }
            applicationContext.startActivity(intent, options.toBundle())
            AppLaunchResult.Success
        } catch (exception: ActivityNotFoundException) {
            AppLaunchResult.ActivityNotFound
        } catch (exception: SecurityException) {
            AppLaunchResult.LaunchNotAllowedOnDisplay
        } catch (exception: IllegalArgumentException) {
            AppLaunchResult.DisplayNotAvailable
        } catch (exception: Exception) {
            AppLaunchResult.UnknownError(exception.message)
        }
    }

    override fun launchWithMode(
        packageName: String,
        activityName: String,
        mode: DexLaunchMode,
        displayId: Int?
    ): AppLaunchResult = when (mode) {
        DexLaunchMode.MODERN_DISPLAY_API -> {
            if (displayId == null) {
                AppLaunchResult.DisplayNotAvailable
            } else {
                launchOnDisplay(packageName, activityName, displayId)
            }
        }
        DexLaunchMode.LEGACY_CURRENT_DISPLAY -> AppLaunchResult.UnknownError(
            "Legacy launch requires the current foreground Activity"
        )
        DexLaunchMode.DEFAULT_DISPLAY -> launch(packageName, activityName)
    }

    private fun verifyPackageExists(packageName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }
    }

    private fun verifyActivityExists(component: ComponentName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getActivityInfo(component, PackageManager.ComponentInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getActivityInfo(component, 0)
        }
    }

    private fun createLaunchIntent(component: ComponentName) = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
        this.component = component
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    }
}
