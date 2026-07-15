package com.trancong.dexworkspacemanager.navigation

import android.net.Uri

enum class AppRoute(val route: String) {
    Home("home"),
    LayoutEditor("layout_editor?workspaceId={workspaceId}"),
    SavedLayouts("saved_layouts"),
    WorkspaceTransfer("workspace_transfer"),
    AppPicker("app_picker/{zoneId}");

    companion object {
        const val ARG_WORKSPACE_ID = "workspaceId"
        const val ARG_ZONE_ID = "zoneId"
        const val RESULT_ZONE_ID = "result_zone_id"
        const val RESULT_PACKAGE_NAME = "result_package_name"
        const val RESULT_ACTIVITY_NAME = "result_activity_name"
        const val RESULT_APP_LABEL = "result_app_label"

        fun layoutEditor(workspaceId: Long? = null): String =
            if (workspaceId == null) {
                "layout_editor"
            } else {
                "layout_editor?$ARG_WORKSPACE_ID=$workspaceId"
            }

        fun appPicker(zoneId: String): String = "app_picker/${Uri.encode(zoneId)}"
    }
}
