package com.trancong.dexworkspacemanager.feature.layouteditor

import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchOrderNormalizerTest {
    @Test fun continuousOrder_isPreserved() {
        assertEquals(listOf(0, 1, 2), normalizedOrders(assignment("a", 0), assignment("b", 1), assignment("c", 2)))
    }

    @Test fun duplicateOrder_isNormalizedDeterministicallyByZoneId() {
        val result = LaunchOrderNormalizer.normalize(listOf(assignment("b", 0), assignment("a", 0)))
        assertEquals(0, result.getValue("a").launchOrder)
        assertEquals(1, result.getValue("b").launchOrder)
    }

    @Test fun gaps_areNormalizedToContinuousOrder() {
        assertEquals(listOf(0, 1, 2), normalizedOrders(assignment("a", 0), assignment("b", 3), assignment("c", 8)))
    }

    @Test fun removingAssignment_thenNormalizingLeavesNoGap() {
        val remaining = listOf(assignment("a", 0), assignment("c", 2))
        assertEquals(listOf(0, 1), LaunchOrderNormalizer.normalize(remaining).values.map { it.launchOrder }.sorted())
    }

    @Test fun addingAssignment_usesMaximumPlusOne() {
        assertEquals(9, LaunchOrderNormalizer.nextOrder(listOf(assignment("a", 3), assignment("b", 8))))
        assertEquals(0, LaunchOrderNormalizer.nextOrder(emptyList()))
    }

    private fun normalizedOrders(vararg assignments: ZoneAppAssignment): List<Int> =
        LaunchOrderNormalizer.normalize(assignments.toList()).values.map { it.launchOrder }.sorted()

    private fun assignment(zone: String, order: Int) =
        ZoneAppAssignment(zone, "pkg.$zone", "Activity$zone", zone, order)
}
