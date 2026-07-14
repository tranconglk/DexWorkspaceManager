package com.trancong.dexworkspacemanager.feature.layouteditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.trancong.dexworkspacemanager.domain.repository.WorkspaceRepository

class LayoutEditorViewModelFactory(
    private val workspaceRepository: WorkspaceRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: CreationExtras
    ): T {
        if (modelClass.isAssignableFrom(LayoutEditorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LayoutEditorViewModel(workspaceRepository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
