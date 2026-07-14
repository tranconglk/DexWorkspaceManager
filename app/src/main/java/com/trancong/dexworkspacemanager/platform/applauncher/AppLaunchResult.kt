package com.trancong.dexworkspacemanager.platform.applauncher

sealed interface AppLaunchResult {
    data object Success : AppLaunchResult
    data object AppNotFound : AppLaunchResult
    data object ActivityNotFound : AppLaunchResult
    data object SecurityError : AppLaunchResult
    data class UnknownError(val message: String?) : AppLaunchResult
}
