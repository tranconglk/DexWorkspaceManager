package com.trancong.dexworkspacemanager.feature.layouteditor

import org.junit.Test

class LayoutZoneTest {
    @Test(expected = IllegalArgumentException::class) fun negativeX_isRejected() { zone(x = -0.1f) }
    @Test(expected = IllegalArgumentException::class) fun yAboveOne_isRejected() { zone(y = 1.1f) }
    @Test(expected = IllegalArgumentException::class) fun zeroWidth_isRejected() { zone(width = 0f) }
    @Test(expected = IllegalArgumentException::class) fun negativeHeight_isRejected() { zone(height = -0.1f) }
    @Test(expected = IllegalArgumentException::class) fun xPlusWidthAboveOne_isRejected() {
        zone(x = 0.6f, width = 0.5f)
    }
    @Test(expected = IllegalArgumentException::class) fun yPlusHeightAboveOne_isRejected() {
        zone(y = 0.6f, height = 0.5f)
    }

    private fun zone(
        x: Float = 0f,
        y: Float = 0f,
        width: Float = 0.5f,
        height: Float = 0.5f
    ) = LayoutZone("zone", "Zone", x, y, width, height)
}
