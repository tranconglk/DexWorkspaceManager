package com.trancong.dexworkspacemanager.feature.layouteditor

object LayoutTemplates {
    fun zonesFor(template: LayoutTemplate): List<LayoutZone> = when (template) {
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
                width = 0.65f,
                height = 1f
            ),
            LayoutZone(
                id = "zone_2",
                label = "Vùng 2",
                x = 0.65f,
                y = 0f,
                width = 0.35f,
                height = 0.5f
            ),
            LayoutZone(
                id = "zone_3",
                label = "Vùng 3",
                x = 0.65f,
                y = 0.5f,
                width = 0.35f,
                height = 0.5f
            )
        )

        LayoutTemplate.EMPTY -> emptyList()
    }
}
