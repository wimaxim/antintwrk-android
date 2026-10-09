// Copyright 2026 Maxim Novichkov
// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.x

internal object AntintwrkBillingRuntime {
    fun attach(activity: MainActivity) {
        activity.lifecycle.addObserver(AntintwrkPlayBilling.create(activity.applicationContext))
    }
}
