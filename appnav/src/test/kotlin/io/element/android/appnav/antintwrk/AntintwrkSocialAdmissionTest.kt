// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import javax.net.ssl.SSLHandshakeException

class AntintwrkSocialAdmissionTest {
    private val actor = "@u42:antintwrk.com"
    private val admitted = AntintwrkAuthSession(AntintwrkAuthStage.AUTHENTICATED, "a".repeat(64), matrixUserId = actor)

    @Test
    fun `same actor public admission survives an unavailable Russian ingress`() = runTest {
        var accepted = false
        val result = antintwrkRestoreSocialAdmission(actor, { throw IOException() }, { admitted }, { accepted = true })
        assertThat(result).isEqualTo(admitted)
        assertThat(accepted).isTrue()
    }

    @Test
    fun `unknown and different actors are never admitted from fallback`() = runTest {
        for (expected in listOf(null, "", "@u43:antintwrk.com")) {
            var accepted = false
            assertThat(runCatching {
                antintwrkRestoreSocialAdmission(expected, { throw AntintwrkRequestError("UNAVAILABLE", 503) }, { admitted }, { accepted = true })
            }.isFailure).isTrue()
            assertThat(accepted).isFalse()
        }
    }

    @Test
    fun `explicit login device and totp challenges cannot fall back to an older public session`() = runTest {
        for (stage in listOf(AntintwrkAuthStage.LOGIN, AntintwrkAuthStage.DEVICE, AntintwrkAuthStage.TOTP)) {
            val required = admitted.copy(stage = stage)
            var fallbackCalled = false
            val result = antintwrkRestoreSocialAdmission(actor, { required }, {
                fallbackCalled = true
                admitted
            }, {})
            assertThat(result).isEqualTo(required)
            assertThat(fallbackCalled).isFalse()
        }
    }

    @Test
    fun `cancellation TLS failure and rate limit never invoke fallback`() = runTest {
        for (error in listOf(CancellationException(), SSLHandshakeException("fixture"), AntintwrkRequestError("RATE_LIMITED", 429))) {
            var fallbackCalled = false
            assertThat(runCatching {
                antintwrkRestoreSocialAdmission(actor, { throw error }, {
                    fallbackCalled = true
                    admitted
                }, {})
            }.isFailure).isTrue()
            assertThat(fallbackCalled).isFalse()
        }
    }
}
