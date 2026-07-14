package com.trancong.dexworkspacemanager.platform.dex

data class DexWorkArea(
    val width: Int,
    val height: Int,
    val originX: Int = 0,
    val originY: Int = 0,
    val bottomInset: Int = 0
) {
    init {
        require(width > 0) { "width must be positive" }
        require(height > 0) { "height must be positive" }
        require(originX >= 0) { "originX must not be negative" }
        require(originY >= 0) { "originY must not be negative" }
        require(bottomInset in 0 until height) { "bottomInset must be within the work area" }
    }

    val usableHeight: Int get() = height - bottomInset
}
