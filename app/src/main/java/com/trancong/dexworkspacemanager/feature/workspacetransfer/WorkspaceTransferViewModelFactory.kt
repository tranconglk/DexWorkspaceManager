package com.trancong.dexworkspacemanager.feature.workspacetransfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.trancong.dexworkspacemanager.domain.repository.WorkspaceRepository
import com.trancong.dexworkspacemanager.platform.transfer.WorkspaceTransferDirectory

class WorkspaceTransferViewModelFactory(
    private val repository: WorkspaceRepository,
    private val transferDirectory: WorkspaceTransferDirectory
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(WorkspaceTransferViewModel::class.java))
        return WorkspaceTransferViewModel(repository, transferDirectory) as T
    }
}
