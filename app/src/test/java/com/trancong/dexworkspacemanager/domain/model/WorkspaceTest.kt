package com.trancong.dexworkspacemanager.domain.model

import com.trancong.dexworkspacemanager.feature.layouteditor.LayoutTemplate
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspaceTest {
    @Test(expected = IllegalArgumentException::class) fun blankName_isRejected() { workspace(name = " ") }
    @Test(expected = IllegalArgumentException::class) fun invalidLeftRatio_isRejected() { workspace(leftRatio = 0.39f) }
    @Test(expected = IllegalArgumentException::class) fun invalidTopRatio_isRejected() { workspace(topRatio = 0.76f) }
    @Test(expected = IllegalArgumentException::class) fun negativeDelay_isRejected() { workspace(delay = -1) }
    @Test(expected = IllegalArgumentException::class) fun delayAboveLimit_isRejected() { workspace(delay = 5_001) }
    @Test fun validWorkspace_isCreated() { assertEquals("Work", workspace().name) }

    private fun workspace(
        name: String = "Work",
        leftRatio: Float = 0.65f,
        topRatio: Float = 0.5f,
        delay: Long = 400
    ) = Workspace(1, name, LayoutTemplate.THREE_ZONES, leftRatio, topRatio, 10, 20, launchDelayMs = delay)
}
