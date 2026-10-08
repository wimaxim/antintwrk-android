// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AntintwrkAccountSignOutTest {
    @Test
    fun `completed logout closes only its matching social account`() {
        assertThat(AntintwrkAccountSignOut.matches("@u123:matrix.antimsg.com", "@u123:matrix.antimsg.com", true)).isTrue()
        assertThat(AntintwrkAccountSignOut.matches("@u123:matrix.antimsg.com", "@u456:matrix.antimsg.com", true)).isFalse()
    }
    @Test
    fun `expired Matrix recovery preserves social admission`() {
        assertThat(AntintwrkAccountSignOut.matches("@u123:matrix.antimsg.com", "@u123:matrix.antimsg.com", false)).isFalse()
    }
    @Test
    fun `initial guest and unrelated accounts never clear social cookies`() {
        assertThat(AntintwrkAccountSignOut.matches(null, null, true)).isFalse()
        assertThat(AntintwrkAccountSignOut.matches("@technical:matrix.antimsg.com", "@technical:matrix.antimsg.com", true)).isFalse()
    }
}
