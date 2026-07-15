package com.trancong.dexworkspacemanager.platform.applauncher

import com.trancong.dexworkspacemanager.feature.layouteditor.LayoutZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutBoundsCalculatorTest {
    @Test fun fullScreenZone_mapsToEntire1920x1200Area() {
        assertEquals(LaunchBounds(0, 0, 1920, 1200), calculate(zone(), 1920, 1200))
    }

    @Test fun leftSixtyFivePercent_usesRoundToIntContract() {
        assertEquals(LaunchBounds(0, 0, 1248, 1200), calculate(zone(width = 0.65f), 1920, 1200))
    }

    @Test fun topRightZone_mapsCorrectly() {
        val result = calculate(zone(x = 0.65f, width = 0.35f, height = 0.5f), 1920, 1200)
        assertEquals(LaunchBounds(1248, 0, 1920, 600), result)
    }

    @Test fun bottomRightZone_mapsCorrectly() {
        val result = calculate(zone(x = 0.65f, y = 0.5f, width = 0.35f, height = 0.5f), 1920, 1200)
        assertEquals(LaunchBounds(1248, 600, 1920, 1200), result)
    }

    @Test fun originAndMargin_areApplied() {
        val result = LayoutBoundsCalculator.calculate(zone(), 1000, 800, 20, 30, 8)
        assertEquals(LaunchBounds(28, 38, 1012, 822), result)
    }

    @Test fun differentResolution_isNotHardCoded() {
        val result = calculate(zone(width = 0.65f), 2560, 1440)
        assertEquals(LaunchBounds(0, 0, 1664, 1440), result)
    }

    @Test fun fractionalCoordinates_followRoundToInt() {
        val result = calculate(zone(x = 0.333f, width = 0.333f), 101, 99)
        assertEquals(34, result.left)
        assertEquals(67, result.right)
    }

    @Test fun validInput_alwaysProducesPositiveBoundsInsideWorkArea() {
        val result = LayoutBoundsCalculator.calculate(zone(0.8f, 0.8f, 0.2f, 0.2f), 500, 300, 10, 20, 1)
        assertTrue(result.left >= 10 && result.top >= 20)
        assertTrue(result.right <= 510 && result.bottom <= 320)
        assertTrue(result.right > result.left && result.bottom > result.top)
    }

    private fun calculate(zone: LayoutZone, width: Int, height: Int) =
        LayoutBoundsCalculator.calculate(zone, width, height)

    private fun zone(
        x: Float = 0f,
        y: Float = 0f,
        width: Float = 1f,
        height: Float = 1f
    ) = LayoutZone("zone", "Zone", x, y, width, height)
}
