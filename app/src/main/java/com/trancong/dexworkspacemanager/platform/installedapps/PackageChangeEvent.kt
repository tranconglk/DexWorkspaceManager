package com.trancong.dexworkspacemanager.platform.installedapps

data class PackageChangeEvent(
    val packageName: String,
    val changeType: PackageChangeType
)

enum class PackageChangeType {
    ADDED,
    REMOVED,
    REPLACED,
    CHANGED
}
