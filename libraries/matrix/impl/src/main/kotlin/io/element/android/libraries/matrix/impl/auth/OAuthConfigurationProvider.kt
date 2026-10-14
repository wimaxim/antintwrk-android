/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.auth

import dev.zacsweers.metro.Inject
import io.element.android.libraries.core.meta.BuildMeta
import io.element.android.libraries.matrix.api.BuildConfig
import io.element.android.libraries.matrix.api.auth.OAuthConfig
import io.element.android.libraries.matrix.api.auth.OAuthRedirectUrlProvider
import org.matrix.rustcomponents.sdk.OAuthConfiguration

@Inject
class OAuthConfigurationProvider(
    private val buildMeta: BuildMeta,
    private val oAuthRedirectUrlProvider: OAuthRedirectUrlProvider,
) {
    fun get(): OAuthConfiguration = configuration(BuildConfig.ANTINTWRK_NATIVE_AUTH)

    internal fun configuration(managed: Boolean): OAuthConfiguration = OAuthConfiguration(
        clientName = buildMeta.applicationName,
        redirectUri = oAuthRedirectUrlProvider.provide(),
        // MAS binds native redirect schemes to this domain. Element metadata rejects our Play callback.
        clientUri = if (managed) "https://antintwrk.com" else OAuthConfig.CLIENT_URI,
        logoUri = if (managed) "https://antintwrk.com/projects/heroworld.org/images/antintwrk-loading-mark-v1.png" else OAuthConfig.LOGO_URI,
        tosUri = if (managed) "https://antintwrk.com/en/terms?native_legal=1" else OAuthConfig.TOS_URI,
        policyUri = if (managed) "https://antintwrk.com/en/privacy?native_legal=1" else OAuthConfig.POLICY_URI,
        staticRegistrations = if (managed) emptyMap() else OAuthConfig.STATIC_REGISTRATIONS,
    )
}
