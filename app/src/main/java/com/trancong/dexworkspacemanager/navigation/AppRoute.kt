package com.trancong.dexworkspacemanager.navigation

enum class AppRoute(val route: String) {
    Home("home"),
    LayoutEditor("layout_editor?workspaceId={workspaceId}"),
    SavedLayouts("saved_layouts");

    companion object {
        const val ARG_WORKSPACE_ID = "workspaceId"

        fun layoutEditor(workspaceId: Long? = null): String =
            if (workspaceId == null) {
                "layout_editor"
            } else {
                "layout_editor?$ARG_WORKSPACE_ID=$workspaceId"
            }
    }
}
