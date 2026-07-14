package com.trancong.dexworkspacemanager.platform.dex

import android.content.Context
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.view.Display

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
        return if (hasExternalDisplay) {
            DexLaunchMode.TARGET_DISPLAY_API
        } else {
            DexLaunchMode.DEFAULT_ACTIVITY
        }
    }

    override fun getRecommendedWorkArea(): DexWorkArea? {
        val displays = try {
            getExternalDisplays()
        } catch (exception: Exception) {
            return null
        }
        val display = displays.singleOrNull { it.isLikelyDexDisplay }
            ?: displays.singleOrNull()
            ?: return null
        return DexWorkArea(
            width = display.width,
            height = display.height,
            bottomInset = 0 // Experimental: public Display APIs do not expose the DeX taskbar inset.
        )
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
    }
}
