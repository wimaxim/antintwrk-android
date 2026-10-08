/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JsResult
import android.webkit.RenderProcessGoneDetail
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import io.element.android.appnav.BuildConfig
import io.element.android.appnav.R
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream

class AntintwrkWebFeedController {
    private val pages = mutableMapOf<AntintwrkSection, WebView>()
    private val urls = mutableMapOf<AntintwrkSection, String>()
    private val crossSectionHistory = ArrayDeque<AntintwrkSection>()
    var ownProfileIdentity: String? = null
    internal var onReturnToMessenger: (() -> Unit)? = null
    internal var messengerProfileDepth by mutableStateOf<Int?>(null)
    internal var messengerProfilePendingFinish by mutableStateOf(false)
    internal fun openMessengerProfile(url: String) {
        crossSectionHistory.clear()
        select(AntintwrkSection.Profile, url)
        val page = view
        messengerProfileDepth = if (page?.url == url) page.copyBackForwardList().currentIndex else (page?.copyBackForwardList()?.currentIndex ?: -1) + 1
        messengerProfilePendingFinish = true
        navigate(url)
    }
    internal fun finishMessengerProfileNavigation(page: WebView) {
        if (messengerProfilePendingFinish && section == AntintwrkSection.Profile && view === page) {
            messengerProfileDepth = page.copyBackForwardList().currentIndex
            messengerProfilePendingFinish = false
        }
    }
    internal fun atMessengerProfileRoot(): Boolean = messengerProfileDepth?.let {
        section == AntintwrkSection.Profile && (messengerProfilePendingFinish || (view?.copyBackForwardList()?.currentIndex ?: 0) <= it)
    } == true
    internal val popups = mutableSetOf<WebView>()
    var section by mutableStateOf(AntintwrkSection.Feed)
    var view: WebView?
        get() = pages[section]
        set(value) {
            if (value == null) pages.remove(section) else pages[section] = value
        }
    var loading by mutableStateOf(true)
    var failed by mutableStateOf(false)
    var canGoBack by mutableStateOf(false)
    var launchFilePicker: ((Intent) -> Unit)? = null
    internal var document by mutableStateOf<AntintwrkWebDocument?>(null)
    var launchDocumentSave: ((String) -> Unit)? = null
    var fileCallback: ValueCallback<Array<Uri>>? = null
    var allowMultipleFiles = false
    internal var prompt by mutableStateOf<AntintwrkWebPrompt?>(null)
    var fullscreen by mutableStateOf<View?>(null)
    var fullscreenCallback: WebChromeClient.CustomViewCallback? = null
    var notifications by mutableStateOf(0)
    var refreshVersion by mutableStateOf(0)
    var currentUrl by mutableStateOf<String?>(null)
    var onSection: (AntintwrkSection) -> Unit = {}
    var onMessenger: ((String?) -> Unit)? = null
    private val policy = AntintwrkWebPolicy(BuildConfig.ANTINTWRK_SOCIAL_ORIGIN)
    fun closeFullscreen() {
        fullscreen = null
        fullscreenCallback?.onCustomViewHidden()
        fullscreenCallback = null
    }
    internal fun nativeSection(url: String): AntintwrkSection? {
        val parts = Uri.parse(url).pathSegments
        return if (ownProfileIdentity != null && parts.getOrNull(1) == "hero" &&
            parts.getOrNull(2) == ownProfileIdentity) {
                AntintwrkSection.Profile
            } else {
                policy.section(url)
            }
    }
    fun selectSection(destination: AntintwrkSection, root: String) {
        messengerProfileDepth = null
        messengerProfilePendingFinish = false
        crossSectionHistory.clear()
        if (destination == section) {
            val previousUrl = currentUrl
            select(destination, root)
            if (previousUrl != root) view?.loadUrl(root) else view?.scrollTo(0, 0)
        } else {
            select(destination, urls[destination] ?: root)
        }
    }
    private fun select(destination: AntintwrkSection, url: String) {
        closeFullscreen()
        section = destination
        currentUrl = url
        urls[destination] = url
        failed = false
        loading = pages[destination] == null
        canGoBack = pages[destination]?.canGoBack() == true || crossSectionHistory.isNotEmpty()
        onSection(destination)
    }
    fun navigate(url: String) {
        if (interceptMessenger(url)) return
        val destination = nativeSection(url)
        if (destination != null && destination != section) {
            crossSectionHistory.addLast(section)
            select(destination, url)
        }
        currentUrl = url
        urls[section] = url
        view?.loadUrl(url)
    }
    internal fun interceptMessenger(url: String): Boolean {
        val callback = onMessenger ?: return false
        val link = policy.messengerLink(url) ?: return false
        callback(link.target)
        return true
    }
    internal fun visited(url: String, policy: AntintwrkWebPolicy) {
        if (policy.embedded(url) == null) return
        val destination = nativeSection(url)
        if (destination != null && destination != section) {
            val current = view
            crossSectionHistory.addLast(section)
            pages.remove(section)
            pages.remove(destination)?.takeIf { it !== current }?.destroy()
            section = destination
            current?.let { pages[destination] = it }
        }
        currentUrl = url
        urls[section] = url
        canGoBack = view?.canGoBack() == true || crossSectionHistory.isNotEmpty()
        onSection(section)
    }
    fun back() {
        if (atMessengerProfileRoot() && onReturnToMessenger != null) {
            messengerProfileDepth = null
            messengerProfilePendingFinish = false
            onReturnToMessenger?.invoke()
        } else if (view?.canGoBack() == true) {
            view?.goBack()
        } else if (crossSectionHistory.isNotEmpty()) {
            val previous = crossSectionHistory.removeLast()
            select(previous, urls[previous] ?: policy.root("en", previous))
        }
    }
    fun retry() {
        failed = false
        loading = true
        view?.reload()
    }
    fun rendererGone(renderer: WebView) {
        val active = view === renderer
        if (active) {
            fileCallback?.onReceiveValue(null)
            fileCallback = null
            prompt?.result?.cancel()
            prompt = null
            document?.complete?.let { runCatching { it(false) } }
            document = null
            closeFullscreen()
            loading = false
            failed = true
            canGoBack = crossSectionHistory.isNotEmpty()
        }
        val tab = pages.entries.firstOrNull { it.value === renderer }?.key
        tab?.let { pages.remove(it) }
        renderer.destroy()
    }
    fun destroy() {
        messengerProfileDepth = null
        messengerProfilePendingFinish = false
        closeFullscreen()
        prompt?.result?.cancel()
        prompt = null
        document?.complete?.invoke(false)
        document = null
        fileCallback?.onReceiveValue(null)
        fileCallback = null
        pages.values.forEach {
            it.stopLoading()
            it.destroy()
        }
        pages.clear()
        popups.toList().forEach(WebView::destroy)
        popups.clear()
        urls.clear()
        crossSectionHistory.clear()
        currentUrl = null
        canGoBack = false
        section = AntintwrkSection.Feed
        notifications = 0
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
    val scope = rememberCoroutineScope()
    fun openExternal(input: String) {
        scope.launch {
            try {
                val url = policy.canonical(input)
                val target = if (policy.sameOrigin(url) && policy.outbound(url)) antintwrkExternalRedirect(url, policy) else url
                require(policy.external(target))
                uriHandler.openUri(target)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                controller.failed = true
                controller.loading = false
            }
        }
    }
    val startUrl = controller.currentUrl?.let(policy::embedded) ?: policy.root(language, controller.section, controller.ownProfileIdentity)
    val authRequired by rememberUpdatedState(onAuthRequired)
    BackHandler(backEnabled && (controller.canGoBack || controller.atMessengerProfileRoot())) { controller.back() }
    Box(Modifier.fillMaxSize()) {
        key(controller.section) {
        if (controller.view != null || !controller.failed) {
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
                        setSupportMultipleWindows(true)
                        javaScriptCanOpenWindowsAutomatically = false
                    }
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                    webChromeClient = object : WebChromeClient() {
                        override fun onCreateWindow(view: WebView, dialog: Boolean, userGesture: Boolean, result: Message): Boolean {
                            if (!userGesture || controller.view !== view) return false
                            val transport = result.obj as? WebView.WebViewTransport ?: return false
                            val popup = WebView(view.context)
                            controller.popups.add(popup)
                            popup.settings.allowFileAccess = false
                            popup.settings.allowContentAccess = false
                            popup.webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(child: WebView, request: WebResourceRequest): Boolean {
                                    val url = policy.canonical(request.url.toString())
                                    val target = policy.embedded(url)
                                    if (target != null) {
                                        controller.navigate(target)
                                    } else if (policy.external(url) || policy.outbound(url)) {
                                        openExternal(url)
                                    }
                                    controller.popups.remove(popup)
                                    popup.destroy()
                                    return true
                                }
                            }
                            transport.webView = popup
                            result.sendToTarget()
                            return true
                        }
                        override fun onCloseWindow(window: WebView) {
                            if (controller.popups.remove(window)) window.destroy()
                        }
                        override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
                            if (controller.view !== view) {
                                result.cancel()
                                return true
                            }
                            controller.prompt?.result?.cancel()
                            controller.prompt = AntintwrkWebPrompt(message.take(2000), result, false)
                            return true
                        }
                        override fun onJsConfirm(view: WebView, url: String, message: String, result: JsResult): Boolean {
                            if (controller.view !== view) {
                                result.cancel()
                                return true
                            }
                            controller.prompt?.result?.cancel()
                            controller.prompt = AntintwrkWebPrompt(message.take(2000), result, true)
                            return true
                        }
                        override fun onJsBeforeUnload(view: WebView, url: String, message: String, result: JsResult): Boolean =
                            onJsConfirm(view, url, message, result)
                        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                            controller.closeFullscreen()
                            controller.fullscreen = view
                            controller.fullscreenCallback = callback
                        }
                        override fun onHideCustomView() = controller.closeFullscreen()
                        override fun onShowFileChooser(
                            webView: WebView,
                            callback: ValueCallback<Array<Uri>>,
                            params: FileChooserParams,
                        ): Boolean {
                            controller.fileCallback?.onReceiveValue(null)
                            controller.fileCallback = null
                            if (controller.view !== webView || policy.embedded(webView.url.orEmpty()) == null) {
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
                    AntintwrkNativeWebSignals.attach(this, controller, policy) { authRequired() }
                    if (BuildConfig.ANTINTWRK_NATIVE_AUTH &&
                        runCatching { WebViewFeature.isFeatureSupported(WebViewFeature.WEB_AUTHENTICATION) }.getOrDefault(false)) {
                        WebSettingsCompat.setWebAuthenticationSupport(settings, WebSettingsCompat.WEB_AUTHENTICATION_SUPPORT_FOR_APP)
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            if (!request.isForMainFrame) return false
                            val url = policy.canonical(request.url.toString())
                            if (controller.interceptMessenger(url)) return true
                            if (BuildConfig.ANTINTWRK_NATIVE_AUTH && policy.needsAuth(url)) {
                                authRequired()
                                return true
                            }
                            val target = policy.embedded(url)
                            if (target != null) {
                                if (target == url &&
                                    (controller.nativeSection(target) == null || controller.nativeSection(target) == controller.section)) {
                                        return false
                                    }
                                controller.navigate(target)
                            } else if ((policy.external(url) || policy.outbound(url)) &&
                                (request.hasGesture() || policy.outbound(url) || (request.isRedirect && policy.embedded(view.url.orEmpty()) != null))) {
                                openExternal(url)
                            } else {
                                controller.failed = true
                                controller.loading = false
                            }
                            return true
                        }
                        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                            if (request.url.host in setOf("antintwrk.com", "www.antintwrk.com")) {
                                return WebResourceResponse(
                                    "text/plain",
                                    "UTF-8",
                                    403,
                                    "Legacy social domain blocked",
                                    emptyMap(),
                                    ByteArrayInputStream(ByteArray(0))
                                )
                            }
                            return null
                        }
                        override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
                            if (controller.view === view) controller.visited(url, policy)
                        }
                        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                            controller.rendererGone(view)
                            return true
                        }
                        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                            if (controller.view === view) {
                                controller.loading = true
                                controller.failed = false
                            }
                        }
                        override fun onPageFinished(view: WebView, url: String?) {
                            if (controller.view === view) {
                                controller.loading = false
                                url?.let { controller.visited(it, policy) }
                                controller.finishMessengerProfileNavigation(view)
                            }
                        }
                        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                            if (request.isForMainFrame && controller.view === view) {
                                controller.failed = true
                                controller.loading = false
                            }
                        }
                        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                            if (request.isForMainFrame && controller.view === view) {
                                controller.failed = true
                                controller.loading = false
                                if (response.statusCode == 401) authRequired()
                            }
                        }
                    }
                    setDownloadListener { url, _, _, _, _ ->
                        if (policy.external(url) || policy.outbound(url)) openExternal(url)
                    }
                    loadUrl(startUrl)
                }).also { view ->
                    (view.parent as? ViewGroup)?.removeView(view)
                    view.onResume()
                }
            },
            update = {
                it.visibility = if (controller.failed) View.INVISIBLE else View.VISIBLE
                it.onResume()
            },
            onRelease = { view ->
                view.post { if (!view.isAttachedToWindow) view.onPause() }
            },
        )
        }
        }
        controller.fullscreen?.let { customView ->
            AndroidView(modifier = Modifier.fillMaxSize(), factory = {
                (customView.parent as? ViewGroup)?.removeView(customView)
                customView
            })
        }
        AntintwrkWebDialogs(controller)
        if (controller.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (controller.failed) {
            Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.screen_ant_web_error))
                TextButton(stringResource(R.string.screen_ant_web_retry), controller::retry)
            }
        }
    }
}
