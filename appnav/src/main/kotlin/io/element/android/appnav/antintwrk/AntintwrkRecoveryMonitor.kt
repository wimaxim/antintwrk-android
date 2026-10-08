// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import io.element.android.libraries.matrix.api.encryption.RecoveryState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** Retry unavailable custody without resetting keys; keep watching after a successful recovery. */
internal class AntintwrkRecoveryMonitor(
    private val states: StateFlow<RecoveryState>?,
    private val prepare: suspend () -> Boolean,
) {
    suspend fun run() {
        var retryMillis = 30_000L
        while (true) {
            val ready = try {
                prepare()
            } catch (_: TimeoutCancellationException) {
                false
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                false
            }
            if (ready) {
                retryMillis = 30_000L
                // Allow asynchronous SDK observers to settle before reacting to a new incomplete state.
                delay(1_000)
                if (states != null) {
                    withTimeoutOrNull(300_000) {
                        states.first { it == RecoveryState.INCOMPLETE || it == RecoveryState.DISABLED }
                    }
                } else {
                    delay(300_000)
                }
            } else {
                delay(retryMillis)
                retryMillis = (retryMillis * 2).coerceAtMost(300_000L)
            }
        }
    }
}
