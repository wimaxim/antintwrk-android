// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Real Chromium chooser/file read, without social credentials, requests, uploads or publication. */
class AntintwrkShareFixtureActivity : Activity() {
    val complete = CountDownLatch(1)
    var result = ""
    private lateinit var page: WebView
    private val controller = AntintwrkWebFeedController()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        page = WebView(this)
        page.settings.javaScriptEnabled = true
        setContentView(page)
        val directory = File(cacheDir, "share-fixture").apply { mkdirs() }
        val files = listOf("jpg" to "image/jpeg", "mp4" to "video/mp4").map { (extension, mime) ->
            val file = File(directory, "fixture.$extension").apply { writeText("QA-$extension") }
            AntintwrkSharedFile(FileProvider.getUriForFile(this, "$packageName.fileprovider", file), mime)
        }
        controller.origin = AntintwrkSocialOrigins.PUBLIC
        controller.incomingActor = "@u42:antintwrk.com"
        controller.routedSession = AntintwrkAuthSession(AntintwrkAuthStage.AUTHENTICATED, "a".repeat(64), matrixUserId = controller.incomingActor)
        controller.view = page
        controller.loading = false
        controller.incomingDelivery = AntintwrkIncomingShare("fixture", controller.incomingActor, "<script>must stay text</script>", files)
        controller.incomingImportBusy = true
        page.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: android.webkit.WebResourceRequest): android.webkit.WebResourceResponse? =
                controller.incomingResource(view, request) ?: super.shouldInterceptRequest(view, request)

            override fun onPageFinished(view: WebView, url: String?) {
                controller.deliverIncomingShare(page)
            }
        }
        val html = """
            <body class="antintwrk-app-view"><div data-hero-composer>
            <input data-publication-type type=radio value=post checked>
            <div data-rich-surface contenteditable=true></div>
            <input type=file accept="image/jpeg,image/png,image/webp" multiple data-photo-input>
            <input type=file accept="video/mp4,video/quicktime,video/webm,video/x-m4v" data-publication-video-input>
            </div><script>
            window.qaRead=[];
            document.querySelectorAll('input[type=file]').forEach(input=>input.addEventListener('change',async()=>{
                for(const file of input.files)window.qaRead.push(await file.text());
            }));</script></body>
        """.trimIndent()
        page.loadDataWithBaseURL("${controller.origin}/en/hero-compose?app=1", html, "text/html", "UTF-8", "${controller.origin}/en/hero-compose?app=1")
    }

    fun check() {
        page.evaluateJavascript(
            "JSON.stringify({text:document.querySelector('[data-rich-surface]').textContent,files:window.qaRead,importer:typeof window.antintwrkImportNativeShare,url:location.href,script:document.querySelector('[data-rich-surface] script')!==null})"
        ) {
            result =
                it + "; native url=" + page.url + "; active=" + (controller.view === page) + "; actor=" + controller.incomingActor + "; routed=" +
                    controller.routedSession?.matrixUserId +
                    "; busy=" +
                    controller.incomingImportBusy +
                    "; failed=" +
                    AntintwrkIncomingShares.requests.value?.failed
            if (it.contains("QA-jpg") && it.contains("QA-mp4")) complete.countDown()
        }
    }
    override fun onDestroy() {
        controller.destroy()
        File(cacheDir, "share-fixture").deleteRecursively()
        super.onDestroy()
    }
}

@RunWith(AndroidJUnit4::class)
class AntintwrkShareWebViewTest {
    @Test
    fun realChromiumReceivesBothStagedMediaAndPlainText() {
        val testContext = InstrumentationRegistry.getInstrumentation().context
        ActivityScenario.launch<AntintwrkShareFixtureActivity>(Intent(testContext, AntintwrkShareFixtureActivity::class.java)).use { scenario ->
            var latch: CountDownLatch? = null
            for (attempt in 0 until 100) {
                scenario.onActivity { activity ->
                    activity.check()
                    latch = activity.complete
                }
                if (latch?.await(100, TimeUnit.MILLISECONDS) == true) break
            }
            var state = ""
            scenario.onActivity { state = it.result }
            assertTrue("Both staged files must be read by Chromium: $state", latch?.count == 0L)
            scenario.onActivity {
                assertTrue(it.result.contains("must stay text"))
                assertTrue(it.result.contains("script\\\":false"))
            }
        }
    }
}
