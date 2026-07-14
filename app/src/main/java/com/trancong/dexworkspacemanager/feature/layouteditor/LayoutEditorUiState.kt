package com.trancong.dexworkspacemanager.feature.layouteditor

data class LayoutEditorUiState(
    val selectedTemplate: LayoutTemplate = LayoutTemplate.THREE_ZONES,
    val leftRatio: Float = 0.65f,
    val topRatio: Float = 0.5f
)
