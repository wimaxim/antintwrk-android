// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

/** Local, idempotent navigation only: does not create a room, send a message or alter recovery keys. */
internal suspend fun antintwrkPrepareChats(isCurrent: () -> Boolean, prepare: suspend () -> Unit) {
    repeat(5) { attempt ->
        if (!isCurrent()) throw CancellationException("Superseded chat navigation")
        try {
            withTimeout(6_000) { prepare() }
            return
        } catch (timeout: TimeoutCancellationException) {
            if (attempt == 4) throw timeout
            delay(1_000L shl attempt)
        }
    }
}
