package com.trancong.dexworkspacemanager.platform.dex

data class ExternalDisplayInfo(
    val id: Int,
    val name: String,
    val width: Int,
    val height: Int,
    val densityDpi: Int,
    val isDefaultDisplay: Boolean,
    val isLikelyDexDisplay: Boolean
)
