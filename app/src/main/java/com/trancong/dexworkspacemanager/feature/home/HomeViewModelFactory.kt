package com.trancong.dexworkspacemanager.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.trancong.dexworkspacemanager.domain.repository.WorkspaceRepository

class HomeViewModelFactory(
    private val workspaceRepository: WorkspaceRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return HomeViewModel(workspaceRepository) as T
    }
}
