// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import java.io.IOException
import javax.net.ssl.SSLException

/** Only replay safe navigation reads; never use for sending messages, creating rooms or auth mutations. */
internal suspend fun <T> antintwrkRetryNavigationRead(isCurrent: () -> Boolean = { true }, request: suspend () -> T): T {
    repeat(5) { attempt ->
        currentCoroutineContext().ensureActive()
        if (!isCurrent()) throw CancellationException("Superseded navigation")
        try {
            return request()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            val transient = when (failure) {
                is SSLException -> false
                is IOException -> true
                is AntintwrkRequestError -> failure.httpStatus in setOf(408, 500, 502, 503, 504) &&
                    failure.code in setOf("", "UNAVAILABLE")
                else -> false
            }
            if (!transient || attempt == 4) throw failure
            delay(1_000L shl attempt)
        }
    }
    error("Unreachable")
}
