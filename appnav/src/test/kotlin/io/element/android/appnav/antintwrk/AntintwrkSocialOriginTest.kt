// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AntintwrkSocialOriginTest {
    private fun response(country: String?, detected: Boolean, origin: String) = JSONObject()
        .put("version", 1).put("country", country ?: JSONObject.NULL).put("detected", detected).put("social_origin", origin)

    @Test
    fun `country routes RU and unknown through Russian entry and others through public entry`() {
        for (country in listOf("US", "DE", "UA", "BR", "CN")) {
            assertThat(AntintwrkSocialOrigins.recommendation(response(country, true, AntintwrkSocialOrigins.PUBLIC)))
                .isEqualTo(AntintwrkSocialOrigins.PUBLIC)
        }
        assertThat(AntintwrkSocialOrigins.recommendation(response("RU", true, AntintwrkSocialOrigins.RUSSIAN)))
            .isEqualTo(AntintwrkSocialOrigins.RUSSIAN)
        assertThat(AntintwrkSocialOrigins.recommendation(response(null, false, AntintwrkSocialOrigins.RUSSIAN)))
            .isEqualTo(AntintwrkSocialOrigins.RUSSIAN)
    }

    @Test
    fun `malformed recommendations and arbitrary origins cannot choose a host`() {
        for (json in listOf(
                response("RU", true, AntintwrkSocialOrigins.PUBLIC),
                response("US", true, AntintwrkSocialOrigins.RUSSIAN),
                response(null, false, AntintwrkSocialOrigins.PUBLIC),
                response("ru", true, AntintwrkSocialOrigins.RUSSIAN),
                response("ZZ", true, AntintwrkSocialOrigins.PUBLIC),
                response(null, true, AntintwrkSocialOrigins.RUSSIAN),
                response("US", false, AntintwrkSocialOrigins.PUBLIC),
                response("US", true, "https://evil.example"),
                response("US", true, "http://antintwrk.com"),
                response("US", true, AntintwrkSocialOrigins.PUBLIC).put("version", 2),
                JSONObject(),
            )) {
                assertThat(runCatching { AntintwrkSocialOrigins.recommendation(json) }.isFailure).isTrue()
            }
    }

    @Test
    fun `both social domains canonicalise navigation without trusting the inactive bridge origin`() {
        for (origin in AntintwrkSocialOrigins.allowed) {
            val policy = AntintwrkWebPolicy(origin)
            for (input in AntintwrkSocialOrigins.allowed) {
                assertThat(policy.embedded("$input/en/communities?app=1")).isEqualTo("$origin/en/communities?app=1")
                assertThat(policy.canonical("$input/en/goto?game=1")).isEqualTo("$origin/en/goto?game=1")
                assertThat(policy.outbound("$input/en/goto?game=1")).isTrue()
                assertThat(policy.sameOrigin(input)).isEqualTo(input == origin)
            }
            for (bad in listOf("https://antintwrk.com.evil.example/en/profile", "http://antintwrk.com/en/profile", "https://x@antintwrk.com/en/profile")) {
                assertThat(policy.embedded(bad)).isNull()
            }
        }
    }

    @Test
    fun `logout and account changes invalidate late cookie responses`() {
        val epoch = AntintwrkSocialCookieEpoch.current()
        AntintwrkSocialCookieEpoch.advance()
        assertThat(runCatching { AntintwrkSocialCookieEpoch.check(epoch) }.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
        AntintwrkSocialCookieEpoch.check(AntintwrkSocialCookieEpoch.current())
    }

    @Test
    fun `initial handoff selects public origin and later revalidation preserves the existing page`() = runTest {
        var lookups = 0
        val coordinator = AntintwrkSocialOriginCoordinator(ApplicationProvider.getApplicationContext(), lookup = {
            lookups++
            if (lookups == 1) AntintwrkSocialOrigins.PUBLIC else AntintwrkSocialOrigins.RUSSIAN
        }, transfer = { _, _, session -> session.copy(csrf = "b".repeat(64)) })
        val web = AntintwrkWebFeedController()
        val admission = AntintwrkAuthSession(AntintwrkAuthStage.AUTHENTICATED, "a".repeat(64), matrixUserId = "@u42:antintwrk.com")
        val client = AntintwrkNativeAuthClient()
        coordinator.prepare(client, admission, web)
        assertThat(web.origin).isEqualTo(AntintwrkSocialOrigins.PUBLIC)
        assertThat(web.routedSession?.csrf).isEqualTo("b".repeat(64))
        web.currentUrl = "https://antintwrk.com/en/hero-compose?app=1"
        web.updateEditor(true)
        coordinator.prepare(client, admission, web)
        assertThat(lookups).isEqualTo(1)
        assertThat(web.currentUrl).contains("hero-compose")
        assertThat(web.composing).isTrue()
        assertThat(web.origin).isEqualTo(AntintwrkSocialOrigins.PUBLIC)
    }

    @Test
    fun `initial failed handoff falls back without admitting another actor`() = runTest {
        val admission = AntintwrkAuthSession(AntintwrkAuthStage.AUTHENTICATED, "a".repeat(64), matrixUserId = "@u42:antintwrk.com")
        val coordinator = AntintwrkSocialOriginCoordinator(
            ApplicationProvider.getApplicationContext(),
            lookup = { AntintwrkSocialOrigins.PUBLIC },
            transfer = { _, _, session -> session.copy(matrixUserId = "@u43:antintwrk.com") }
        )
        val web = AntintwrkWebFeedController()
        coordinator.prepare(AntintwrkNativeAuthClient(), admission, web)
        assertThat(web.origin).isEqualTo(AntintwrkSocialOrigins.RUSSIAN)
        assertThat(web.routedSession).isEqualTo(admission)
    }
}
