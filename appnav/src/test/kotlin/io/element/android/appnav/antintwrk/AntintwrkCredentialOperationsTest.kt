// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AntintwrkCredentialOperationsTest {
    @Test
    fun `late logout cleanup cannot cancel a new credential request`() = runTest {
        val operations = AntintwrkCredentialOperations()
        val token = operations.cleanupToken()
        val release = CompletableDeferred<Unit>()
        val login = async(start = CoroutineStart.UNDISPATCHED) {
            operations.authenticate {
            release.await()
            "verified"
        }
        }
        var cleared = false
        val cleanup = async(start = CoroutineStart.UNDISPATCHED) { operations.clear(token) { cleared = true } }
        assertThat(cleanup.isCompleted).isFalse()
        release.complete(Unit)
        assertThat(login.await()).isEqualTo("verified")
        cleanup.await()
        assertThat(cleared).isFalse()
    }

    @Test
    fun `credential UI waits for already running logout cleanup`() = runTest {
        val operations = AntintwrkCredentialOperations()
        val release = CompletableDeferred<Unit>()
        val cleanup = async(start = CoroutineStart.UNDISPATCHED) { operations.clear(operations.cleanupToken()) { release.await() } }
        var opened = false
        val login = async(start = CoroutineStart.UNDISPATCHED) { operations.authenticate { opened = true } }
        assertThat(opened).isFalse()
        release.complete(Unit)
        cleanup.await()
        login.await()
        assertThat(opened).isTrue()
    }

    @Test
    fun `cleanup belongs to one generation and repeated old cleanup is ignored`() = runTest {
        val operations = AntintwrkCredentialOperations()
        val token = operations.cleanupToken()
        var cleared = 0
        operations.clear(token) { cleared++ }
        operations.clear(token) { cleared++ }
        assertThat(cleared).isEqualTo(1)
    }

    @Test
    fun `login result survives foreground resume without a new guest admission`() {
        for (page in listOf(AntintwrkPage.Login, AntintwrkPage.Register)) {
            for (busy in listOf(true, false)) {
                assertThat(antintwrkRevalidateOnResume(AntintwrkAuthStage.LOGIN, busy, page)).isFalse()
            }
        }
        assertThat(antintwrkRevalidateOnResume(null, true, AntintwrkPage.Welcome)).isTrue()
        assertThat(antintwrkRevalidateOnResume(null, true, AntintwrkPage.Login)).isFalse()
        assertThat(antintwrkRevalidateOnResume(AntintwrkAuthStage.AUTHENTICATED, false, AntintwrkPage.Welcome)).isTrue()
        assertThat(antintwrkRevalidateOnResume(AntintwrkAuthStage.DEVICE, false, AntintwrkPage.Login)).isTrue()
        assertThat(antintwrkRevalidateOnResume(AntintwrkAuthStage.TOTP, true, AntintwrkPage.Login)).isFalse()
    }
}
