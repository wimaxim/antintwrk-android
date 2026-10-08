// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test

class AntintwrkMessengerRoutesTest {
    @Test
    fun `a new admitted source actor discards stale routes`() {
        AntintwrkMessengerRoutes.open("@u123:matrix.antimsg.com", "@u456:matrix.antimsg.com")
        AntintwrkMessengerRoutes.discardOtherActor("@u123:matrix.antimsg.com")
        assertThat(AntintwrkMessengerRoutes.requests.value).isNotNull()
        AntintwrkMessengerRoutes.discardOtherActor("@u789:matrix.antimsg.com")
        assertThat(AntintwrkMessengerRoutes.requests.value).isNull()
    }
    @After
    fun clear() = AntintwrkMessengerRoutes.clear()

    @Test
    fun `consuming an earlier route preserves a newer request`() {
        AntintwrkMessengerRoutes.open("@u123:matrix.antimsg.com", "@u456:matrix.antimsg.com")
        val first = requireNotNull(AntintwrkMessengerRoutes.requests.value)
        AntintwrkMessengerRoutes.open("@u123:matrix.antimsg.com", "@u789:matrix.antimsg.com")
        AntintwrkMessengerRoutes.consume(first)
        assertThat(AntintwrkMessengerRoutes.requests.value?.recipient).isEqualTo("@u789:matrix.antimsg.com")
    }
    @Test
    fun `route retains its source account and explicit logout clears it`() {
        AntintwrkMessengerRoutes.open("@u123:matrix.antimsg.com", "@u456:matrix.antimsg.com")
        assertThat(AntintwrkMessengerRoutes.requests.value?.actor).isEqualTo("@u123:matrix.antimsg.com")
        AntintwrkMessengerRoutes.clear()
        assertThat(AntintwrkMessengerRoutes.requests.value).isNull()
    }
    @Test(expected = IllegalArgumentException::class)
    fun `foreign recipient is refused`() {
        AntintwrkMessengerRoutes.open("@u123:matrix.antimsg.com", "@u456:foreign.example")
    }
    @Test(expected = IllegalArgumentException::class)
    fun `self conversation is refused`() {
        AntintwrkMessengerRoutes.open("@u123:matrix.antimsg.com", "@u123:matrix.antimsg.com")
    }
}
