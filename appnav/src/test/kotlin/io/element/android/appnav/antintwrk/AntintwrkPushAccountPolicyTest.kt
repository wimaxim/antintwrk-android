// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AntintwrkPushAccountPolicyTest {
    @Test
    fun `only the last admitted source actor can receive Matrix notifications`() {
        assertThat(AntintwrkPushAccountPolicy.allows("@u123:matrix.antimsg.com", "@u123:matrix.antimsg.com", false)).isTrue()
        assertThat(AntintwrkPushAccountPolicy.allows("@u123:matrix.antimsg.com", "@u456:matrix.antimsg.com", false)).isFalse()
        assertThat(AntintwrkPushAccountPolicy.allows("@u123:matrix.antimsg.com", null, false)).isFalse()
    }

    @Test
    fun `explicit social logout rejects notification from every retained Matrix store`() {
        assertThat(AntintwrkPushAccountPolicy.allows("@u123:matrix.antimsg.com", "@u123:matrix.antimsg.com", true)).isFalse()
    }
}
