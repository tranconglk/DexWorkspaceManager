package com.trancong.dexworkspacemanager.feature.layouteditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.trancong.dexworkspacemanager.domain.repository.WorkspaceRepository
import com.trancong.dexworkspacemanager.platform.applauncher.AppLauncher

class LayoutEditorViewModelFactory(
    private val workspaceRepository: WorkspaceRepository,
    private val appLauncher: AppLauncher,
    private val workspaceId: Long?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: CreationExtras
    ): T {
        if (modelClass.isAssignableFrom(LayoutEditorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LayoutEditorViewModel(
                workspaceRepository = workspaceRepository,
                appLauncher = appLauncher,
                workspaceId = workspaceId
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
