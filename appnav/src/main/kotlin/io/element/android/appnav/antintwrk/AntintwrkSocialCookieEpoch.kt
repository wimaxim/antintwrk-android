// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/** Serializes cookie writes against account changes and logout, including cancelled IO. */
internal object AntintwrkSocialCookieEpoch {
    val monitor = Any()
    var generation = 0L
        private set
    private var clearing = CompletableDeferred<Unit>().apply { complete(Unit) }
    fun beginClear(): CompletableDeferred<Unit> = synchronized(monitor) {
        advance()
        CompletableDeferred<Unit>().also { clearing = it }
    }
    suspend fun awaitClear() {
        while (true) {
            val pending = synchronized(monitor) { clearing }
            try {
                withTimeout(5_000) { pending.await() }
            } catch (_: TimeoutCancellationException) {
                throw java.io.IOException("Cookie cleanup did not complete")
            }
            if (synchronized(monitor) { clearing === pending }) return
        }
    }
    fun advance() = synchronized(monitor) { ++generation }
    fun current() = synchronized(monitor) { generation }
    fun check(expected: Long) {
        if (generation != expected) throw CancellationException("Social operation superseded")
    }
}
