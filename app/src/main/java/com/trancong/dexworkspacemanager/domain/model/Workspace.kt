package com.trancong.dexworkspacemanager.domain.model

import com.trancong.dexworkspacemanager.feature.layouteditor.LayoutTemplate

data class Workspace(
    val id: Long = 0,
    val name: String,
    val template: LayoutTemplate,
    val leftRatio: Float,
    val topRatio: Float,
    val createdAt: Long,
    val updatedAt: Long,
    val appAssignments: List<WorkspaceAppAssignment> = emptyList(),
    val launchDelayMs: Long = 400L,
    val isFavorite: Boolean = false
) {
    init {
        require(name.isNotBlank()) { "Workspace name must not be blank." }
        require(leftRatio in 0.4f..0.8f) { "leftRatio must be between 0.4 and 0.8." }
        require(topRatio in 0.25f..0.75f) { "topRatio must be between 0.25 and 0.75." }
        require(launchDelayMs in 0L..5_000L) { "launchDelayMs must be between 0 and 5000." }
    }
}
