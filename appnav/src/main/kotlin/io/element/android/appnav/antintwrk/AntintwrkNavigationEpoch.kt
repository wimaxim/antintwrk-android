// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

/** A late navigation result cannot surface an error for a newer intent or a stopped activity. */
internal class AntintwrkNavigationEpoch {
    var current: Long = 0
        private set

    fun advance(): Long = ++current

    fun accepts(epoch: Long, foreground: Boolean): Boolean = foreground && epoch == current
}
