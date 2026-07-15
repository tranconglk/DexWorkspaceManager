package com.trancong.dexworkspacemanager.platform.installedapps

import com.trancong.dexworkspacemanager.domain.model.WorkspaceAppAssignment

class WorkspaceAppsAvailabilityChecker(
    private val installedAppsProvider: InstalledAppsProvider
) {
    suspend fun check(
        assignments: Collection<WorkspaceAppAssignment>
    ): Map<String, InstalledAppAvailability> = buildMap {
        assignments.forEach { assignment ->
            put(
                assignment.zoneId,
                installedAppsProvider.checkAvailability(
                    assignment.packageName,
                    assignment.activityName
                )
            )
        }
    }
}
