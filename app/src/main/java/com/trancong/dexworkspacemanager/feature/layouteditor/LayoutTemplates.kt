package com.trancong.dexworkspacemanager.feature.layouteditor

object LayoutTemplates {
    fun zonesFor(
        template: LayoutTemplate,
        leftRatio: Float = 0.65f
    ): List<LayoutZone> {
        val coercedLeftRatio = leftRatio.coerceIn(0.4f, 0.8f)

        return when (template) {
        LayoutTemplate.TWO_ZONES -> listOf(
            LayoutZone(
                id = "zone_1",
                label = "Vùng 1",
                x = 0f,
                y = 0f,
                width = 0.5f,
                height = 1f
            ),
            LayoutZone(
                id = "zone_2",
                label = "Vùng 2",
                x = 0.5f,
                y = 0f,
                width = 0.5f,
                height = 1f
            )
        )

        LayoutTemplate.THREE_ZONES -> listOf(
            LayoutZone(
                id = "zone_1",
                label = "Vùng 1",
                x = 0f,
                y = 0f,
                width = coercedLeftRatio,
                height = 1f
            ),
            LayoutZone(
                id = "zone_2",
                label = "Vùng 2",
                x = coercedLeftRatio,
                y = 0f,
                width = 1f - coercedLeftRatio,
                height = 0.5f
            ),
            LayoutZone(
                id = "zone_3",
                label = "Vùng 3",
                x = coercedLeftRatio,
                y = 0.5f,
                width = 1f - coercedLeftRatio,
                height = 0.5f
            )
        )

        LayoutTemplate.EMPTY -> emptyList()
        }
    }
}
