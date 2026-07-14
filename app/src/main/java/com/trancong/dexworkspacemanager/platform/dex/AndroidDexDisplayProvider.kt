package com.trancong.dexworkspacemanager.platform.dex

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.view.Display
import android.os.Build

class AndroidDexDisplayProvider(context: Context) : DexDisplayProvider {
    private val applicationContext = context.applicationContext
    private val displayManager = applicationContext.getSystemService(Context.DISPLAY_SERVICE)
        as DisplayManager

    override fun getCurrentState(): DexDisplayState = try {
        when (val displays = getExternalDisplays()) {
            emptyList<ExternalDisplayInfo>() -> DexDisplayState.NotConnected
            else -> if (displays.size == 1) {
                DexDisplayState.Connected(displays.first())
            } else {
                DexDisplayState.MultipleDisplays(displays)
            }
        }
    } catch (exception: Exception) {
        DexDisplayState.Error("Không thể đọc trạng thái màn hình ngoài")
    }

    override fun getExternalDisplays(): List<ExternalDisplayInfo> {
        val allDisplays = displayManager.displays.toList()
        val presentationDisplays = displayManager
            .getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
            .toList()
        val validDisplays = (allDisplays + presentationDisplays)
            .distinctBy { display -> display.displayId }
            .filter { display ->
                display.displayId != Display.DEFAULT_DISPLAY &&
                    display.state != Display.STATE_OFF &&
                    display.mode.physicalWidth > 0 &&
                    display.mode.physicalHeight > 0
            }

        val infos = validDisplays.map(::toExternalDisplayInfo)
        return if (infos.size == 1 && !infos.first().isLikelyDexDisplay) {
            listOf(infos.first().copy(isLikelyDexDisplay = true))
        } else {
            infos
        }
    }

    override fun determineRecommendedLaunchMode(): DexLaunchMode {
        val hasExternalDisplay = when (getCurrentState()) {
            is DexDisplayState.Connected,
            is DexDisplayState.MultipleDisplays -> true
            DexDisplayState.NotConnected,
            is DexDisplayState.Error -> false
        }
        if (!hasExternalDisplay) return DexLaunchMode.DEFAULT_DISPLAY

        val supportsSecondaryActivities = applicationContext.packageManager.hasSystemFeature(
            PackageManager.FEATURE_ACTIVITIES_ON_SECONDARY_DISPLAYS
        )
        val isSamsung = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
        val isOldAndroidGeneration = Build.VERSION.SDK_INT < MODERN_DISPLAY_API_MIN_SDK
        val isLegacySamsungGeneration = isSamsung &&
            Build.VERSION.SDK_INT <= LEGACY_SAMSUNG_MAX_SDK

        return if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            supportsSecondaryActivities &&
            !isOldAndroidGeneration &&
            !isLegacySamsungGeneration
        ) {
            DexLaunchMode.MODERN_DISPLAY_API
        } else {
            DexLaunchMode.LEGACY_CURRENT_DISPLAY
        }
    }

    @Suppress("DEPRECATION")
    private fun toExternalDisplayInfo(display: Display): ExternalDisplayInfo {
        val metrics = DisplayMetrics()
        display.getRealMetrics(metrics)
        val normalizedName = display.name.lowercase()
        val nameSuggestsDex = DEX_NAME_HINTS.any(normalizedName::contains)
        return ExternalDisplayInfo(
            id = display.displayId,
            name = display.name,
            width = metrics.widthPixels,
            height = metrics.heightPixels,
            densityDpi = metrics.densityDpi,
            isDefaultDisplay = display.displayId == Display.DEFAULT_DISPLAY,
            isLikelyDexDisplay = nameSuggestsDex
        )
    }

    private companion object {
        val DEX_NAME_HINTS = listOf("dex", "desktop", "external", "hdmi")
        const val MODERN_DISPLAY_API_MIN_SDK = Build.VERSION_CODES.S
        const val LEGACY_SAMSUNG_MAX_SDK = Build.VERSION_CODES.R
    }
}
