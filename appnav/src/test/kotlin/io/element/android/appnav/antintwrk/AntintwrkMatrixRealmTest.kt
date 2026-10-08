// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import com.google.common.truth.Truth.assertThat
import io.element.android.features.userprofile.api.AntintwrkProfileNavigation
import io.element.android.libraries.matrix.api.user.AntintwrkMatrixIdentity
import org.junit.After
import org.junit.Test

class AntintwrkMatrixRealmTest {
    @After
    fun cleanup() {
        AntintwrkMessengerRoutes.clear()
        AntintwrkProfileNavigation.clear()
    }

    @Test
    fun `public identity and legacy identity both resolve stable social IDs`() {
        for (domain in listOf("antintwrk.com", "matrix.antimsg.com")) {
            assertThat(AntintwrkMatrixIdentity.socialId("@u18:$domain")).isEqualTo("18")
            assertThat(AntintwrkAccountSignOut.matches("@u18:$domain", "@u18:$domain", true)).isTrue()
            AntintwrkMessengerRoutes.open("@u18:$domain", "@u123:$domain")
            assertThat(AntintwrkMessengerRoutes.requests.value?.recipient).isEqualTo("@u123:$domain")
            AntintwrkProfileNavigation.open("@u18:$domain", "@u123:$domain")
            assertThat(AntintwrkProfileNavigation.requests.value?.socialId).isEqualTo("123")
        }
    }

    @Test
    fun `matching social number never merges different Matrix identities`() {
        assertThat(AntintwrkAccountSignOut.matches("@u18:antintwrk.com", "@u18:matrix.antimsg.com", true)).isFalse()
        assertThat(AntintwrkPushAccountPolicy.allows("@u18:antintwrk.com", "@u18:matrix.antimsg.com", false)).isFalse()
        assertThat(runCatching { AntintwrkMessengerRoutes.open("@u18:antintwrk.com", "@u123:matrix.antimsg.com") }.isFailure).isTrue()
        assertThat(AntintwrkMessengerRoutes.requests.value).isNull()
        AntintwrkProfileNavigation.open("@u18:antintwrk.com", "@u123:matrix.antimsg.com")
        assertThat(AntintwrkProfileNavigation.requests.value).isNull()
    }

    @Test
    fun `only canonical source IDs in the two explicit realms are admitted`() {
        for (value in listOf(
                "@u0:antintwrk.com",
                "@u018:antintwrk.com",
                "@u18:evil.example",
                "@u18:antintwrk.com.evil.example",
                             "@u18:antintwrk.com:443",
                "@u18:ANTINTWRK.COM",
                "@u18:antintwrk.com\n",
                "@alice:antintwrk.com",
                             "@u1234567890123456789:antintwrk.com"
            )) {
            assertThat(AntintwrkMatrixIdentity.socialId(value)).isNull()
        }
    }
}
