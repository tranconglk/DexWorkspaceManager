package com.trancong.dexworkspacemanager.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.trancong.dexworkspacemanager.domain.repository.WorkspaceRepository
import com.trancong.dexworkspacemanager.domain.repository.AppPreferencesRepository
import com.trancong.dexworkspacemanager.platform.installedapps.WorkspaceAppsAvailabilityChecker
import com.trancong.dexworkspacemanager.platform.installedapps.PackageChangeMonitor

class HomeViewModelFactory(
    private val workspaceRepository: WorkspaceRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val availabilityChecker: WorkspaceAppsAvailabilityChecker,
    private val packageChangeMonitor: PackageChangeMonitor
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return HomeViewModel(
            workspaceRepository,
            appPreferencesRepository,
            availabilityChecker,
            packageChangeMonitor
        ) as T
    }
}
