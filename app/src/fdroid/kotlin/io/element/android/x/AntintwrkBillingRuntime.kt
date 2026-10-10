// Copyright 2026 Maxim Novichkov
// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.x

// This distribution does not include the proprietary Google Play Billing SDK.
internal object AntintwrkBillingRuntime {
    fun attach(
        @Suppress("UNUSED_PARAMETER") activity: MainActivity,
        @Suppress("UNUSED_PARAMETER") canLaunch: () -> Boolean,
    ): io.element.android.appnav.antintwrk.AntintwrkStore? = null
}
