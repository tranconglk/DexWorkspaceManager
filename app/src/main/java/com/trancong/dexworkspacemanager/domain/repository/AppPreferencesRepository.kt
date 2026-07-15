package com.trancong.dexworkspacemanager.domain.repository

import kotlinx.coroutines.flow.Flow

interface AppPreferencesRepository {
    val quickLaunchWorkspaceId: Flow<Long?>
    val showDexPinHint: Flow<Boolean>

    suspend fun setQuickLaunchWorkspaceId(workspaceId: Long?)
    suspend fun setShowDexPinHint(show: Boolean)
}
