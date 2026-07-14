package com.trancong.dexworkspacemanager.feature.layouteditor

import com.trancong.dexworkspacemanager.platform.dex.DexDisplayState
import com.trancong.dexworkspacemanager.platform.dex.DexLaunchMode
import com.trancong.dexworkspacemanager.platform.dex.DexWindowLaunchStrategy

data class LayoutEditorUiState(
    val workspaceId: Long? = null,
    val originalCreatedAt: Long? = null,
    val isLoadingWorkspace: Boolean = false,
    val loadError: String? = null,
    val selectedTemplate: LayoutTemplate = LayoutTemplate.THREE_ZONES,
    val leftRatio: Float = 0.65f,
    val topRatio: Float = 0.5f,
    val workspaceName: String = "",
    val isNameDialogVisible: Boolean = false,
    val isSaving: Boolean = false,
    val saveMessage: String? = null,
    val saveError: String? = null,
    val appAssignments: Map<String, ZoneAppAssignment> = emptyMap(),
    val launchMessage: String? = null,
    val launchError: String? = null,
    val dexDisplayState: DexDisplayState = DexDisplayState.NotConnected,
    val selectedExternalDisplayId: Int? = null,
    val recommendedDexLaunchMode: DexLaunchMode = DexLaunchMode.DEFAULT_ACTIVITY,
    val canLaunchOnDex: Boolean = false,
    val dexLaunchDescription: String = "Chưa phát hiện DeX",
    val selectedTestStrategy: DexWindowLaunchStrategy =
        DexWindowLaunchStrategy.MODERN_DISPLAY_AND_BOUNDS,
    val lastCompatibilityMessage: String? = null,
    val lastCompatibilityError: String? = null
)
