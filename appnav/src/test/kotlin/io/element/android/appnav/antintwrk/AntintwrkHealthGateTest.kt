// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AntintwrkHealthGateTest {
    @Test
    fun `a transient failure never hides a service and success resets the counter`() {
        val gate = AntintwrkHealthGate()
        assertThat(gate.record(false)).isFalse()
        assertThat(gate.record(true)).isFalse()
        assertThat(gate.record(false)).isFalse()
        assertThat(gate.record(false)).isTrue()
        assertThat(gate.record(true)).isFalse()
    }
    @Test
    fun `one failed service does not affect the other`() {
        val social = AntintwrkHealthGate()
        val messenger = AntintwrkHealthGate()
        repeat(100) {
            social.record(false)
            messenger.record(true)
        }
        assertThat(social.unavailable).isTrue()
        assertThat(social.failures).isEqualTo(2)
        assertThat(messenger.unavailable).isFalse()
    }
}
