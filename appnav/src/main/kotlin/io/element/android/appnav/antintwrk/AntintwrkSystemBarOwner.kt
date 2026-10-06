/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

/** Loading and welcome can overlap during an Appyx transition. */
internal class AntintwrkSystemBarOwner {
    private var activeScopes = 0
    private var restoreStyle: () -> Unit = {}

    fun acquire(onRestore: () -> Unit): () -> Unit {
        activeScopes++
        restoreStyle = onRestore
        var active = true
        return {
            if (active) {
                active = false
                activeScopes--
                if (activeScopes == 0) restoreStyle()
            }
        }
    }
}
