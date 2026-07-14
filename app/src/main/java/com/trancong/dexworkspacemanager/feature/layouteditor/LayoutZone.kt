package com.trancong.dexworkspacemanager.feature.layouteditor

data class LayoutZone(
    val id: String,
    val label: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
) {
    init {
        require(x in 0f..1f) { "x must be between 0 and 1, but was $x" }
        require(y in 0f..1f) { "y must be between 0 and 1, but was $y" }
        require(width > 0f && width <= 1f) {
            "width must be greater than 0 and at most 1, but was $width"
        }
        require(height > 0f && height <= 1f) {
            "height must be greater than 0 and at most 1, but was $height"
        }
        require(x + width <= 1f) {
            "x + width must not exceed 1, but was ${x + width}"
        }
        require(y + height <= 1f) {
            "y + height must not exceed 1, but was ${y + height}"
        }
    }
}
