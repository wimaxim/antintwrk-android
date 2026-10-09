// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import javax.net.ssl.SSLException

class AntintwrkNavigationRetryTest {
    @Test
    fun `temporary failure recovers without surfacing the error`() = runTest {
        var calls = 0
        val result = antintwrkRetryNavigationRead {
            if (++calls < 3) throw IOException("Temporary transport failure")
            "ready"
        }
        assertThat(result).isEqualTo("ready")
        assertThat(calls).isEqualTo(3)
        assertThat(testScheduler.currentTime).isEqualTo(3_000L)
    }

    @Test
    fun `persistent unavailable response is surfaced only after five attempts`() = runTest {
        var calls = 0
        val failure = runCatching {
            antintwrkRetryNavigationRead {
                calls++
                throw AntintwrkRequestError("UNAVAILABLE", 503)
            }
        }.exceptionOrNull()
        assertThat(failure).isInstanceOf(AntintwrkRequestError::class.java)
        assertThat(calls).isEqualTo(5)
        assertThat(testScheduler.currentTime).isEqualTo(15_000L)
    }

    @Test
    fun `authorization contact tls and cancellation failures are not retried`() = runTest {
        for (failure in listOf(
                AntintwrkRequestError("AUTH_REQUIRED", 401),
                AntintwrkRequestError("CONTACT_UNAVAILABLE", 403),
            AntintwrkRequestError("RATE_LIMITED", 429),
                SSLException("Trust failure"),
                CancellationException("Stopped")
            )) {
            var calls = 0
            assertThat(runCatching {
                antintwrkRetryNavigationRead {
                calls++
                throw failure
            }
            }.exceptionOrNull()).isSameInstanceAs(failure)
            assertThat(calls).isEqualTo(1)
        }
    }

    @Test
    fun `superseded intent cannot retry while activity is resumed again`() = runTest {
        val guard = AntintwrkNavigationEpoch()
        val epoch = guard.advance()
        var calls = 0
        val failure = runCatching {
            antintwrkRetryNavigationRead({ guard.accepts(epoch, true) }) {
                calls++
                guard.advance()
                throw IOException("Late failure")
            }
        }.exceptionOrNull()
        assertThat(failure).isInstanceOf(CancellationException::class.java)
        assertThat(calls).isEqualTo(1)
    }
}
