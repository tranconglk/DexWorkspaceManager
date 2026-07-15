package com.trancong.dexworkspacemanager.platform.transfer

import com.trancong.dexworkspacemanager.domain.model.Workspace
import com.trancong.dexworkspacemanager.domain.model.WorkspaceAppAssignment
import com.trancong.dexworkspacemanager.feature.layouteditor.LayoutTemplate
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceJsonSerializerTest {
    private val serializer = DefaultWorkspaceJsonSerializer()

    @Test fun workspace_roundTrip_preservesLogicalDataAndNormalizesOrder() {
        val source = workspace(
            assignments = listOf(assignment("zone_2", 8), assignment("zone_1", 3))
        )
        val result = serializer.deserialize(serializer.serialize(source)) as WorkspaceTransferResult.Success
        val restored = requireNotNull(result.workspace)
        assertEquals(source.name, restored.name)
        assertEquals(source.template, restored.template)
        assertEquals(source.leftRatio, restored.leftRatio)
        assertEquals(source.topRatio, restored.topRatio)
        assertEquals(source.launchDelayMs, restored.launchDelayMs)
        assertEquals(listOf(0, 1), restored.appAssignments.map { it.launchOrder })
        assertEquals(setOf("zone_1", "zone_2"), restored.appAssignments.map { it.zoneId }.toSet())
    }

    @Test fun unsupportedSchemaVersion_isRejected() {
        val json = JSONObject(serializer.serialize(workspace())).put("schemaVersion", 999).toString()
        assertTrue(serializer.deserialize(json) is WorkspaceTransferResult.UnsupportedVersion)
    }

    @Test fun missingWorkspace_isRejected() {
        val json = JSONObject().put("schemaVersion", WorkspaceTransferContract.CURRENT_SCHEMA_VERSION).toString()
        assertTrue(serializer.deserialize(json) is WorkspaceTransferResult.InvalidData)
    }

    @Test fun invalidTemplate_isRejected() = assertInvalid { it.put("template", "INVALID") }

    @Test fun duplicateZone_isRejected() {
        val source = workspace(assignments = listOf(assignment("zone_1", 0), assignment("zone_2", 1)))
        val root = JSONObject(serializer.serialize(source))
        root.getJSONObject("workspace").getJSONArray("appAssignments")
            .getJSONObject(1).put("zoneId", "zone_1")
        assertTrue(serializer.deserialize(root.toString()) is WorkspaceTransferResult.InvalidData)
    }

    @Test fun zoneThreeInTwoZoneTemplate_isRejected() {
        val root = JSONObject(serializer.serialize(workspace()))
        root.getJSONObject("workspace").getJSONArray("appAssignments")
            .getJSONObject(0).put("zoneId", "zone_3")
        assertTrue(serializer.deserialize(root.toString()) is WorkspaceTransferResult.InvalidData)
    }

    @Test fun assignmentInEmptyTemplate_isRejected() = assertInvalid { it.put("template", "EMPTY") }

    @Test fun malformedJson_isRejected() {
        assertTrue(serializer.deserialize("{broken") is WorkspaceTransferResult.InvalidData)
    }

    private fun assertInvalid(change: (JSONObject) -> Unit) {
        val root = JSONObject(serializer.serialize(workspace()))
        change(root.getJSONObject("workspace"))
        assertTrue(serializer.deserialize(root.toString()) is WorkspaceTransferResult.InvalidData)
    }

    private fun workspace(assignments: List<WorkspaceAppAssignment> = listOf(assignment("zone_1", 0))) =
        Workspace(7, "Test", LayoutTemplate.TWO_ZONES, 0.65f, 0.5f, 10, 20, assignments, 700, true)

    private fun assignment(zone: String, order: Int) =
        WorkspaceAppAssignment(zone, "pkg.$zone", "pkg.$zone.Main", zone, order)
}
