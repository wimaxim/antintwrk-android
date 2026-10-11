/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
@file:OptIn(ExperimentalTestApi::class)

package io.element.android.appnav.antintwrk

import android.webkit.WebView
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import com.google.common.truth.Truth.assertThat
import io.element.android.compound.theme.ElementTheme
import io.element.android.tests.testutils.robolectric.RobolectricTest
import io.element.android.tests.testutils.setSafeContent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class AntintwrkWebFeedTest : RobolectricTest() {
    @Test
    fun `legal reader keeps only localized agreement routes inside native presentation`() {
        for ((route, canonical) in mapOf("terms" to "terms", "useragreement" to "terms", "privacy" to "privacy", "privacypolicy" to "privacy")) {
            assertThat(policy.legalReader("https://antimsg.com/ru/$route?native_legal=0&native_legal=1&app=0#details"))
                .isEqualTo("https://antimsg.com/ru/$canonical?native_legal=1#details")
        }
        for (url in listOf(
            "https://evil.example/en/terms",
                "https://antimsg.com/evil/en/terms",
            "https://antimsg.com/en/profile",
                "https://antimsg.com/zz/terms",
                "http://antimsg.com/en/terms",
            "https://antimsg.com:444/en/terms",
                "https://user@antimsg.com/en/terms",
        )) {
            assertThat(policy.legalReader(url)).isNull()
        }
        assertThat(policy.embedded("https://antintwrk.com/en/useragreement"))
            .isEqualTo("https://antimsg.com/en/terms?app=1")
        assertThat(policy.embedded("https://antimsg.com/en/privacypolicy"))
            .isEqualTo("https://antimsg.com/en/privacy?app=1")
        assertThat(policy.legalReader("https://antintwrk.com/en/privacy"))
            .isEqualTo("https://antimsg.com/en/privacy?native_legal=1")
    }

    @Test
    fun `selecting any tab returns to its root even after navigating to people`() {
        val controller = AntintwrkWebFeedController()
        val people = policy.route("en", "people")
        controller.navigate(people)
        controller.selectSection(AntintwrkSection.Communities, policy.root("en", AntintwrkSection.Communities))
        controller.selectSection(AntintwrkSection.Feed, policy.feed("en"))
        assertThat(controller.currentUrl).isEqualTo(policy.feed("en"))
        controller.selectSection(AntintwrkSection.Feed, policy.feed("en"))
        assertThat(controller.currentUrl).isEqualTo(policy.feed("en"))
        assertThat(controller.section).isEqualTo(AntintwrkSection.Feed)
    }

    @Test
    fun `native navigation starts loading before the network and retained tabs do not reload`() {
        val controller = AntintwrkWebFeedController()
        val feed = mockk<WebView>(relaxed = true)
        controller.view = feed
        controller.currentUrl = policy.feed("en")
        controller.loading = false
        every { feed.loadUrl(any()) } answers { assertThat(controller.loading).isTrue() }
        controller.navigate(policy.feed("en"))
        verify(exactly = 1) { feed.loadUrl(any()) }
        controller.pageFinished(feed, policy.feed("en"))
        controller.selectSection(AntintwrkSection.Communities, policy.root("en", AntintwrkSection.Communities))
        controller.view = mockk(relaxed = true)
        controller.selectSection(AntintwrkSection.Feed, policy.feed("en"))
        assertThat(controller.view).isSameInstanceAs(feed)
        assertThat(controller.loading).isFalse()
        verify(exactly = 1) { feed.loadUrl(any()) }
    }

    @Test
    fun `community root stays intact while people uses an additional page`() {
        val controller = AntintwrkWebFeedController()
        val root = policy.root("en", AntintwrkSection.Communities)
        controller.selectSection(AntintwrkSection.Communities, root)
        val page = mockk<WebView>(relaxed = true)
        controller.registerPage(AntintwrkSection.Communities, page, root)
        controller.pageFinished(page, root)
        controller.navigate(policy.route("en", "people"))
        controller.selectSection(AntintwrkSection.Profile, policy.root("en", AntintwrkSection.Profile))
        controller.selectSection(AntintwrkSection.Communities, root)
        assertThat(controller.currentUrl).isEqualTo(root)
        assertThat(controller.view).isSameInstanceAs(page)
        verify(exactly = 0) { page.loadUrl(root) }
        controller.pageFinished(page, root)
        controller.selectSection(AntintwrkSection.Feed, policy.feed("en"))
        controller.selectSection(AntintwrkSection.Communities, root)
        verify(exactly = 0) { page.loadUrl(root) }
        assertThat(controller.loading).isFalse()
    }

    @Test
    fun `additional navigation never loads a retained root and closing it preserves scroll`() {
        val controller = AntintwrkWebFeedController()
        controller.ownProfileIdentity = "tester"
        val feed = mockk<WebView>(relaxed = true)
        controller.registerPage(AntintwrkSection.Feed, feed, policy.feed("en"))
        controller.pageFinished(feed, policy.feed("en"))
        val extra = mockk<WebView>(relaxed = true)
        controller.navigate(policy.route("en", "people"))
        controller.registerPage(null, extra, policy.route("en", "people"))
        controller.pageFinished(extra, policy.route("en", "people"))
        controller.finishMessengerProfileNavigation(extra)
        controller.navigate(policy.route("en", "hero/123"))
        assertThat(controller.view).isSameInstanceAs(extra)
        assertThat(controller.pageUrl(AntintwrkSection.Feed)).isEqualTo(policy.feed("en"))
        controller.selectSection(AntintwrkSection.Feed, policy.feed("en"))
        assertThat(controller.view).isSameInstanceAs(feed)
        assertThat(controller.loading).isFalse()
        verify(exactly = 0) { feed.loadUrl(any()) }
        verify(exactly = 0) { feed.scrollTo(any(), any()) }
        assertThat(controller.canNavigateInPage(feed, policy.route("en", "hero/123"))).isFalse()
        assertThat(controller.canNavigateInPage(extra, policy.feed("en"))).isFalse()
        assertThat(controller.canNavigateInPage(extra, policy.route("en", "support"))).isTrue()
    }

    @Test
    fun `hidden additional callbacks never replace a selected root and logout destroys all pages`() {
        val controller = AntintwrkWebFeedController()
        val feed = mockk<WebView>(relaxed = true)
        val extra = mockk<WebView>(relaxed = true)
        controller.registerPage(AntintwrkSection.Feed, feed, policy.feed("en"))
        controller.pageFinished(feed, policy.feed("en"))
        val people = policy.route("en", "people")
        controller.navigate(people)
        controller.registerPage(null, extra, people)
        controller.selectSection(AntintwrkSection.Feed, policy.feed("en"))
        controller.pageStarted(extra, people)
        controller.pageFailed(extra)
        controller.pageFinished(extra, people)
        assertThat(controller.currentUrl).isEqualTo(policy.feed("en"))
        assertThat(controller.failed).isFalse()
        assertThat(controller.loading).isFalse()
        controller.destroy()
        verify { feed.destroy() }
        verify { extra.destroy() }
        assertThat(controller.additionalActive).isFalse()
        assertThat(controller.page(null)).isNull()
    }

    @Test
    fun `prewarm requires successful initial feed and resets with account pages`() {
        val controller = AntintwrkWebFeedController()
        val feed = mockk<WebView>(relaxed = true)
        val url = policy.feed("en")
        controller.registerPage(AntintwrkSection.Feed, feed, url)
        assertThat(controller.prewarmStarted).isFalse()
        controller.pageFailed(feed)
        controller.feedReady(feed, url)
        assertThat(controller.prewarmStarted).isFalse()
        controller.pageStarted(feed, url)
        controller.pageFinished(feed, url)
        controller.feedReady(feed, url)
        assertThat(controller.prewarmStarted).isTrue()
        controller.destroy()
        controller.feedReady(feed, url)
        assertThat(controller.prewarmStarted).isFalse()
    }

    @Test
    fun `hidden load and failure never change feed and remain available on selection`() {
        val controller = AntintwrkWebFeedController()
        val feed = mockk<WebView>(relaxed = true)
        val hidden = mockk<WebView>(relaxed = true)
        val root = policy.root("en", AntintwrkSection.Communities)
        controller.registerPage(AntintwrkSection.Feed, feed, policy.feed("en"))
        controller.pageFinished(feed, policy.feed("en"))
        controller.registerPage(AntintwrkSection.Communities, hidden, root)
        controller.pageStarted(hidden, root)
        controller.pageFailed(hidden)
        controller.pageFinished(hidden, root)
        assertThat(controller.view).isSameInstanceAs(feed)
        assertThat(controller.section).isEqualTo(AntintwrkSection.Feed)
        assertThat(controller.currentUrl).isEqualTo(policy.feed("en"))
        assertThat(controller.failed).isFalse()
        assertThat(controller.loading).isFalse()
        controller.selectSection(AntintwrkSection.Communities, root)
        assertThat(controller.failed).isTrue()
        assertThat(controller.view).isSameInstanceAs(hidden)
        verify(exactly = 0) { feed.loadUrl(any()) }
    }

    @Test
    fun `manual refresh starts the loader and uses GET rather than replaying POST`() {
        val controller = AntintwrkWebFeedController()
        val page = mockk<WebView>(relaxed = true)
        controller.view = page
        controller.currentUrl = policy.feed("en")
        controller.loading = false
        every { page.loadUrl(any(), any()) } answers { assertThat(controller.loading).isTrue() }
        controller.retry()
        verify { page.loadUrl(policy.feed("en"), mapOf("Cache-Control" to "no-cache")) }
        verify(exactly = 0) { page.reload() }
    }

    @Test
    fun `background failure retains only existing admission and explicit denial revokes it`() {
        assertThat(antintwrkRetainAdmission(true, true, "UNAVAILABLE")).isTrue()
        assertThat(antintwrkRetainAdmission(true, true, "RATE_LIMITED")).isTrue()
        assertThat(antintwrkRetainAdmission(true, false)).isFalse()
        assertThat(antintwrkRetainAdmission(false, true)).isFalse()
        for (code in listOf("AUTH_REQUIRED", "SESSION_EXPIRED", "FORBIDDEN", "ACCOUNT_UNAVAILABLE", "UNKNOWN")) {
            assertThat(antintwrkRetainAdmission(true, true, code)).isFalse()
        }
    }

    @Test
    fun `pull refresh ignores horizontal upward and short gestures`() {
        assertThat(antintwrkRefreshDrag(0f, 120f, 96f)).isTrue()
        assertThat(antintwrkRefreshDrag(130f, 120f, 96f)).isFalse()
        assertThat(antintwrkRefreshDrag(0f, -120f, 96f)).isFalse()
        assertThat(antintwrkRefreshDrag(0f, 30f, 96f)).isFalse()
    }

    private val policy = AntintwrkWebPolicy("https://antimsg.com")

    @Test
    fun `social message links carry only a validated source recipient`() {
        assertThat(policy.messengerLink("https://antimsg.com/en/messages")?.target).isNull()
        assertThat(policy.messengerLink("https://antimsg.com/ru/messages?with=123")?.target).isEqualTo("123")
        assertThat(policy.messengerLink("https://antimsg.com/ru/messages/abcdefghijklmnop")?.target).isEqualTo("room:abcdefghijklmnop")
        for (url in listOf(
            "https://foreign.example/en/messages?with=123",
            "https://antimsg.com/en/messages?with=@other:matrix.antimsg.com",
            "https://antimsg.com/en/messages?with=123&with=456",
            "https://antimsg.com/en/messages?with=0",
            "https://antimsg.com/en/messages/123",
            "https://antimsg.com/en/messages/abcdefghijklmnop?with=123",
        )) {
            assertThat(policy.messengerLink(url)).isNull()
        }
    }

    @Test
    fun `message navigation opens native messenger without replacing the social page`() {
        val controller = AntintwrkWebFeedController()
        controller.currentUrl = policy.feed("en")
        var recipient: String? = null
        controller.onMessenger = { recipient = it }
        controller.navigate("https://antimsg.com/en/messages?with=123")
        assertThat(recipient).isEqualTo("123")
        assertThat(controller.currentUrl).isEqualTo(policy.feed("en"))
    }

    @Test
    fun `feed uses configured origin supported language and guest presentation`() {
        assertThat(policy.feed("ru")).isEqualTo("https://antimsg.com/ru/hero-feed?app=1")
        assertThat(policy.feed("ja")).isEqualTo("https://antimsg.com/en/hero-feed?app=1")
        assertThat(AntintwrkWebPolicy("https://social.example.org").feed("ar"))
            .isEqualTo("https://social.example.org/ar/hero-feed?app=1")
    }

    @Test
    fun `internal navigation retains filters repeated query values and fragment`() {
        val target = policy.embedded("https://antimsg.com/ru/hero-feed?view=memes&scope=latest&tag=a&tag=b&app=0#comments")
        assertThat(target).isEqualTo("https://antimsg.com/ru/hero-feed?view=memes&scope=latest&tag=a&tag=b&app=1#comments")
        assertThat(policy.embedded(target!!)).isEqualTo(target)
        assertThat(policy.embedded("https://antimsg.com/en/discover?q=hello%20world"))
            .isEqualTo("https://antimsg.com/en/discover?q=hello%20world&app=1")
    }

    @Test
    fun `legacy social navigation rewrites but the old origin stays untrusted`() {
        assertThat(policy.embedded("https://antintwrk.com/en/profile")).isEqualTo("https://antimsg.com/en/profile?app=1")
        assertThat(policy.sameOrigin("https://antintwrk.com/en/profile")).isFalse()
        assertThat(policy.external("https://antimsg.com/en/profile")).isFalse()
        assertThat(policy.external("https://antintwrk.com/en/profile")).isFalse()
        assertThat(policy.canonical("https://antintwrk.com.evil.org/en/profile")).isEqualTo("https://antintwrk.com.evil.org/en/profile")
    }

    @Test
    fun `foreign origins insecure urls account routes and arbitrary schemes are not embedded`() {
        listOf(
            "http://antimsg.com/ru/hero-feed",
            "https://antimsg.com.evil.org/ru/hero-feed",
            "https://evil.org/ru/hero-feed",
            "https://name@antimsg.com/ru/hero-feed",
            "https://antimsg.com:444/ru/hero-feed",
            "https://antimsg.com/en/?auth=login",
            "https://antimsg.com/en/goto",
            "https://antimsg.com/register.php",
            "javascript:alert(1)",
            "file:///etc/passwd",
            "intent://anything",
            "https://antimsg.com/xx/hero-feed",
        ).forEach { assertThat(policy.embedded(it)).isNull() }
        assertThat(policy.external("https://example.org/article")).isTrue()
        assertThat(policy.external("javascript:alert(1)")).isFalse()
        assertThat(policy.external("intent://anything")).isFalse()
    }

    @Test
    fun `social pages embed but outbound routes preserve the browser transition`() {
        listOf("communities", "profile", "hero-compose", "notifications", "people", "monetization", "messages").forEach {
            assertThat(policy.embedded("https://antimsg.com/ru/$it")).endsWith("?app=1")
        }
        listOf("/goto?to=game", "/go/?url=https%3A%2F%2Fexample.org", "/ru/goto?to=game", "/en/go/").forEach {
            val url = "https://antimsg.com$it"
            assertThat(policy.outbound(url)).isTrue()
            assertThat(policy.embedded(url)).isNull()
        }
        assertThat(policy.outbound("https://antimsg.com/ru/hero-feed?q=/goto")).isFalse()
        assertThat(policy.outbound("https://antimsg.com/ru/goto-other")).isFalse()
    }

    @Test
    fun `web routes select their native section and auxiliary pages retain context`() {
        assertThat(policy.section("https://antimsg.com/ru/channel/page/manage?app=1")).isNull()
        assertThat(policy.section("https://antimsg.com/ru/profile?app=1")).isEqualTo(AntintwrkSection.Profile)
        assertThat(policy.section("https://antimsg.com/ru/reels?app=1")).isNull()
        listOf("discover", "notifications", "hero/someone").forEach {
            assertThat(policy.section("https://antimsg.com/ru/$it?app=1")).isNull()
        }
        assertThat(policy.needsAuth("https://antimsg.com/ru/?auth=login")).isTrue()
        assertThat(policy.needsAuth("https://evil.org/ru/?auth=login")).isFalse()
    }

    @Test
    fun `first visit challenge stays inside without granting foreign redirects`() {
        val url = "https://antimsg.com/_firstvisit.html?url=https%3A%2F%2Fantimsg.com%2Fru%2Fhero-feed%3Fapp%3D1&token=test"
        assertThat(policy.embedded(url)).isEqualTo(url)
        assertThat(policy.embedded("https://antimsg.com/_firstvisit.html?url=https%3A%2F%2Fevil.org")).isNull()
        assertThat(policy.embedded("https://antimsg.com/_firstvisit.html?url=https%3A%2F%2Fantimsg.com%2F_firstvisit.html")).isNull()
    }

    @Test
    fun `main document errors show retry but subresource errors do not hide content`() = runAndroidComposeUiTest {
        lateinit var controller: AntintwrkWebFeedController
        setSafeContent {
            ElementTheme {
                controller = rememberAntintwrkWebFeedController()
                AntintwrkWebFeed(controller, backEnabled = true)
            }
        }
        val error = android.webkit.WebResourceResponse("text/plain", "utf-8", java.io.ByteArrayInputStream(byteArrayOf())).apply {
            setStatusCodeAndReasonPhrase(503, "Service Unavailable")
        }
        runOnIdle {
            val view = controller.view!!
            view.webViewClient.onReceivedHttpError(view, request(false), error)
            assertThat(controller.failed).isFalse()
            view.webViewClient.onReceivedHttpError(view, request(true), error)
        }
        onNodeWithText("The social network is temporarily unavailable. We will retry automatically. You can continue using the messenger.").assertIsDisplayed()
        onNodeWithText("Retry").performClick()
        runOnIdle { assertThat(controller.failed).isFalse() }
    }

    private fun request(main: Boolean) = object : android.webkit.WebResourceRequest {
        override fun getUrl() = android.net.Uri.parse("https://antimsg.com/en/hero-feed?app=1")
        override fun isForMainFrame() = main
        override fun isRedirect() = false
        override fun hasGesture() = false
        override fun getMethod() = "GET"
        override fun getRequestHeaders(): Map<String, String> = emptyMap()
    }

    @Test
    fun `webview loads real content and keeps its instance while switching sections`() = runAndroidComposeUiTest {
        val visible = mutableStateOf(true)
        lateinit var controller: AntintwrkWebFeedController
        setSafeContent {
            ElementTheme {
                controller = rememberAntintwrkWebFeedController()
                if (visible.value) AntintwrkWebFeed(controller, backEnabled = true)
            }
        }
        lateinit var original: android.webkit.WebView
        runOnIdle {
            original = controller.view!!
            assertThat(original.url).contains("/hero-feed?app=1")
            assertThat(original.settings.javaScriptEnabled).isTrue()
            assertThat(original.settings.domStorageEnabled).isTrue()
            assertThat(original.settings.useWideViewPort).isTrue()
            assertThat(original.settings.loadWithOverviewMode).isTrue()
            assertThat(original.settings.textZoom).isEqualTo(100)
            assertThat(original.settings.builtInZoomControls).isFalse()
            assertThat(original.settings.supportZoom()).isFalse()
            assertThat(original.settings.displayZoomControls).isFalse()
            assertThat(original.settings.allowFileAccess).isFalse()
            assertThat(original.settings.allowContentAccess).isFalse()
            assertThat(original.settings.mixedContentMode).isEqualTo(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW)
            visible.value = false
        }
        runOnIdle { visible.value = true }
        runOnIdle { assertThat(controller.view).isSameInstanceAs(original) }
    }
    @Test
    fun `successful first feed mounts hidden roots without replacing the visible webview`() = runAndroidComposeUiTest {
        lateinit var controller: AntintwrkWebFeedController
        setSafeContent {
            ElementTheme {
                controller = rememberAntintwrkWebFeedController()
                controller.ownProfileIdentity = "tester"
                AntintwrkWebFeed(controller, backEnabled = true)
            }
        }
        lateinit var feed: WebView
        runOnIdle {
            feed = controller.view!!
            assertThat(controller.page(AntintwrkSection.Communities)).isNull()
            assertThat(controller.page(AntintwrkSection.Profile)).isNull()
            controller.pageFinished(feed, policy.feed("en"))
            controller.feedReady(feed, policy.feed("en"))
        }
        runOnIdle {
            assertThat(controller.view).isSameInstanceAs(feed)
            assertThat(controller.currentUrl).isEqualTo(policy.feed("en"))
            assertThat(controller.pageUrl(AntintwrkSection.Communities)).isEqualTo(policy.root("en", AntintwrkSection.Communities))
            assertThat(controller.pageUrl(AntintwrkSection.Profile)).isEqualTo(policy.root("en", AntintwrkSection.Profile, "tester"))
            assertThat(controller.page(AntintwrkSection.Profile)).isNotNull()
            assertThat(controller.page(AntintwrkSection.Communities)!!.visibility).isEqualTo(android.view.View.INVISIBLE)
        }
    }

    @Test
    fun `native tabs keep separate webviews and web cross-section back restores the original page`() = runAndroidComposeUiTest {
        lateinit var controller: AntintwrkWebFeedController
        setSafeContent {
            ElementTheme {
                controller = rememberAntintwrkWebFeedController()
                AntintwrkWebFeed(controller, backEnabled = true)
            }
        }
        lateinit var feed: android.webkit.WebView
        runOnIdle {
            feed = controller.view!!
            controller.selectSection(AntintwrkSection.Communities, policy.root("en", AntintwrkSection.Communities))
        }
        lateinit var communities: android.webkit.WebView
        runOnIdle {
            communities = controller.view!!
            assertThat(communities).isNotSameInstanceAs(feed)
            controller.selectSection(AntintwrkSection.Feed, policy.feed("en"))
        }
        runOnIdle {
            assertThat(controller.view).isSameInstanceAs(feed)
            controller.navigate("https://antimsg.com/en/group/example?app=1")
        }
        runOnIdle {
            assertThat(controller.section).isEqualTo(AntintwrkSection.Feed)
            assertThat(controller.additionalActive).isTrue()
            assertThat(controller.view).isNotSameInstanceAs(communities)
            assertThat(controller.view).isNotSameInstanceAs(feed)
            controller.back()
        }
        runOnIdle {
            assertThat(controller.section).isEqualTo(AntintwrkSection.Feed)
            assertThat(controller.view).isSameInstanceAs(feed)
        }
    }

    @Test
    fun `native web signals reject foreign origins iframes invented urls and oversized input`() {
        val controller = AntintwrkWebFeedController()
        var auth = 0
        val current = "https://antimsg.com/en/hero-feed?app=1"
        fun send(origin: String, main: Boolean, message: String) =
            AntintwrkNativeWebSignals.accept(controller, policy, current, origin, main, message, onAuthRequired = { auth++ })
        send("https://evil.org", true, """{"type":"auth-required"}""")
        send("https://antintwrk.com", true, """{"type":"auth-required"}""")
        send("https://antimsg.com", false, """{"type":"auth-required"}""")
        send("https://antimsg.com", true, """{"type":"navigation","url":"https://antimsg.com/en/profile?app=1"}""")
        send("https://antimsg.com", true, "x".repeat(2049))
        assertThat(auth).isEqualTo(0)
        assertThat(controller.currentUrl).isNull()
        send("https://evil.org", true, """{"type":"editor","active":true}""")
        assertThat(controller.composing).isFalse()
        send("https://antimsg.com", true, """{"type":"editor","active":"true"}""")
        assertThat(controller.composing).isFalse()
        send("https://antimsg.com", true, """{"type":"editor","active":true}""")
        assertThat(controller.composing).isTrue()
        send("https://antimsg.com", true, """{"type":"editor","active":false}""")
        assertThat(controller.composing).isFalse()
        send("https://antimsg.com", true, """{"type":"changed"}""")
        assertThat(controller.refreshVersion).isEqualTo(1)
        send("https://antimsg.com", true, """{"type":"auth-required"}""")
        assertThat(auth).isEqualTo(1)
    }

    @Test
    fun `back from a messenger profile returns through the host once`() {
        val controller = AntintwrkWebFeedController()
        var returns = 0
        controller.onReturnToMessenger = { returns++ }
        controller.openMessengerProfile("https://antimsg.com/en/hero/123?app=1")
        assertThat(controller.atMessengerProfileRoot()).isTrue()
        controller.back()
        assertThat(returns).isEqualTo(1)
        assertThat(controller.atMessengerProfileRoot()).isFalse()
        controller.back()
        assertThat(returns).isEqualTo(1)
    }

    @Test
    fun `own profile opened from messenger also uses the additional page and returns to messenger`() {
        val controller = AntintwrkWebFeedController()
        controller.ownProfileIdentity = "123"
        val root = policy.root("en", AntintwrkSection.Profile, "123")
        val own = mockk<WebView>(relaxed = true)
        controller.registerPage(AntintwrkSection.Profile, own, root)
        controller.pageFinished(own, root)
        controller.openMessengerProfile(root)
        val extra = mockk<WebView>(relaxed = true)
        controller.registerPage(null, extra, root)
        controller.visited(root, policy)
        assertThat(controller.view).isSameInstanceAs(extra)
        assertThat(controller.canNavigateInPage(extra, root)).isTrue()
        assertThat(controller.atMessengerProfileRoot()).isTrue()
        verify(exactly = 0) { own.loadUrl(any()) }
    }
}
