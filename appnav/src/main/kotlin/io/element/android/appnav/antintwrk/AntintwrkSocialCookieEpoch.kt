// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import kotlinx.coroutines.CancellationException

/** Serializes cookie writes against account changes and logout, including cancelled IO. */
internal object AntintwrkSocialCookieEpoch {
    val monitor = Any()
    var generation = 0L
        private set
    fun advance() = synchronized(monitor) { ++generation }
    fun current() = synchronized(monitor) { generation }
    fun check(expected: Long) {
        if (generation != expected) throw CancellationException("Social operation superseded")
    }
}
