// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AntintwrkGeoRefreshTest {
    private val admission = AntintwrkAuthSession(AntintwrkAuthStage.AUTHENTICATED, "a".repeat(64), matrixUserId = "@u42:antintwrk.com")
    private fun web() = AntintwrkWebFeedController().apply {
        routingSelected = true
        origin = AntintwrkSocialOrigins.RUSSIAN
        routedSession = admission
        currentUrl = "$origin/en/hero-feed?app=1"
        loading = false
    }
    private fun pending(web: AntintwrkWebFeedController, actor: String? = admission.matrixUserId, at: Long = 0) {
        web.pendingRouting = AntintwrkPreparedSocialRouting(
            AntintwrkSocialOrigins.PUBLIC,
        admission.copy(matrixUserId = actor),
        AntintwrkSocialCookieEpoch.current(),
        at
        )
    }

    @Test
    fun `five minute resume cache and forced network checks prepare without changing the document`() = runTest {
        var now = 0L
        var lookups = 0
        val web = web()
        val coordinator = AntintwrkSocialOriginCoordinator(
            ApplicationProvider.getApplicationContext(),
            lookup = {
                lookups++
                AntintwrkSocialOrigins.PUBLIC
            },
            transfer = { _, _, session -> session },
            clock = { now },
            read = { _, session, _ -> session }
        )
        coordinator.preload()
        coordinator.refresh(AntintwrkNativeAuthClient(), admission, web)
        assertThat(lookups).isEqualTo(1)
        coordinator.refresh(AntintwrkNativeAuthClient(), admission, web, force = true)
        assertThat(lookups).isEqualTo(2)
        assertThat(web.pendingRouting?.origin).isEqualTo(AntintwrkSocialOrigins.PUBLIC)
        assertThat(web.origin).isEqualTo(AntintwrkSocialOrigins.RUSSIAN)
        assertThat(web.currentUrl).isEqualTo("https://antimsg.com/en/hero-feed?app=1")
        now = 299_999
        assertThat(coordinator.nextCheckDelay()).isEqualTo(30_000)
        coordinator.refresh(AntintwrkNativeAuthClient(), admission, web)
        assertThat(lookups).isEqualTo(2)
        now = 300_000
        coordinator.refresh(AntintwrkNativeAuthClient(), admission, web)
        assertThat(lookups).isEqualTo(3)
        assertThat(coordinator.nextCheckDelay()).isEqualTo(300_000)
    }

    @Test
    fun `safe native section navigation applies the prepared host and keeps profile metadata`() {
        val web = web()
        val old = WebView(ApplicationProvider.getApplicationContext())
        web.registerPage(AntintwrkSection.Feed, old, web.currentUrl!!)
        web.pageFinished(old, web.currentUrl)
        web.profileName = "Test"
        web.selectSection(AntintwrkSection.Communities, "https://antimsg.com/ru/communities?app=1")
        web.selectSection(AntintwrkSection.Feed, "https://antimsg.com/en/hero-feed?app=1")
        pending(web, at = android.os.SystemClock.elapsedRealtime())
        web.selectSection(AntintwrkSection.Communities, "https://antimsg.com/en/communities?app=1")
        assertThat(web.origin).isEqualTo(AntintwrkSocialOrigins.PUBLIC)
        assertThat(web.currentUrl).isEqualTo("https://antintwrk.com/en/communities?app=1")
        assertThat(web.profileName).isEqualTo("Test")
        assertThat(web.pageUrl(AntintwrkSection.Feed)).startsWith("https://antintwrk.com/en/")
        assertThat(web.pendingRouting).isNull()
        assertThat(web.page(AntintwrkSection.Feed)).isNull()
        assertThat(Shadows.shadowOf(old).wasDestroyCalled()).isTrue()
        assertThat(Shadows.shadowOf(old).lastLoadedUrl).isNull()
    }

    @Test
    fun `migrating an additional page preserves the language of its return tab`() {
        val web = web()
        web.selectSection(AntintwrkSection.Feed, "https://antimsg.com/ru/hero-feed?app=1")
        web.loading = false
        web.navigate("https://antimsg.com/ru/people?app=1", allowRouting = false)
        web.loading = false
        pending(web, at = android.os.SystemClock.elapsedRealtime())
        assertThat(web.applyPreparedRouting()).isTrue()
        web.back()
        assertThat(web.currentUrl).isEqualTo("https://antintwrk.com/ru/hero-feed?app=1")
    }

    @Test
    fun `hidden composer and form drafts survive navigation even after the keyboard closes`() {
        val web = web()
        web.updateEditor(true)
        web.selectSection(AntintwrkSection.Communities, "https://antimsg.com/en/communities?app=1")
        pending(web)
        assertThat(web.applyPreparedRouting(0)).isFalse()
        assertThat(web.pendingRouting).isNotNull()
        web.selectSection(AntintwrkSection.Feed, "https://antimsg.com/en/hero-feed?app=1")
        web.updateEditor(false)
        web.loading = false
        web.markDraft()
        web.imeVisible = false
        assertThat(web.applyPreparedRouting(0)).isFalse()
    }

    @Test
    fun `expired foreign actor and late logout preparations never switch origins`() {
        for (actor in listOf(admission.matrixUserId, "@u43:antintwrk.com")) {
            val web = web()
            pending(web, actor)
            assertThat(web.applyPreparedRouting(if (actor == admission.matrixUserId) 180_001 else 0)).isFalse()
            assertThat(web.pendingRouting).isNull()
        }
        val web = web()
        pending(web)
        AntintwrkSocialCookieEpoch.advance()
        assertThat(web.applyPreparedRouting(0)).isFalse()
        assertThat(web.origin).isEqualTo(AntintwrkSocialOrigins.RUSSIAN)
    }

    @Test
    fun `transport outage immediately prepares alternate host with a bounded retry cooldown`() = runTest {
        var lookups = 0
        val web = web()
        val coordinator = AntintwrkSocialOriginCoordinator(
            ApplicationProvider.getApplicationContext(),
            lookup = {
                lookups++
                AntintwrkSocialOrigins.RUSSIAN
            },
            transfer = { _, _, session -> session },
            clock = { 0 },
            read = { _, session, _ -> session }
        )
        coordinator.refresh(AntintwrkNativeAuthClient(), admission, web, force = true, unavailable = true)
        assertThat(web.pendingRouting?.origin).isEqualTo(AntintwrkSocialOrigins.PUBLIC)
        coordinator.refresh(AntintwrkNativeAuthClient(), admission, web, force = true, unavailable = true)
        assertThat(lookups).isEqualTo(1)
    }

    @Test
    fun `challenge and changed actor at admission cannot prepare a route`() = runTest {
        for (fresh in listOf(admission.copy(stage = AntintwrkAuthStage.DEVICE), admission.copy(matrixUserId = "@u43:antintwrk.com"))) {
            val web = web()
            val coordinator = AntintwrkSocialOriginCoordinator(
                ApplicationProvider.getApplicationContext(),
                lookup = { AntintwrkSocialOrigins.PUBLIC },
                transfer = { _, _, session -> session },
                read = { _, _, _ -> fresh }
            )
            coordinator.refresh(AntintwrkNativeAuthClient(), admission, web, force = true)
            assertThat(web.pendingRouting).isNull()
            assertThat(web.origin).isEqualTo(AntintwrkSocialOrigins.RUSSIAN)
        }
    }
}
