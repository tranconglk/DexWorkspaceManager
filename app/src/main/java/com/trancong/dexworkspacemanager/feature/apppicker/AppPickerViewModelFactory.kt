package com.trancong.dexworkspacemanager.feature.apppicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.trancong.dexworkspacemanager.platform.installedapps.InstalledAppsProvider

class AppPickerViewModelFactory(
    private val installedAppsProvider: InstalledAppsProvider
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AppPickerViewModel::class.java))
        return AppPickerViewModel(installedAppsProvider) as T
    }
}
