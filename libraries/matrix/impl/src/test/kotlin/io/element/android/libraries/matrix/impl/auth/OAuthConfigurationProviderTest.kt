/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.auth

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.auth.OAuthConfig
import io.element.android.libraries.matrix.test.auth.FAKE_REDIRECT_URL
import io.element.android.libraries.matrix.test.auth.FakeOAuthRedirectUrlProvider
import io.element.android.libraries.matrix.test.core.aBuildMeta
import org.junit.Test
import java.net.URI

class OAuthConfigurationProviderTest {
    @Test
    fun `managed release and debug callbacks belong to the advertised domain`() {
        for (callback in listOf("com.antintwrk.app:/", "com.antintwrk.app.debug:/")) {
            val config = OAuthConfigurationProvider(aBuildMeta(applicationName = "antintwrk"), FakeOAuthRedirectUrlProvider(callback))
                .configuration(managed = true)
            val domainScheme = URI(config.clientUri).host.split('.').reversed().joinToString(".")
            assertThat(URI(config.redirectUri).scheme).startsWith("$domainScheme.")
            assertThat(config.redirectUri).isEqualTo(callback)
            for (link in listOf(config.logoUri, config.tosUri, config.policyUri)) {
                assertThat(URI(link).host).isEqualTo("antintwrk.com")
                assertThat(URI(link).scheme).isEqualTo("https")
            }
            assertThat(config.staticRegistrations).isEmpty()
        }
    }

    @Test
    fun `unmanaged client preserves upstream registration metadata`() {
        val config = OAuthConfigurationProvider(aBuildMeta(), FakeOAuthRedirectUrlProvider()).configuration(managed = false)
        assertThat(config.clientUri).isEqualTo(OAuthConfig.CLIENT_URI)
        assertThat(config.staticRegistrations).isEqualTo(OAuthConfig.STATIC_REGISTRATIONS)
    }

    @Test
    fun get() {
        val result = OAuthConfigurationProvider(
            buildMeta = aBuildMeta(
                applicationName = "myName",
            ),
            oAuthRedirectUrlProvider = FakeOAuthRedirectUrlProvider(),
        ).get()
        assertThat(result.clientName).isEqualTo("myName")
        assertThat(result.redirectUri).isEqualTo(FAKE_REDIRECT_URL)
    }
}
