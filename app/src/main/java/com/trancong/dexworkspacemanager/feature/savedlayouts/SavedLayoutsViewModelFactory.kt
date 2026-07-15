package com.trancong.dexworkspacemanager.feature.savedlayouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.trancong.dexworkspacemanager.domain.repository.WorkspaceRepository
import com.trancong.dexworkspacemanager.platform.transfer.WorkspaceTransferDirectory

class SavedLayoutsViewModelFactory(
    private val workspaceRepository: WorkspaceRepository,
    private val workspaceTransferDirectory: WorkspaceTransferDirectory
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: CreationExtras
    ): T {
        if (modelClass.isAssignableFrom(SavedLayoutsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SavedLayoutsViewModel(workspaceRepository, workspaceTransferDirectory) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
