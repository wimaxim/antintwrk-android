// Copyright 2026 Maxim Novichkov
// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.x

import androidx.lifecycle.lifecycleScope
import io.element.android.appnav.antintwrk.AntintwrkStore

internal object AntintwrkBillingRuntime {
    fun attach(activity: MainActivity, canLaunch: () -> Boolean): AntintwrkStore {
        val transport = AntintwrkPlayBilling.create(activity.applicationContext)
        activity.lifecycle.addObserver(transport)
        return AntintwrkStoreAdapter(activity, transport, activity.lifecycleScope, canLaunch)
    }
}
