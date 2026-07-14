package com.trancong.dexworkspacemanager.platform.applauncher

object KnownAppLaunchProfiles {
    private val profiles = listOf(
        AppLaunchProfile(
            packageName = "com.android.chrome",
            strategy = LaunchStrategy.STANDARD_BOUNDS
        ),
        AppLaunchProfile(
            packageName = "com.waze",
            strategy = LaunchStrategy.LEGACY_DEX_BOUNDS
        )
    ).associateBy(AppLaunchProfile::packageName)

    fun strategyFor(packageName: String): LaunchStrategy =
        profiles[packageName]?.strategy ?: LaunchStrategy.STANDARD_BOUNDS
}
