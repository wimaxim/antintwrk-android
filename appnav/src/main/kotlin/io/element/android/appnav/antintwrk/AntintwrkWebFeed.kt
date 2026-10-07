/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
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

/** Query flag selects a presentation; it grants no account or content access. */
internal class AntintwrkWebPolicy(private val origin: String) {
    private val base = Uri.parse(origin)
    fun feed(language: String): String {
        val supported = setOf("en", "ru", "uk", "de", "es", "fr", "it", "ar", "zh")
        return "$origin/${language.takeIf { it in supported } ?: "en"}/hero-feed?app=1"
    }
    fun embedded(url: String): String? {
        val uri = Uri.parse(url)
        if (uri.scheme != "https" || uri.host != base.host || uri.port != base.port || uri.userInfo != null) return null
        // Keep the engine's first-visit cookie challenge inside WebView, unchanged.
        if (uri.path == "/_firstvisit.html") {
            val destination = uri.getQueryParameter("url") ?: return null
            if (Uri.parse(destination).path == "/_firstvisit.html" || embedded(destination) == null) return null
            return url
        }
        val parts = uri.pathSegments
        if (parts.size < 2 || parts[1] !in setOf("hero-feed", "hero", "channel", "group", "discover", "reels", "stories") ||
            uri.getQueryParameter("auth") != null) {
                return null
            }
        if (parts[0] !in setOf("en", "ru", "uk", "de", "es", "fr", "it", "ar", "zh")) return null
        return uri.buildUpon().clearQuery().apply {
            uri.queryParameterNames.filter { it != "app" }.forEach { name ->
                uri.getQueryParameters(name).forEach { value -> appendQueryParameter(name, value) }
            }
            appendQueryParameter("app", "1")
        }.build().toString()
    }
    fun external(url: String): Boolean = Uri.parse(url).scheme in setOf("https", "http", "mailto", "tel")
}

internal class AntintwrkWebFeedController {
    var view: WebView? = null
    var loading by mutableStateOf(true)
    var failed by mutableStateOf(false)
    var canGoBack by mutableStateOf(false)
    fun back() {
        view?.goBack()
    }
    fun retry() {
        failed = false
        view?.reload()
    }
    fun destroy() {
        view?.stopLoading()
        view?.destroy()
        view = null
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
internal fun AntintwrkWebFeed(controller: AntintwrkWebFeedController, backEnabled: Boolean) {
    val policy = remember { AntintwrkWebPolicy(BuildConfig.ANTINTWRK_SOCIAL_ORIGIN) }
    val language = LocalConfiguration.current.locales[0].language
    val uriHandler = LocalUriHandler.current
    val startUrl = policy.feed(language)
    BackHandler(backEnabled && controller.canGoBack) { controller.back() }
    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                (controller.view ?: WebView(context).apply {
                    controller.view = this
                    setBackgroundColor(Color(0xFF101214).toArgb())
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = false
                        allowContentAccess = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        mediaPlaybackRequiresUserGesture = true
                    }
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            if (!request.isForMainFrame) return false
                            val url = request.url.toString()
                            val target = policy.embedded(url)
                            if (target != null) {
                                if (target == url) return false
                                view.loadUrl(target)
                            } else if (policy.external(url) && request.hasGesture()) {
                                runCatching { uriHandler.openUri(url) }
                            } else {
                                controller.failed = true
                                controller.loading = false
                            }
                            return true
                        }
                        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                            controller.loading = true
                            controller.failed = false
                        }
                        override fun onPageFinished(view: WebView, url: String?) {
                            controller.loading = false
                            controller.canGoBack = view.canGoBack()
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
