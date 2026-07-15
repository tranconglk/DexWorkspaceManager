package com.trancong.dexworkspacemanager.platform.installedapps

import com.trancong.dexworkspacemanager.domain.model.WorkspaceAppAssignment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspaceAppsAvailabilityCheckerTest {
    @Test fun allAvailable_areMappedByZoneId() = runBlocking {
        val result = checker(mapOf("pkg.a" to InstalledAppAvailability.Available)).check(
            listOf(assignment("zone_1", "pkg.a"), assignment("zone_2", "pkg.a"))
        )
        assertEquals(InstalledAppAvailability.Available, result["zone_1"])
        assertEquals(InstalledAppAvailability.Available, result["zone_2"])
    }

    @Test fun eachFailureState_isPreserved() = runBlocking {
        val expected = mapOf(
            "missing" to InstalledAppAvailability.PackageMissing,
            "activity" to InstalledAppAvailability.ActivityMissing,
            "disabled" to InstalledAppAvailability.Disabled,
            "unknown" to InstalledAppAvailability.UnknownError("safe")
        )
        val result = checker(expected).check(expected.keys.mapIndexed { index, pkg ->
            assignment("zone_$index", pkg)
        })
        expected.keys.forEachIndexed { index, pkg -> assertEquals(expected[pkg], result["zone_$index"]) }
    }

    @Test(expected = CancellationException::class)
    fun cancellation_isNotSwallowed() {
        runBlocking {
            WorkspaceAppsAvailabilityChecker(
                FakeInstalledAppsProvider(emptyMap(), cancelPackage = "cancel")
            ).check(listOf(assignment("zone_1", "cancel")))
        }
    }

    private fun checker(results: Map<String, InstalledAppAvailability>) =
        WorkspaceAppsAvailabilityChecker(FakeInstalledAppsProvider(results))

    private fun assignment(zone: String, pkg: String) =
        WorkspaceAppAssignment(zone, pkg, "$pkg.Main", pkg, 0)
}

private class FakeInstalledAppsProvider(
    private val results: Map<String, InstalledAppAvailability>,
    private val cancelPackage: String? = null
) : InstalledAppsProvider {
    override suspend fun getLaunchableApps(): List<InstalledApp> = emptyList()

    override suspend fun checkAvailability(
        packageName: String,
        activityName: String
    ): InstalledAppAvailability {
        if (packageName == cancelPackage) throw CancellationException("cancelled")
        return results.getValue(packageName)
    }
}
