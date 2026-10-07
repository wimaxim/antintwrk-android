/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
@file:OptIn(ExperimentalTestApi::class)

package io.element.android.appnav.antintwrk

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
import org.junit.Test

class AntintwrkWebFeedTest : RobolectricTest() {
    private val policy = AntintwrkWebPolicy("https://antintwrk.com")

    @Test
    fun `feed uses configured origin supported language and guest presentation`() {
        assertThat(policy.feed("ru")).isEqualTo("https://antintwrk.com/ru/hero-feed?app=1")
        assertThat(policy.feed("ja")).isEqualTo("https://antintwrk.com/en/hero-feed?app=1")
        assertThat(AntintwrkWebPolicy("https://social.example.org").feed("ar"))
            .isEqualTo("https://social.example.org/ar/hero-feed?app=1")
    }

    @Test
    fun `internal navigation retains filters repeated query values and fragment`() {
        val target = policy.embedded("https://antintwrk.com/ru/hero-feed?view=memes&scope=latest&tag=a&tag=b&app=0#comments")
        assertThat(target).isEqualTo("https://antintwrk.com/ru/hero-feed?view=memes&scope=latest&tag=a&tag=b&app=1#comments")
        assertThat(policy.embedded(target!!)).isEqualTo(target)
        assertThat(policy.embedded("https://antintwrk.com/en/discover?q=hello%20world"))
            .isEqualTo("https://antintwrk.com/en/discover?q=hello%20world&app=1")
    }

    @Test
    fun `foreign origins insecure urls account routes and arbitrary schemes are not embedded`() {
        listOf(
            "http://antintwrk.com/ru/hero-feed",
            "https://antintwrk.com.evil.org/ru/hero-feed",
            "https://evil.org/ru/hero-feed",
            "https://name@antintwrk.com/ru/hero-feed",
            "https://antintwrk.com:444/ru/hero-feed",
            "https://antintwrk.com/en/?auth=login",
            "https://antintwrk.com/en/goto",
            "https://antintwrk.com/register.php",
            "javascript:alert(1)",
            "file:///etc/passwd",
            "intent://anything",
            "https://antintwrk.com/xx/hero-feed",
        ).forEach { assertThat(policy.embedded(it)).isNull() }
        assertThat(policy.external("https://example.org/article")).isTrue()
        assertThat(policy.external("javascript:alert(1)")).isFalse()
        assertThat(policy.external("intent://anything")).isFalse()
    }

    @Test
    fun `social pages embed but outbound routes preserve the browser transition`() {
        listOf("communities", "profile", "hero-compose", "notifications", "people", "monetization", "messages").forEach {
            assertThat(policy.embedded("https://antintwrk.com/ru/$it")).endsWith("?app=1")
        }
        listOf("/goto?to=game", "/go/?url=https%3A%2F%2Fexample.org", "/ru/goto?to=game", "/en/go/").forEach {
            val url = "https://antintwrk.com$it"
            assertThat(policy.outbound(url)).isTrue()
            assertThat(policy.embedded(url)).isNull()
        }
        assertThat(policy.outbound("https://antintwrk.com/ru/hero-feed?q=/goto")).isFalse()
        assertThat(policy.outbound("https://antintwrk.com/ru/goto-other")).isFalse()
    }

    @Test
    fun `web routes select their native section and auxiliary pages retain context`() {
        assertThat(policy.section("https://antintwrk.com/ru/channel/page/manage?app=1"))
            .isEqualTo(AntintwrkSection.Communities)
        assertThat(policy.section("https://antintwrk.com/ru/profile?app=1")).isEqualTo(AntintwrkSection.Profile)
        assertThat(policy.section("https://antintwrk.com/ru/reels?app=1")).isEqualTo(AntintwrkSection.Feed)
        listOf("discover", "notifications", "hero/someone").forEach {
            assertThat(policy.section("https://antintwrk.com/ru/$it?app=1")).isNull()
        }
        assertThat(policy.needsAuth("https://antintwrk.com/ru/?auth=login")).isTrue()
        assertThat(policy.needsAuth("https://evil.org/ru/?auth=login")).isFalse()
    }

    @Test
    fun `first visit challenge stays inside without granting foreign redirects`() {
        val url = "https://antintwrk.com/_firstvisit.html?url=https%3A%2F%2Fantintwrk.com%2Fru%2Fhero-feed%3Fapp%3D1&token=test"
        assertThat(policy.embedded(url)).isEqualTo(url)
        assertThat(policy.embedded("https://antintwrk.com/_firstvisit.html?url=https%3A%2F%2Fevil.org")).isNull()
        assertThat(policy.embedded("https://antintwrk.com/_firstvisit.html?url=https%3A%2F%2Fantintwrk.com%2F_firstvisit.html")).isNull()
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
        onNodeWithText("Could not load the feed. Check your connection and try again.").assertIsDisplayed()
        onNodeWithText("Retry").performClick()
        runOnIdle { assertThat(controller.failed).isFalse() }
    }

    private fun request(main: Boolean) = object : android.webkit.WebResourceRequest {
        override fun getUrl() = android.net.Uri.parse("https://antintwrk.com/en/hero-feed?app=1")
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
            assertThat(original.settings.allowFileAccess).isFalse()
            assertThat(original.settings.allowContentAccess).isFalse()
            assertThat(original.settings.mixedContentMode).isEqualTo(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW)
            visible.value = false
        }
        runOnIdle { visible.value = true }
        runOnIdle { assertThat(controller.view).isSameInstanceAs(original) }
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
            controller.navigate("https://antintwrk.com/en/group/example?app=1")
        }
        runOnIdle {
            assertThat(controller.section).isEqualTo(AntintwrkSection.Communities)
            assertThat(controller.view).isSameInstanceAs(communities)
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
        val current = "https://antintwrk.com/en/hero-feed?app=1"
        fun send(origin: String, main: Boolean, message: String) =
            AntintwrkNativeWebSignals.accept(controller, policy, current, origin, main, message, onAuthRequired = { auth++ })
        send("https://evil.org", true, """{"type":"auth-required"}""")
        send("https://antintwrk.com", false, """{"type":"auth-required"}""")
        send("https://antintwrk.com", true, """{"type":"navigation","url":"https://antintwrk.com/en/profile?app=1"}""")
        send("https://antintwrk.com", true, "x".repeat(2049))
        assertThat(auth).isEqualTo(0)
        assertThat(controller.currentUrl).isNull()
        send("https://antintwrk.com", true, """{"type":"changed"}""")
        assertThat(controller.refreshVersion).isEqualTo(1)
        send("https://antintwrk.com", true, """{"type":"auth-required"}""")
        assertThat(auth).isEqualTo(1)
    }
}
