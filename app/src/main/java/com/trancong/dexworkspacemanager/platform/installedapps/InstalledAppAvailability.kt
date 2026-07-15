package com.trancong.dexworkspacemanager.platform.installedapps

sealed interface InstalledAppAvailability {
    data object Available : InstalledAppAvailability
    data object PackageMissing : InstalledAppAvailability
    data object ActivityMissing : InstalledAppAvailability
    data object Disabled : InstalledAppAvailability
    data class UnknownError(val message: String? = null) : InstalledAppAvailability
}
