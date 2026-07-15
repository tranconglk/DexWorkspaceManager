package com.trancong.dexworkspacemanager.feature.home

import com.trancong.dexworkspacemanager.domain.model.Workspace
import com.trancong.dexworkspacemanager.feature.layouteditor.LayoutTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WorkspaceHomeSorterTest {
    @Test fun favoritesAndRecent_areSortedAndDoNotOverlap() {
        val workspaces = listOf(
            workspace(1, updatedAt = 10, favorite = false),
            workspace(2, updatedAt = 20, favorite = true),
            workspace(3, updatedAt = 30, favorite = true),
            workspace(4, updatedAt = 40, favorite = false)
        )
        val result = WorkspaceHomeSorter.sort(workspaces, savedQuickLaunchId = 2, limit = 4)
        assertEquals(listOf(3L, 2L), result.favorites.map { it.id })
        assertEquals(listOf(4L, 1L), result.recent.map { it.id })
        assertFalse(result.recent.any { recent -> result.favorites.any { it.id == recent.id } })
    }

    @Test fun eachVisibleGroup_isLimitedToFourItems() {
        val workspaces = (1L..12L).map { workspace(it, it, favorite = it <= 6) }
        val result = WorkspaceHomeSorter.sort(workspaces, null, 4)
        assertEquals(4, result.favorites.size)
        assertEquals(4, result.recent.size)
    }

    @Test fun validSavedQuickLaunchId_isSelected() {
        val result = WorkspaceHomeSorter.sort(
            listOf(workspace(1, 20, true), workspace(2, 10, true)),
            savedQuickLaunchId = 2,
            limit = 4
        )
        assertEquals(2L, result.quickLaunchWorkspace?.id)
    }

    @Test fun invalidSavedQuickLaunchId_fallsBackToNewestFavorite() {
        val result = WorkspaceHomeSorter.sort(
            listOf(workspace(1, 20, true), workspace(2, 30, true)),
            savedQuickLaunchId = 99,
            limit = 4
        )
        assertEquals(2L, result.quickLaunchWorkspace?.id)
    }

    private fun workspace(id: Long, updatedAt: Long, favorite: Boolean) = Workspace(
        id = id,
        name = "Workspace $id",
        template = LayoutTemplate.TWO_ZONES,
        leftRatio = 0.65f,
        topRatio = 0.5f,
        createdAt = 1,
        updatedAt = updatedAt,
        isFavorite = favorite
    )
}
