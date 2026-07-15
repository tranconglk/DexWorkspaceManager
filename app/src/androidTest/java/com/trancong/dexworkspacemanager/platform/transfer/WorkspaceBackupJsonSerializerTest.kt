package com.trancong.dexworkspacemanager.platform.transfer

import com.trancong.dexworkspacemanager.domain.model.Workspace
import com.trancong.dexworkspacemanager.domain.model.WorkspaceAppAssignment
import com.trancong.dexworkspacemanager.feature.layouteditor.LayoutTemplate
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceBackupJsonSerializerTest {
    private val serializer = DefaultWorkspaceBackupJsonSerializer(DefaultWorkspaceJsonSerializer())

    @Test fun multipleWorkspaces_roundTripPreservesFavoritesAndAssignments() {
        val source = listOf(workspace(1, true), workspace(2, false))
        val result = serializer.deserialize(serializer.serialize(source, exportedAt = 1234))
        assertEquals(1234, result.exportedAt)
        assertEquals(2, result.workspaces.size)
        assertTrue(result.workspaces[0].isFavorite)
        assertFalse(result.workspaces[1].isFavorite)
        assertEquals("pkg.2", result.workspaces[1].appAssignments.single().packageName)
    }

    @Test(expected = UnsupportedBackupVersionException::class)
    fun unsupportedBackupVersion_isRejected() {
        val root = JSONObject(serializer.serialize(listOf(workspace(1, false)), 1))
        serializer.deserialize(root.put("backupSchemaVersion", 999).toString())
    }

    @Test(expected = IllegalArgumentException::class)
    fun oneInvalidWorkspace_rejectsWholeBackup() {
        val root = JSONObject(serializer.serialize(listOf(workspace(1, false), workspace(2, true)), 1))
        root.getJSONArray("workspaces").getJSONObject(1).put("template", "INVALID")
        serializer.deserialize(root.toString())
    }

    private fun workspace(id: Long, favorite: Boolean) = Workspace(
        id,
        "Workspace $id",
        LayoutTemplate.TWO_ZONES,
        0.65f,
        0.5f,
        10,
        20,
        listOf(WorkspaceAppAssignment("zone_1", "pkg.$id", "pkg.$id.Main", "App $id", 0)),
        400,
        favorite
    )
}
