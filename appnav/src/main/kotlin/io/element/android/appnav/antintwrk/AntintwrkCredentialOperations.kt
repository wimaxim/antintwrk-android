// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

/** Provider logout must not cancel a later account's system credential request. */
internal class AntintwrkCredentialOperations {
    private val generation = AtomicLong()
    private val lock = Mutex()

    fun cleanupToken(): Long = generation.get()

    suspend fun <T> authenticate(request: suspend () -> T): T {
        generation.incrementAndGet()
        return lock.withLock { request() }
    }

    suspend fun clear(token: Long, cleanup: suspend () -> Unit) = lock.withLock {
        if (generation.get() == token) {
            generation.incrementAndGet()
            cleanup()
        }
    }
}
