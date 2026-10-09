// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AntintwrkNavigationEpochTest {
    @Test
    fun `late failed navigation stays discarded after stop and resume`() = runTest {
        val guard = AntintwrkNavigationEpoch()
        val epoch = guard.advance()
        val completion = CompletableDeferred<Unit>()
        val result = async {
            completion.await()
            guard.accepts(epoch, foreground = true)
        }
        guard.advance() // ON_STOP invalidates the request, even after the next ON_RESUME.
        completion.complete(Unit)
        assertThat(result.await()).isFalse()
        assertThat(guard.accepts(guard.advance(), foreground = true)).isTrue()
    }

    @Test
    fun `new navigation supersedes an old result`() {
        val guard = AntintwrkNavigationEpoch()
        val old = guard.advance()
        val latest = guard.advance()
        assertThat(guard.accepts(old, foreground = true)).isFalse()
        assertThat(guard.accepts(latest, foreground = true)).isTrue()
        assertThat(guard.accepts(latest, foreground = false)).isFalse()
    }
}
