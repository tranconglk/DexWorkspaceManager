package com.trancong.dexworkspacemanager.platform.dex

interface DexDisplayProvider {
    fun getCurrentState(): DexDisplayState
    fun getExternalDisplays(): List<ExternalDisplayInfo>
}
