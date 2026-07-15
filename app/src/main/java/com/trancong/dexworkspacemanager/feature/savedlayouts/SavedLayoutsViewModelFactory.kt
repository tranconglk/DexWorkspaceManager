package com.trancong.dexworkspacemanager.feature.savedlayouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.trancong.dexworkspacemanager.domain.repository.WorkspaceRepository
import com.trancong.dexworkspacemanager.platform.transfer.WorkspaceTransferDirectory
import com.trancong.dexworkspacemanager.platform.installedapps.WorkspaceAppsAvailabilityChecker
import com.trancong.dexworkspacemanager.platform.installedapps.PackageChangeMonitor

class SavedLayoutsViewModelFactory(
    private val workspaceRepository: WorkspaceRepository,
    private val workspaceTransferDirectory: WorkspaceTransferDirectory,
    private val availabilityChecker: WorkspaceAppsAvailabilityChecker,
    private val packageChangeMonitor: PackageChangeMonitor
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: CreationExtras
    ): T {
        if (modelClass.isAssignableFrom(SavedLayoutsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SavedLayoutsViewModel(
                workspaceRepository,
                workspaceTransferDirectory,
                availabilityChecker,
                packageChangeMonitor
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
