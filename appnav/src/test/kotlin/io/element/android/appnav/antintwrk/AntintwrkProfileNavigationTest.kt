// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import com.google.common.truth.Truth.assertThat
import io.element.android.features.userprofile.api.AntintwrkProfileNavigation
import org.junit.After
import org.junit.Test

class AntintwrkProfileNavigationTest {
    @After
    fun cleanUp() = AntintwrkProfileNavigation.clear()

    @Test
    fun `only immutable managed source identities have social profiles`() {
        assertThat(AntintwrkProfileNavigation.socialId("@u123:matrix.antimsg.com")).isEqualTo("123")
        for (id in listOf("@u0:matrix.antimsg.com", "@u123:foreign.example", "@alice:matrix.antimsg.com", "@u9999999999999999999:matrix.antimsg.com")) {
            assertThat(AntintwrkProfileNavigation.socialId(id)).isNull()
        }
    }

    @Test
    fun `old completion cannot consume a newer profile request`() {
        AntintwrkProfileNavigation.open("@u1:matrix.antimsg.com", "@u2:matrix.antimsg.com")
        val first = AntintwrkProfileNavigation.requests.value!!
        AntintwrkProfileNavigation.open("@u1:matrix.antimsg.com", "@u3:matrix.antimsg.com")
        AntintwrkProfileNavigation.consume(first)
        assertThat(AntintwrkProfileNavigation.requests.value?.socialId).isEqualTo("3")
        assertThat(AntintwrkProfileNavigation.requests.value?.actor).isEqualTo("@u1:matrix.antimsg.com")
    }

    @Test
    fun `foreign actor and URL targets cannot create navigation`() {
        AntintwrkProfileNavigation.open("@u1:foreign.example", "@u2:matrix.antimsg.com")
        AntintwrkProfileNavigation.open("@u1:matrix.antimsg.com", "https://evil.example")
        assertThat(AntintwrkProfileNavigation.requests.value).isNull()
    }
}
