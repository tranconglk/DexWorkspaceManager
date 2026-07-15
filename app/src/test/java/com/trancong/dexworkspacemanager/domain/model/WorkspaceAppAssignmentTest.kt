package com.trancong.dexworkspacemanager.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspaceAppAssignmentTest {
    @Test(expected = IllegalArgumentException::class) fun blankZoneId_isRejected() { assignment(zone = "") }
    @Test(expected = IllegalArgumentException::class) fun blankPackageName_isRejected() { assignment(pkg = " ") }
    @Test(expected = IllegalArgumentException::class) fun blankActivityName_isRejected() { assignment(activity = "") }
    @Test(expected = IllegalArgumentException::class) fun blankLabel_isRejected() { assignment(label = "") }
    @Test(expected = IllegalArgumentException::class) fun negativeOrder_isRejected() { assignment(order = -1) }
    @Test fun validAssignment_isCreated() { assertEquals(2, assignment(order = 2).launchOrder) }

    private fun assignment(
        zone: String = "zone_1",
        pkg: String = "com.example",
        activity: String = "com.example.Main",
        label: String = "Example",
        order: Int = 0
    ) = WorkspaceAppAssignment(zone, pkg, activity, label, order)
}
