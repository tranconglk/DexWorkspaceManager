package com.trancong.dexworkspacemanager.platform.applauncher

import com.trancong.dexworkspacemanager.feature.layouteditor.LayoutZone
import kotlin.math.roundToInt

object LayoutBoundsCalculator {
    fun calculate(
        zone: LayoutZone,
        availableWidth: Int,
        availableHeight: Int,
        originX: Int = 0,
        originY: Int = 0,
        marginPx: Int = 0
    ): LaunchBounds {
        require(availableWidth > 0) { "availableWidth must be positive" }
        require(availableHeight > 0) { "availableHeight must be positive" }
        require(originX >= 0) { "originX must not be negative" }
        require(originY >= 0) { "originY must not be negative" }
        require(marginPx >= 0) { "marginPx must not be negative" }

        val workRight = originX + availableWidth
        val workBottom = originY + availableHeight
        val left = (originX + zone.x * availableWidth).roundToInt() + marginPx
        val top = (originY + zone.y * availableHeight).roundToInt() + marginPx
        val right = (originX + (zone.x + zone.width) * availableWidth).roundToInt() - marginPx
        val bottom =
            (originY + (zone.y + zone.height) * availableHeight).roundToInt() - marginPx

        return LaunchBounds(
            left = left.coerceIn(originX, workRight),
            top = top.coerceIn(originY, workBottom),
            right = right.coerceIn(originX, workRight),
            bottom = bottom.coerceIn(originY, workBottom)
        )
    }
}
