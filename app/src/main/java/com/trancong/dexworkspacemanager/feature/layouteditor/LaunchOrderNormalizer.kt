package com.trancong.dexworkspacemanager.feature.layouteditor

object LaunchOrderNormalizer {
    fun normalize(assignments: Collection<ZoneAppAssignment>): Map<String, ZoneAppAssignment> =
        assignments
            .sortedWith(compareBy<ZoneAppAssignment> { it.launchOrder }.thenBy { it.zoneId })
            .mapIndexed { index, assignment -> assignment.copy(launchOrder = index) }
            .associateBy(ZoneAppAssignment::zoneId)

    fun nextOrder(assignments: Collection<ZoneAppAssignment>): Int =
        (assignments.maxOfOrNull(ZoneAppAssignment::launchOrder) ?: -1) + 1
}
