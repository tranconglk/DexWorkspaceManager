package com.trancong.dexworkspacemanager.feature.layouteditor

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LayoutEditorViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LayoutEditorUiState())
    val uiState: StateFlow<LayoutEditorUiState> = _uiState.asStateFlow()

    fun selectTemplate(template: LayoutTemplate) {
        _uiState.update { currentState ->
            currentState.copy(selectedTemplate = template)
        }
    }

    fun updateLeftRatio(ratio: Float) {
        _uiState.update { currentState ->
            currentState.copy(leftRatio = ratio.coerceIn(0.4f, 0.8f))
        }
    }

    fun updateTopRatio(ratio: Float) {
        _uiState.update { currentState ->
            currentState.copy(topRatio = ratio.coerceIn(0.25f, 0.75f))
        }
    }

    fun resetLayout() {
        _uiState.update { currentState ->
            currentState.copy(selectedTemplate = LayoutTemplate.EMPTY)
        }
    }
}
