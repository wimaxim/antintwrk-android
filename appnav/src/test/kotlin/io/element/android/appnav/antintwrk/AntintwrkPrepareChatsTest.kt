// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AntintwrkPrepareChatsTest {
    @Test
    fun `stalled local navigation retries and can recover without an error`() = runTest {
        var attempts = 0
        antintwrkPrepareChats({ true }) { if (++attempts < 3) awaitCancellation() }
        assertThat(attempts).isEqualTo(3)
    }

    @Test
    fun `persistent stall is bounded to five attempts`() = runTest {
        var attempts = 0
        val failure = runCatching {
            antintwrkPrepareChats({ true }) {
            attempts++
            awaitCancellation()
        }
        }.exceptionOrNull()
        assertThat(attempts).isEqualTo(5)
        assertThat(failure).isInstanceOf(TimeoutCancellationException::class.java)
    }

    @Test
    fun `superseded request cannot retry or switch to messenger`() = runTest {
        var current = true
        var attempts = 0
        val failure = runCatching {
            antintwrkPrepareChats({ current }) {
                attempts++
                current = false
                awaitCancellation()
            }
        }.exceptionOrNull()
        assertThat(attempts).isEqualTo(1)
        assertThat(failure).isInstanceOf(CancellationException::class.java)
    }
}
