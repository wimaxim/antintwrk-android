/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.element.android.appnav.BuildConfig
import io.element.android.appnav.R
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton

class AntintwrkWebFeedController {
    var view: WebView? = null
    var loading by mutableStateOf(true)
    var failed by mutableStateOf(false)
    var canGoBack by mutableStateOf(false)
    var section by mutableStateOf(AntintwrkSection.Feed)
    var launchFilePicker: ((Intent) -> Unit)? = null
    var fileCallback: ValueCallback<Array<Uri>>? = null
    var allowMultipleFiles = false
    var currentUrl by mutableStateOf<String?>(null)
    var onSection: (AntintwrkSection) -> Unit = {}
    private val historySections = mutableMapOf<Pair<Int, String>, AntintwrkSection>()
    fun navigate(url: String, destination: AntintwrkSection? = null) {
        destination?.let {
            section = it
            onSection(it)
        }
        view?.loadUrl(url)
    }
    internal fun visited(url: String, policy: AntintwrkWebPolicy) {
        if (policy.embedded(url) == null) return
        val index = view?.copyBackForwardList()?.currentIndex ?: 0
        section = policy.section(url) ?: historySections[index to url] ?: section
        historySections[index to url] = section
        currentUrl = url
        canGoBack = view?.canGoBack() == true
        onSection(section)
    }
    fun back() {
        view?.goBack()
    }
    fun retry() {
        failed = false
        view?.reload()
    }
    fun destroy() {
        fileCallback?.onReceiveValue(null)
        fileCallback = null
        view?.stopLoading()
        view?.destroy()
        view = null
        currentUrl = null
        canGoBack = false
        historySections.clear()
        section = AntintwrkSection.Feed
    }
}

@Composable
internal fun rememberAntintwrkWebFeedController(): AntintwrkWebFeedController {
    val controller = remember { AntintwrkWebFeedController() }
    DisposableEffect(controller) { onDispose { controller.destroy() } }
    return controller
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun AntintwrkWebFeed(controller: AntintwrkWebFeedController, backEnabled: Boolean, onAuthRequired: () -> Unit = {}) {
    val policy = remember { AntintwrkWebPolicy(BuildConfig.ANTINTWRK_SOCIAL_ORIGIN) }
    val language = LocalConfiguration.current.locales[0].language
    val uriHandler = LocalUriHandler.current
    val startUrl = policy.feed(language)
    val authRequired by rememberUpdatedState(onAuthRequired)
    BackHandler(backEnabled && controller.canGoBack) { controller.back() }
    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                (controller.view ?: WebView(context).apply {
                    controller.view = this
                    setBackgroundColor(Color(0xFF101214).toArgb())
                    settings.apply {
                        if (BuildConfig.ANTINTWRK_NATIVE_AUTH) userAgentString = AntintwrkNativeAuthClient.USER_AGENT
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = false
                        allowContentAccess = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        mediaPlaybackRequiresUserGesture = true
                    }
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                    webChromeClient = object : WebChromeClient() {
                        override fun onShowFileChooser(
                            webView: WebView,
                            callback: ValueCallback<Array<Uri>>,
                            params: FileChooserParams,
                        ): Boolean {
                            controller.fileCallback?.onReceiveValue(null)
                            controller.fileCallback = null
                            if (policy.embedded(webView.url.orEmpty()) == null) {
                                callback.onReceiveValue(null)
                                return true
                            }
                            controller.fileCallback = callback
                            controller.allowMultipleFiles = params.mode == FileChooserParams.MODE_OPEN_MULTIPLE
                            runCatching { requireNotNull(controller.launchFilePicker).invoke(params.createIntent()) }.onFailure {
                                controller.fileCallback = null
                                callback.onReceiveValue(null)
                            }
                            return true
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            if (!request.isForMainFrame) return false
                            val url = request.url.toString()
                            if (BuildConfig.ANTINTWRK_NATIVE_AUTH && policy.needsAuth(url)) {
                                authRequired()
                                return true
                            }
                            val target = policy.embedded(url)
                            if (target != null) {
                                if (target == url) return false
                                controller.navigate(target, policy.section(target))
                            } else if (policy.external(url) && (request.hasGesture() || policy.outbound(url))) {
                                runCatching { uriHandler.openUri(url) }
                            } else {
                                controller.failed = true
                                controller.loading = false
                            }
                            return true
                        }
                        override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
                            controller.visited(url, policy)
                        }
                        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                            controller.loading = true
                            controller.failed = false
                        }
                        override fun onPageFinished(view: WebView, url: String?) {
                            controller.loading = false
                            url?.let { controller.visited(it, policy) }
                        }
                        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                            if (request.isForMainFrame) {
                                controller.failed = true
                                controller.loading = false
                            }
                        }
                        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                            if (request.isForMainFrame) {
                                controller.failed = true
                                controller.loading = false
                                if (response.statusCode == 401) authRequired()
                            }
                        }
                    }
                    setDownloadListener { url, _, _, _, _ ->
                        if (policy.external(url)) runCatching { uriHandler.openUri(url) }
                    }
                    loadUrl(startUrl)
                }).also { view ->
                    (view.parent as? ViewGroup)?.removeView(view)
                    view.onResume()
                }
            },
            update = { it.visibility = if (controller.failed) View.INVISIBLE else View.VISIBLE },
            onRelease = { it.onPause() },
        )
        if (controller.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (controller.failed) {
            Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.screen_ant_web_error))
                TextButton(stringResource(R.string.screen_ant_web_retry), controller::retry)
            }
        }
    }
}
