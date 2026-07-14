package com.trancong.dexworkspacemanager.platform.dex

sealed interface DexDisplayState {
    data object NotConnected : DexDisplayState
    data class Connected(val display: ExternalDisplayInfo) : DexDisplayState
    data class MultipleDisplays(val displays: List<ExternalDisplayInfo>) : DexDisplayState
    data class Error(val message: String) : DexDisplayState
}
