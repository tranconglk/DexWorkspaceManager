package com.trancong.dexworkspacemanager.platform.transfer

import com.trancong.dexworkspacemanager.domain.model.Workspace
import com.trancong.dexworkspacemanager.domain.model.WorkspaceAppAssignment
import com.trancong.dexworkspacemanager.feature.layouteditor.LayoutTemplate
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class DefaultWorkspaceJsonSerializer : WorkspaceJsonSerializer {
    override fun serialize(workspace: Workspace): String {
        val assignments = JSONArray()
        workspace.appAssignments.sortedWith(
            compareBy<WorkspaceAppAssignment> { it.launchOrder }.thenBy { it.zoneId }
        ).forEach { assignment ->
            assignments.put(
                JSONObject()
                    .put("zoneId", assignment.zoneId)
                    .put("packageName", assignment.packageName)
                    .put("activityName", assignment.activityName)
                    .put("appLabel", assignment.appLabel)
                    .put("launchOrder", assignment.launchOrder)
            )
        }
        val workspaceJson = JSONObject()
            .put("name", workspace.name)
            .put("template", workspace.template.name)
            .put("leftRatio", workspace.leftRatio.toDouble())
            .put("topRatio", workspace.topRatio.toDouble())
            .put("launchDelayMs", workspace.launchDelayMs)
            .put("isFavorite", workspace.isFavorite)
            .put("appAssignments", assignments)
        return JSONObject()
            .put("schemaVersion", WorkspaceTransferContract.CURRENT_SCHEMA_VERSION)
            .put("workspace", workspaceJson)
            .toString(2)
    }

    override fun deserialize(json: String): WorkspaceTransferResult {
        return try {
        val root = JSONObject(json)
        if (!root.has("schemaVersion")) return invalid("Thiếu schemaVersion")
        val version = root.getInt("schemaVersion")
        if (version != WorkspaceTransferContract.CURRENT_SCHEMA_VERSION) {
            return WorkspaceTransferResult.UnsupportedVersion(version)
        }
        val source = root.getJSONObject("workspace")
        val name = source.getString("name").trim()
        if (name.isEmpty()) return invalid("Tên workspace không được trống")
        val template = runCatching {
            LayoutTemplate.valueOf(source.getString("template"))
        }.getOrElse { return invalid("Template không hợp lệ") }
        val leftRatio = source.getDouble("leftRatio").toFloat()
        val topRatio = source.getDouble("topRatio").toFloat()
        val delay = source.getLong("launchDelayMs")
        if (leftRatio !in 0.4f..0.8f) return invalid("leftRatio không hợp lệ")
        if (topRatio !in 0.25f..0.75f) return invalid("topRatio không hợp lệ")
        if (delay !in 0L..5_000L) return invalid("launchDelayMs không hợp lệ")

        val allowedZones = when (template) {
            LayoutTemplate.TWO_ZONES -> setOf("zone_1", "zone_2")
            LayoutTemplate.THREE_ZONES -> setOf("zone_1", "zone_2", "zone_3")
            LayoutTemplate.EMPTY -> emptySet()
        }
        val array = source.getJSONArray("appAssignments")
        if (template == LayoutTemplate.EMPTY && array.length() > 0) {
            return invalid("Template EMPTY không được chứa ứng dụng")
        }
        val parsed = buildList {
            repeat(array.length()) { index ->
                val item = array.getJSONObject(index)
                val zoneId = item.getString("zoneId").trim()
                val packageName = item.getString("packageName").trim()
                val activityName = item.getString("activityName").trim()
                val appLabel = item.getString("appLabel").trim()
                val launchOrder = item.getInt("launchOrder")
                if (zoneId.isEmpty() || packageName.isEmpty() ||
                    activityName.isEmpty() || appLabel.isEmpty()
                ) return invalid("Thông tin ứng dụng không được trống")
                if (zoneId !in allowedZones) return invalid("Vùng $zoneId không hợp lệ cho template")
                if (launchOrder < 0) return invalid("launchOrder không được âm")
                add(WorkspaceAppAssignment(zoneId, packageName, activityName, appLabel, launchOrder))
            }
        }
        if (parsed.map { it.zoneId }.distinct().size != parsed.size) {
            return invalid("appAssignments bị trùng zoneId")
        }
        val normalized = parsed.sortedWith(
            compareBy<WorkspaceAppAssignment> { it.launchOrder }.thenBy { it.zoneId }
        ).mapIndexed { index, assignment -> assignment.copy(launchOrder = index) }
        val now = System.currentTimeMillis()
        WorkspaceTransferResult.Success(
            fileName = "",
            workspace = Workspace(
                id = 0,
                name = name,
                template = template,
                leftRatio = leftRatio,
                topRatio = topRatio,
                createdAt = now,
                updatedAt = now,
                appAssignments = normalized,
                launchDelayMs = delay,
                isFavorite = false
            )
        )
        } catch (exception: JSONException) {
            invalid(exception.message ?: "JSON không hợp lệ")
        } catch (exception: IllegalArgumentException) {
            invalid(exception.message ?: "Dữ liệu không hợp lệ")
        }
    }

    private fun invalid(message: String) = WorkspaceTransferResult.InvalidData(message)
}
