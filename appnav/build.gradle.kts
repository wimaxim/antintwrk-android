/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:Suppress("UnstableApiUsage")

import extension.allFeaturesApi
import extension.setupDependencyInjection
import extension.testCommonDependencies

plugins {
    id("io.element.android-compose-library")
    id("kotlin-parcelize")
}

android {
    namespace = "io.element.android.appnav"
    testOptions { unitTests.isIncludeAndroidResources = true }
    buildFeatures { buildConfig = true }
    defaultConfig {
        buildConfigField(
            "boolean",
            "ANTINTWRK_ONBOARDING",
            providers.gradleProperty("antintwrkOnboarding").map { it.toBoolean().toString() }.orElse("false").get()
        )
        buildConfigField(
            "boolean",
            "ANTINTWRK_SHELL_PREVIEW",
            providers.gradleProperty("antintwrkShellPreview").map { it.toBoolean().toString() }.orElse("false").get()
        )
        buildConfigField("String", "ANTINTWRK_SOCIAL_ORIGIN", "\"https://antintwrk.com\"")
        buildConfigField("boolean", "ANTINTWRK_NATIVE_AUTH", providers.gradleProperty("antintwrkNativeAuth").orElse("false").get())
        buildConfigField(
            "boolean",
            "ANTINTWRK_WEB_FEED",
            providers.gradleProperty("antintwrkWebFeed").map { it.toBoolean().toString() }.orElse("false").get()
        )
    }
}

setupDependencyInjection()

dependencies {
    allFeaturesApi(project)

    implementation(projects.libraries.core)
    implementation(projects.libraries.accountselect.api)
    implementation(projects.libraries.androidutils)
    implementation(projects.libraries.architecture)
    implementation(projects.libraries.deeplink.api)
    implementation(projects.libraries.featureflag.api)
    implementation(projects.libraries.matrix.api)
    implementation(projects.libraries.oauth.api)
    implementation(projects.libraries.permissions.api)
    implementation(projects.libraries.preferences.api)
    implementation(projects.libraries.push.api)
    implementation(projects.libraries.pushproviders.api)
    implementation(projects.libraries.designsystem)
    implementation(projects.libraries.matrixui)
    implementation(projects.libraries.matrixmedia.api)
    implementation(projects.libraries.sessionStorage.api)
    implementation(projects.libraries.uiCommon)
    implementation(projects.libraries.uiStrings)
    implementation(projects.libraries.wellknown.api)
    implementation(projects.features.login.api)

    implementation(libs.coil)
    implementation(platform(libs.network.okhttp.bom))
    implementation(libs.network.okhttp)
    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.2.1")

    implementation(projects.features.announcement.api)
    implementation(projects.features.ftue.api)
    implementation(projects.features.linknewdevice.api)
    implementation(projects.features.share.api)

    implementation(projects.services.apperror.api)
    implementation(projects.services.appnavstate.api)
    implementation(projects.services.analytics.api)

    testCommonDependencies(libs, true)
    testImplementation(libs.network.mockwebserver)
    testImplementation(projects.features.login.test)
    testImplementation(projects.features.share.test)
    testImplementation(projects.libraries.accountselect.test)
    testImplementation(projects.libraries.featureflag.test)
    testImplementation(projects.libraries.matrix.test)
    testImplementation(projects.libraries.matrixmedia.test)
    testImplementation(projects.libraries.oauth.test)
    testImplementation(projects.libraries.permissions.test)
    testImplementation(projects.libraries.preferences.test)
    testImplementation(projects.libraries.push.test)
    testImplementation(projects.libraries.pushproviders.test)
    testImplementation(projects.libraries.sessionStorage.test)
    testImplementation(projects.features.announcement.test)
    testImplementation(projects.features.forward.test)
    testImplementation(projects.features.messages.test)
    testImplementation(projects.features.networkmonitor.test)
    testImplementation(projects.features.preferences.test)
    testImplementation(projects.features.rageshake.test)
    testImplementation(projects.features.signedout.test)
    testImplementation(projects.services.apperror.test)
    testImplementation(projects.services.appnavstate.test)
    testImplementation(projects.services.analytics.test)
}
