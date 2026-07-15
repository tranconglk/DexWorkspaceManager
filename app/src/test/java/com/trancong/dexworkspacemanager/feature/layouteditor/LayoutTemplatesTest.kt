package com.trancong.dexworkspacemanager.feature.layouteditor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutTemplatesTest {
    @Test fun twoZones_haveStableIdsAndCoverWidth() {
        val zones = LayoutTemplates.zonesFor(LayoutTemplate.TWO_ZONES)
        assertEquals(listOf("zone_1", "zone_2"), zones.map { it.id })
        assertEquals(2, zones.size)
        assertEquals(1f, zones.sumOf { it.width.toDouble() }.toFloat(), 0f)
        assertTrue(zones.all(::isInsideUnitBounds))
        assertEquals(zones[0].x + zones[0].width, zones[1].x, 0f)
    }

    @Test fun threeZones_applyRatiosAndPositions() {
        val zones = LayoutTemplates.zonesFor(LayoutTemplate.THREE_ZONES, 0.65f, 0.4f)
        val left = zones.single { it.id == "zone_1" }
        val topRight = zones.single { it.id == "zone_2" }
        val bottomRight = zones.single { it.id == "zone_3" }
        assertEquals(3, zones.size)
        assertEquals(0.65f, left.width, 0f)
        assertEquals(0.65f, topRight.x, 0f)
        assertEquals(0.4f, topRight.height, 0f)
        assertEquals(0.4f, bottomRight.y, 0f)
        assertTrue(zones.all(::isInsideUnitBounds))
    }

    @Test fun ratiosOutsideLimits_areClamped() {
        val minimum = LayoutTemplates.zonesFor(LayoutTemplate.THREE_ZONES, -1f, -1f)
        val maximum = LayoutTemplates.zonesFor(LayoutTemplate.THREE_ZONES, 2f, 2f)
        assertEquals(0.4f, minimum.single { it.id == "zone_1" }.width, 0f)
        assertEquals(0.25f, minimum.single { it.id == "zone_2" }.height, 0f)
        assertEquals(0.8f, maximum.single { it.id == "zone_1" }.width, 0f)
        assertEquals(0.75f, maximum.single { it.id == "zone_2" }.height, 0f)
    }

    @Test fun emptyTemplate_returnsNoZones() {
        assertTrue(LayoutTemplates.zonesFor(LayoutTemplate.EMPTY).isEmpty())
    }

    private fun isInsideUnitBounds(zone: LayoutZone): Boolean =
        zone.x >= 0f && zone.y >= 0f && zone.x + zone.width <= 1f &&
            zone.y + zone.height <= 1f
}
