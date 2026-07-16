import org.cyclonedx.Version
import org.cyclonedx.gradle.CyclonedxDirectTask
import org.cyclonedx.model.Component

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.cyclonedx)
}

android {
    namespace = "com.trancong.dexworkspacemanager"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.trancong.dexworkspacemanager"
        minSdk = 28
        targetSdk = 37
        versionCode = 2
        versionName = "0.1.1-test"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = false
            isShrinkResources = false
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    lint {
        checkReleaseBuilds = true
        abortOnError = true
        htmlReport = true
        xmlReport = true
        textReport = true
    }
}

val lockedConfigurationNames = setOf(
    "androidLintTool",
    "debugAndroidTestCompileClasspath",
    "debugAndroidTestRuntimeClasspath",
    "debugCompileClasspath",
    "debugRuntimeClasspath",
    "debugUnitTestCompileClasspath",
    "debugUnitTestRuntimeClasspath",
    "kspDebugKotlinProcessorClasspath",
    "kspReleaseKotlinProcessorClasspath",
    "releaseCompileClasspath",
    "releaseRuntimeClasspath",
)

configurations.configureEach {
    if (name in lockedConfigurationNames) {
        resolutionStrategy.activateDependencyLocking()
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

tasks.register("printReleaseInfo") {
    group = "help"
    description = "Prints release metadata for the release manifest script."
    notCompatibleWithConfigurationCache("Reads Android extension metadata at execution time.")
    doLast {
        println("VERSION_NAME=${android.defaultConfig.versionName}")
        println("VERSION_CODE=${android.defaultConfig.versionCode}")
        println("APPLICATION_ID=${android.defaultConfig.applicationId}")
        println("COMPILE_SDK=${android.compileSdk}")
        println("MIN_SDK=${android.defaultConfig.minSdk}")
        println("TARGET_SDK=${android.defaultConfig.targetSdk}")
        println("AGP_VERSION=${libs.versions.agp.get()}")
        println("KOTLIN_VERSION=${libs.versions.kotlin.get()}")
    }
}

val releaseSbomFileName = "dex-workspace-manager-v${android.defaultConfig.versionName}.cdx.json"

tasks.named<CyclonedxDirectTask>("cyclonedxDirectBom") {
    includeConfigs.set(listOf("releaseRuntimeClasspath"))
    projectType.set(Component.Type.APPLICATION)
    schemaVersion.set(Version.VERSION_16)
    componentName.set(rootProject.name)
    componentGroup.set(android.defaultConfig.applicationId)
    componentVersion.set(android.defaultConfig.versionName)
    includeBomSerialNumber.set(true)
    includeBuildEnvironment.set(false)
    includeBuildSystem.set(false)
    includeLicenseText.set(false)
    jsonOutput.set(layout.buildDirectory.file("reports/sbom/$releaseSbomFileName"))
    xmlOutput.unsetConvention()
}

tasks.register("generateReleaseSbom") {
    group = "reporting"
    description = "Generates the release runtime dependency SBOM in CycloneDX JSON format."
    dependsOn("cyclonedxDirectBom")
}
