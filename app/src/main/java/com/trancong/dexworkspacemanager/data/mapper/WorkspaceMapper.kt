package com.trancong.dexworkspacemanager.data.mapper

import com.trancong.dexworkspacemanager.data.local.entity.WorkspaceEntity
import com.trancong.dexworkspacemanager.data.local.model.WorkspaceWithAssignments
import com.trancong.dexworkspacemanager.domain.model.Workspace
import com.trancong.dexworkspacemanager.feature.layouteditor.LayoutTemplate

fun WorkspaceEntity.toDomain(): Workspace = Workspace(
    id = id,
    name = name,
    template = runCatching { LayoutTemplate.valueOf(template) }
        .getOrDefault(LayoutTemplate.THREE_ZONES),
    leftRatio = leftRatio,
    topRatio = topRatio,
    createdAt = createdAt,
    updatedAt = updatedAt,
    launchDelayMs = launchDelayMs
)

fun WorkspaceWithAssignments.toDomain(): Workspace = workspace.toDomain().copy(
    appAssignments = assignments.map { it.toDomain() }
)

fun Workspace.toEntity(): WorkspaceEntity = WorkspaceEntity(
    id = id,
    name = name,
    template = template.name,
    leftRatio = leftRatio,
    topRatio = topRatio,
    createdAt = createdAt,
    updatedAt = updatedAt,
    launchDelayMs = launchDelayMs
)
