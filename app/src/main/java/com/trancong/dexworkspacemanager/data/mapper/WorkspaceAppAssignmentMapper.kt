package com.trancong.dexworkspacemanager.data.mapper

import com.trancong.dexworkspacemanager.data.local.entity.WorkspaceAppAssignmentEntity
import com.trancong.dexworkspacemanager.domain.model.WorkspaceAppAssignment
import com.trancong.dexworkspacemanager.feature.layouteditor.ZoneAppAssignment

fun WorkspaceAppAssignmentEntity.toDomain(): WorkspaceAppAssignment = WorkspaceAppAssignment(
    zoneId = zoneId,
    packageName = packageName,
    activityName = activityName,
    appLabel = appLabel,
    launchOrder = launchOrder
)

fun WorkspaceAppAssignment.toEntity(workspaceId: Long): WorkspaceAppAssignmentEntity =
    WorkspaceAppAssignmentEntity(
        workspaceId = workspaceId,
        zoneId = zoneId,
        packageName = packageName,
        activityName = activityName,
        appLabel = appLabel,
        launchOrder = launchOrder
    )

fun ZoneAppAssignment.toDomain(): WorkspaceAppAssignment = WorkspaceAppAssignment(
    zoneId = zoneId,
    packageName = packageName,
    activityName = activityName,
    appLabel = appLabel,
    launchOrder = launchOrder
)

fun WorkspaceAppAssignment.toEditorAssignment(): ZoneAppAssignment = ZoneAppAssignment(
    zoneId = zoneId,
    packageName = packageName,
    activityName = activityName,
    appLabel = appLabel,
    launchOrder = launchOrder
)
