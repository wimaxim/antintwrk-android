import extension.buildConfigFieldStr
import extension.readLocalProperty
import extension.setupDependencyInjection
import extension.testCommonDependencies

/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

plugins {
    id("io.element.android-compose-library")
    id("kotlin-parcelize")
    alias(libs.plugins.kotlin.serialization)
}

abstract class PrepareAntintwrkCallOverlay : Exec() {
    @get:OutputDirectory
    abstract val assetsDirectory: DirectoryProperty
}

val embeddedCallArtifact = configurations.create("antintwrkEmbeddedCallArtifact") {
    isTransitive = false
}
dependencies.add(embeddedCallArtifact.name, "${libs.element.call.embedded.get()}@aar")
val callOverlayAssets = layout.buildDirectory.dir("generated/antintwrkCall/assets")
val prepareCallOverlay = tasks.register<PrepareAntintwrkCallOverlay>("prepareAntintwrkCallOverlay") {
    val patchScript = rootProject.file("tools/antintwrk-call/patch_embedded_call.py")
    inputs.files(embeddedCallArtifact, patchScript)
    assetsDirectory.set(callOverlayAssets)
    commandLine(
        "python3",
        patchScript.absolutePath,
        "--version",
        "0.26.0",
        "--aar",
        embeddedCallArtifact.singleFile.absolutePath,
        "--output",
        callOverlayAssets.get().asFile.absolutePath,
    )
}
androidComponents.onVariants { variant ->
    variant.sources.assets?.addGeneratedSourceDirectory(prepareCallOverlay, PrepareAntintwrkCallOverlay::assetsDirectory)
}

android {
    namespace = "io.element.android.features.call.impl"

    buildFeatures {
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    defaultConfig {
        buildConfigFieldStr(
            name = "SENTRY_DSN",
            value = System.getenv("ELEMENT_CALL_SENTRY_DSN")
                ?: readLocalProperty("features.call.sentry.dsn")
                ?: ""
        )
        buildConfigFieldStr(
            name = "POSTHOG_USER_ID",
            value = System.getenv("ELEMENT_CALL_POSTHOG_USER_ID")
                ?: readLocalProperty("features.call.posthog.userid")
                ?: ""
        )
        buildConfigFieldStr(
            name = "POSTHOG_API_HOST",
            value = System.getenv("ELEMENT_CALL_POSTHOG_API_HOST")
                ?: readLocalProperty("features.call.posthog.api.host")
                ?: ""
        )
        buildConfigFieldStr(
            name = "POSTHOG_API_KEY",
            value = System.getenv("ELEMENT_CALL_POSTHOG_API_KEY")
                ?: readLocalProperty("features.call.posthog.api.key")
                ?: ""
        )
        buildConfigFieldStr(
            name = "RAGESHAKE_URL",
            value = System.getenv("ELEMENT_CALL_RAGESHAKE_URL")
                ?: readLocalProperty("features.call.regeshake.url")
                ?: ""
        )
    }
}

setupDependencyInjection()

dependencies {
    implementation(projects.appconfig)
    implementation(projects.features.enterprise.api)
    implementation(projects.libraries.architecture)
    implementation(projects.libraries.androidutils)
    implementation(projects.libraries.audio.api)
    implementation(projects.libraries.core)
    implementation(projects.libraries.designsystem)
    implementation(projects.libraries.featureflag.api)
    implementation(projects.libraries.matrix.api)
    implementation(projects.libraries.matrixmedia.api)
    implementation(projects.libraries.network)
    implementation(projects.libraries.preferences.api)
    implementation(projects.libraries.push.api)
    implementation(projects.libraries.uiStrings)
    implementation(projects.services.analytics.api)
    implementation(projects.services.appnavstate.api)
    implementation(projects.services.toolbox.api)
    implementation(libs.androidx.webkit)
    implementation(libs.coil.compose)
    implementation(libs.serialization.json)
    api(projects.features.call.api)

    testCommonDependencies(libs, true)
    testImplementation(projects.features.call.test)
    testImplementation(projects.libraries.featureflag.test)
    testImplementation(projects.libraries.preferences.test)
    testImplementation(projects.libraries.matrix.test)
    testImplementation(projects.libraries.matrixmedia.test)
    testImplementation(projects.libraries.push.test)
    testImplementation(projects.services.analytics.test)
    testImplementation(projects.services.appnavstate.impl)
    testImplementation(projects.services.appnavstate.test)
    testImplementation(projects.services.toolbox.test)
}
