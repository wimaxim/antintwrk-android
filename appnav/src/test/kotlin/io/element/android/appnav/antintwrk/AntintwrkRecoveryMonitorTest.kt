// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.encryption.RecoveryState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AntintwrkRecoveryMonitorTest {
    @Test
    fun `false result retries with bounded backoff instead of ending monitoring`() = runTest {
        var attempts = 0
        backgroundScope.launch {
            AntintwrkRecoveryMonitor(null) {
            attempts++
            false
        }.run()
        }
        runCurrent()
        assertThat(attempts).isEqualTo(1)
        advanceTimeBy(30_000)
        runCurrent()
        assertThat(attempts).isEqualTo(2)
        advanceTimeBy(60_000)
        runCurrent()
        assertThat(attempts).isEqualTo(3)
    }

    @Test
    fun `new incomplete state recovers promptly after successful setup`() = runTest {
        val states = MutableStateFlow(RecoveryState.ENABLED)
        var attempts = 0
        backgroundScope.launch {
            AntintwrkRecoveryMonitor(states) {
                attempts++
                states.value = RecoveryState.ENABLED
                true
            }.run()
        }
        runCurrent()
        advanceTimeBy(1_000)
        runCurrent()
        assertThat(attempts).isEqualTo(1)
        states.value = RecoveryState.INCOMPLETE
        runCurrent()
        assertThat(attempts).isEqualTo(2)
        assertThat(states.value).isEqualTo(RecoveryState.ENABLED)
    }

    @Test
    fun `exception retries but cancellation ends monitoring`() = runTest {
        var attempts = 0
        val job = backgroundScope.launch {
            AntintwrkRecoveryMonitor(null) {
                attempts++
                if (attempts == 1) error("synthetic outage")
                throw CancellationException("synthetic cancellation")
            }.run()
        }
        runCurrent()
        advanceTimeBy(30_000)
        runCurrent()
        assertThat(attempts).isEqualTo(2)
        assertThat(job.isCancelled).isTrue()
        advanceTimeBy(600_000)
        runCurrent()
        assertThat(attempts).isEqualTo(2)
    }
}
