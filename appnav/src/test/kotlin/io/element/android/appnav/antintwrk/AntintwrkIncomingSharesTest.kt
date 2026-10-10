// SPDX-License-Identifier: AGPL-3.0-only
@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package io.element.android.appnav.antintwrk

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.element.android.compound.theme.ElementTheme
import io.element.android.tests.testutils.robolectric.RobolectricTest
import io.element.android.tests.testutils.setSafeContent
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class AntintwrkIncomingSharesTest : RobolectricTest() {
    private val app = "com.antintwrk.app.debug"
    private val photo = Uri.parse("content://gallery/photo/1")

    @Test
    fun `text and urls are received as plain text including HTML and line breaks`() {
        val text = "<script>alert(1)</script>\nhttps://example.com/?x=1"
        val input = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        assertThat(antintwrkIncomingShareInput(input, app)).isEqualTo(text to emptyList<Uri>())
    }

    @Test
    fun `gallery stream and clipdata fallback preserve order without duplicates`() {
        val second = Uri.parse("content://gallery/photo/2")
        val input = Intent(Intent.ACTION_SEND_MULTIPLE).setType("image/*").apply {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(photo, second))
            clipData = ClipData.newRawUri("photo", photo).apply { addItem(ClipData.Item(second)) }
        }
        assertThat(antintwrkIncomingShareInput(input, app).second).containsExactly(photo, second).inOrder()
        input.removeExtra(Intent.EXTRA_STREAM)
        assertThat(antintwrkIncomingShareInput(input, app).second).containsExactly(photo, second).inOrder()
    }

    @Test
    fun `private app URIs file paths network URIs malformed and excessive data are rejected atomically`() {
        listOf(
            "file:///data/private",
            "https://example.com/file",
            "content://$app.fileprovider/private",
            "content://$app/private",
            "content:///missing"
        ).forEach {
            val input = Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, Uri.parse(it))
            assertThat(runCatching { antintwrkIncomingShareInput(input, app) }.isFailure).isTrue()
        }
        val longText = Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT, "x".repeat(10_001))
        assertThat(runCatching { antintwrkIncomingShareInput(longText, app) }.isFailure).isTrue()
        val many = Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(List(51) { photo }))
        assertThat(runCatching { antintwrkIncomingShareInput(many, app) }.isFailure).isTrue()
        assertThat(runCatching { antintwrkIncomingShareInput(Intent(), app) }.isFailure).isTrue()
    }

    @Test
    fun `stream copy cannot exceed limits even when provider omits or lies about size`() = runTest {
        val output = ByteArrayOutputStream()
        assertThat(runCatching { antintwrkCopySharedStream(ByteArrayInputStream(ByteArray(1025)), output, 1024) }.isFailure).isTrue()
        assertThat(output.size()).isEqualTo(0)
        assertThat(antintwrkCopySharedStream(ByteArrayInputStream(byteArrayOf(1, 2)), output, 2)).isEqualTo(2)
        assertThat(output.toByteArray()).asList().containsExactly(1.toByte(), 2.toByte()).inOrder()
        assertThat(runCatching { antintwrkCopySharedStream(ByteArrayInputStream(byteArrayOf()), output, 2) }.isFailure).isTrue()
        assertThat(antintwrkSharedExtension("image/svg+xml")).isNull()
        assertThat(antintwrkSharedExtension("application/pdf")).isNull()
    }

    @Test
    fun `source badge follows applied social origin not messenger transport`() {
        assertThat(antintwrkServerLabel(AntintwrkSocialOrigins.RUSSIAN)).isEqualTo("R")
        assertThat(antintwrkServerLabel(AntintwrkSocialOrigins.PUBLIC)).isEqualTo("G")
        assertThat(antintwrkServerLabel("https://attacker.example")).isEqualTo("?")
    }
    @Test
    fun `share confirmation opens social draft and logout clears pending content`() = runAndroidComposeUiTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val controller = AntintwrkWebFeedController().apply {
            loading = false
            origin = AntintwrkSocialOrigins.PUBLIC
            profileName = "QA actor"
        }
        var social = false
        runBlocking { AntintwrkIncomingShares.receive(context, Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "QA share")) }
        AntintwrkIncomingShares.discardOtherActor("@u42:antintwrk.com")
        assertThat(AntintwrkIncomingShares.requests.value).isNotNull()
        setSafeContent {
            ElementTheme { AntintwrkIncomingShareView("@u42:antintwrk.com", controller, "en", { social = true }) }
        }
        onNodeWithText("Create publication").performClick()
        runOnIdle {
            assertThat(social).isTrue()
            assertThat(controller.currentUrl).isEqualTo("https://antintwrk.com/en/hero-compose?app=1")
            assertThat(controller.incomingDelivery?.text).isEqualTo("QA share")
            assertThat(controller.incomingDelivery?.actor).isEqualTo("@u42:antintwrk.com")
            assertThat(AntintwrkIncomingShares.requests.value).isNull()
            controller.destroy()
        }
        runBlocking { AntintwrkIncomingShares.receive(context, Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "Another draft")) }
        AntintwrkIncomingShares.clear()
        assertThat(AntintwrkIncomingShares.requests.value).isNull()
    }
    @Test
    fun `resource capabilities deny stale tokens other actors navigation and ordinary paths stay ordinary`() {
        val page = WebView(ApplicationProvider.getApplicationContext())
        val controller = AntintwrkWebFeedController().apply { incomingActor = "@u42:antintwrk.com" }
        fun resource(path: String, main: Boolean = false, method: String = "GET") = object : WebResourceRequest {
            override fun getUrl() = Uri.parse("https://antintwrk.com$path")
            override fun isForMainFrame() = main
            override fun isRedirect() = false
            override fun hasGesture() = false
            override fun getMethod() = method
            override fun getRequestHeaders() = emptyMap<String, String>()
        }
        assertThat(controller.incomingResource(page, resource("/ordinary.png"))).isNull()
        assertThat(controller.incomingResource(page, resource("/_native-share/expired/0"))?.statusCode).isEqualTo(403)
        val shared = AntintwrkIncomingShare("lease", "@u42:antintwrk.com", "")
        controller.incomingResourceLease = AntintwrkShareResourceLease(page, shared, AntintwrkSocialOrigins.PUBLIC, Long.MAX_VALUE)
        assertThat(controller.incomingResource(page, resource("/_native-share/wrong/0"))?.statusCode).isEqualTo(403)
        assertThat(controller.incomingResource(page, resource("/_native-share/lease/0", main = true))?.statusCode).isEqualTo(403)
        assertThat(controller.incomingResource(page, resource("/_native-share/lease/0", method = "POST"))?.statusCode).isEqualTo(403)
        controller.incomingActor = "@u43:antintwrk.com"
        assertThat(controller.incomingResource(page, resource("/_native-share/lease/0"))?.statusCode).isEqualTo(403)
        controller.clearIncomingDelivery()
        assertThat(controller.incomingResourceLease).isNull()
        page.destroy()
    }
    @Test
    fun `import failure keeps its explanation when the composer is open`() = runAndroidComposeUiTest {
        val controller = AntintwrkWebFeedController().apply { composing = true }
        AntintwrkIncomingShares.failedDelivery(AntintwrkIncomingShare("failed", "@u42:antintwrk.com", ""), draft = false)
        setSafeContent { ElementTheme { AntintwrkIncomingShareView("@u42:antintwrk.com", controller, "en", {}) } }
        onNodeWithText("Could not import this content.", substring = true).assertExists()
        onNodeWithText("Create publication").assertDoesNotExist()
        runOnIdle { AntintwrkIncomingShares.clear() }
    }
}
