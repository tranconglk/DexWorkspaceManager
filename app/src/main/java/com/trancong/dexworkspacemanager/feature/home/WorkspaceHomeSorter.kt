package com.trancong.dexworkspacemanager.feature.home

import com.trancong.dexworkspacemanager.domain.model.Workspace

data class WorkspaceHomeSelection(
    val favorites: List<Workspace>,
    val recent: List<Workspace>,
    val quickLaunchWorkspace: Workspace?
)

object WorkspaceHomeSorter {
    fun sort(
        workspaces: List<Workspace>,
        savedQuickLaunchId: Long?,
        limit: Int
    ): WorkspaceHomeSelection {
        require(limit >= 0) { "limit must not be negative" }
        val allFavorites = workspaces
            .filter(Workspace::isFavorite)
            .sortedByDescending(Workspace::updatedAt)
        val favoriteIds = allFavorites.mapTo(mutableSetOf(), Workspace::id)
        return WorkspaceHomeSelection(
            favorites = allFavorites.take(limit),
            recent = workspaces
                .filterNot { it.id in favoriteIds }
                .sortedByDescending(Workspace::updatedAt)
                .take(limit),
            quickLaunchWorkspace = allFavorites.firstOrNull { it.id == savedQuickLaunchId }
                ?: allFavorites.firstOrNull()
        )
    }
}
