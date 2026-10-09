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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import io.element.android.appnav.BuildConfig
import io.element.android.appnav.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream

class AntintwrkWebFeedController {
    private val pages = mutableMapOf<AntintwrkSection, WebView>()
    private val urls = mutableMapOf<AntintwrkSection, String>()
    private val pageLoading = mutableMapOf<AntintwrkSection, Boolean>()
    private val pageFailures = mutableMapOf<AntintwrkSection, Boolean>()
    internal var prewarmStarted by mutableStateOf(false)
        private set
    internal fun pageSection(page: WebView): AntintwrkSection? = pages.entries.firstOrNull { it.value === page }?.key
    internal fun page(tab: AntintwrkSection): WebView? = pages[tab]
    internal fun pageUrl(tab: AntintwrkSection): String? = urls[tab]
    internal fun registerPage(tab: AntintwrkSection, page: WebView, url: String) {
        pages[tab] = page
        urls[tab] = url
        pageLoading[tab] = true
        pageFailures[tab] = false
        if (tab == section) currentUrl = url
    }
    internal fun pageStarted(page: WebView, url: String?) {
        val tab = pages.entries.firstOrNull { it.value === page }?.key ?: return
        url?.let { urls[tab] = it }
        pageLoading[tab] = true
        pageFailures[tab] = false
        if (view === page) {
            loading = true
            failed = false
        }
    }
    internal fun pageFailed(page: WebView) {
        val tab = pages.entries.firstOrNull { it.value === page }?.key ?: return
        pageLoading[tab] = false
        pageFailures[tab] = true
        if (view === page) {
            loading = false
            failed = true
        }
    }
    internal fun pageFinished(page: WebView, url: String?) {
        val tab = pages.entries.firstOrNull { it.value === page }?.key ?: return
        url?.let { urls[tab] = it }
        pageLoading[tab] = false
        if (view === page) loading = false
    }
    internal fun feedReady(page: WebView, url: String) {
        if (pages[AntintwrkSection.Feed] === page && pageFailures[AntintwrkSection.Feed] != true &&
            urls[AntintwrkSection.Feed] == url && Uri.parse(url).path?.trimEnd('/')?.endsWith("/hero-feed") == true) {
            prewarmStarted = true
        }
    }
    private val crossSectionHistory = ArrayDeque<AntintwrkSection>()
    var ownProfileIdentity: String? = null
    private val editorStates = mutableMapOf<AntintwrkSection, Boolean>()
    private val editorDialogs = mutableMapOf<AntintwrkSection, Boolean>()
    internal var imeVisible = false
    internal var editorDialog = false
    internal var composing by mutableStateOf(false)
    internal fun updateEditor(active: Boolean, dialog: Boolean = false) {
        editorDialog = active && dialog
        editorDialogs[section] = editorDialog
        composing = active
        editorStates[section] = active
    }
    internal var profileAvatarSource: String? = null
    internal var profileName by mutableStateOf<String?>(null)
    internal var profileAvatar by mutableStateOf<android.graphics.Bitmap?>(null)
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
    internal var onAuthRequired: () -> Unit = {}
    internal var onExternalLink: (String) -> Unit = {}
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
        val sameTab = destination == section
        val previousUrl = urls[destination] ?: if (sameTab) currentUrl else null
        select(destination, root)
        if (previousUrl != root && view != null) {
            loading = true
            failed = false
            pageLoading[destination] = true
            pageFailures[destination] = false
            updateEditor(false)
            view?.loadUrl(root)
        } else if (sameTab && previousUrl == root) {
            view?.scrollTo(0, 0)
        }
    }

    private fun select(destination: AntintwrkSection, url: String) {
        closeFullscreen()
        section = destination
        editorDialog = editorDialogs[destination] == true
        composing = editorStates[destination] == true || android.net.Uri.parse(url).path?.trimEnd('/')?.endsWith("/hero-compose") == true
        currentUrl = url
        urls[destination] = url
        failed = pageFailures[destination] == true
        loading = pages[destination] == null || pageLoading[destination] == true
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
        loading = true
        failed = false
        pageLoading[section] = true
        pageFailures[section] = false
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
            // Redirect/SPA navigation uses the destination's own retained page.
            // Moving WebViews between mounted Compose keys would leave a destroyed page attached.
            urls[section] = url
            navigate(url)
            return
        }
        currentUrl = url
        urls[section] = url
        canGoBack = view?.canGoBack() == true || crossSectionHistory.isNotEmpty()
        onSection(section)
    }
    fun back() {
        if (editorDialog || (composing && android.net.Uri.parse(currentUrl.orEmpty()).path?.trimEnd('/')?.endsWith("/hero-compose") != true)) {
            view?.evaluateJavascript("window.dispatchEvent(new Event('antintwrk-close-editor'))", null)
            return
        }
        if (atMessengerProfileRoot() && onReturnToMessenger != null) {
            messengerProfileDepth = null
            messengerProfilePendingFinish = false
            onReturnToMessenger?.invoke()
        } else if (view?.canGoBack() == true) {
            loading = true
            view?.goBack()
        } else if (crossSectionHistory.isNotEmpty()) {
            val previous = crossSectionHistory.removeLast()
            select(previous, urls[previous] ?: policy.root("en", previous))
        }
    }
    fun retry() {
        if (composing || fullscreen != null) return
        val target = currentUrl?.let(policy::embedded) ?: return
        failed = false
        loading = true
        // Explicit refresh revalidates the document without replaying a form POST.
        view?.loadUrl(target, mapOf("Cache-Control" to "no-cache"))
    }

    /** Recovery uses GET and never replays a form POST or a payment submission. */
    internal fun recoverPage() {
        val target = currentUrl?.let(policy::embedded) ?: return
        if (!composing) {
            failed = false
            loading = true
            view?.loadUrl(target)
        }
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
        tab?.let {
            pages.remove(it)
            pageLoading.remove(it)
            pageFailures[it] = true
        }
        renderer.destroy()
    }
    fun destroy() {
        composing = false
        imeVisible = false
        editorDialog = false
        editorStates.clear()
        editorDialogs.clear()
        profileAvatarSource = null
        profileName = null
        profileAvatar = null
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
        pageLoading.clear()
        pageFailures.clear()
        prewarmStarted = false
        loading = true
        failed = false
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

@OptIn(ExperimentalLayoutApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun AntintwrkWebFeed(controller: AntintwrkWebFeedController, backEnabled: Boolean, onAuthRequired: () -> Unit = {}) {
    val policy = remember { AntintwrkWebPolicy(BuildConfig.ANTINTWRK_SOCIAL_ORIGIN) }
    val language = LocalConfiguration.current.locales[0].language
    controller.imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(controller.imeVisible, controller.section) {
        controller.view?.let { AntintwrkWebIme.update(it, controller, policy, install = true) }
    }
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
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(controller.failed, controller.section) {
        if (controller.failed) {
            while (controller.failed) {
                delay(15_000)
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) controller.recoverPage()
            }
        }
    }
    controller.onAuthRequired = onAuthRequired
    controller.onExternalLink = ::openExternal
    BackHandler(backEnabled && (controller.canGoBack || controller.atMessengerProfileRoot() || controller.composing)) { controller.back() }
    Box(Modifier.fillMaxSize()) {
        val tabs = AntintwrkSection.entries.filter { it == controller.section || controller.page(it) != null || controller.prewarmStarted }
        tabs.forEach { tab ->
        key(tab) {
        val active = tab == controller.section
        val startUrl = (if (active) controller.currentUrl else controller.pageUrl(tab))?.let(policy::embedded)
            ?: policy.root(language, tab, controller.ownProfileIdentity)
        if (controller.page(tab) != null || !active || !controller.failed) {
            AndroidView(
            modifier = if (active) Modifier.fillMaxSize() else Modifier.fillMaxSize().zIndex(-1f).alpha(0f).clearAndSetSemantics {},
            factory = { context ->
                (controller.page(tab) ?: AntintwrkRefreshWebView(context).apply {
                    canRefresh = {
                        controller.view === this && !controller.loading && !controller.composing &&
                        controller.fullscreen == null && controller.fileCallback == null && controller.prompt == null && controller.document == null
                    }
                    onRefresh = controller::retry
                    onViewportChanged = { AntintwrkWebIme.update(this, controller, policy) }
                    controller.registerPage(tab, this, startUrl)
                    setBackgroundColor(Color(0xFF101214).toArgb())
                    settings.apply {
                        if (BuildConfig.ANTINTWRK_NATIVE_AUTH) userAgentString = AntintwrkNativeAuthClient.USER_AGENT
                        javaScriptEnabled = true
                        // Honor the responsive page viewport, as a mobile browser does.
                        domStorageEnabled = true
                        // Keep HTTP validation/cache headers authoritative; never serve stale private HTML.
                        cacheMode = WebSettings.LOAD_DEFAULT
                        useWideViewPort = true
                        // Match the site/PWA CSS baseline rather than WebView font inflation.
                        loadWithOverviewMode = true
                        textZoom = 100
                        setSupportZoom(false)
                        builtInZoomControls = false
                        displayZoomControls = false
                        allowFileAccess = false
                        allowContentAccess = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        mediaPlaybackRequiresUserGesture = true
                        setSupportMultipleWindows(true)
                        javaScriptCanOpenWindowsAutomatically = false
                    }
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                    val ownerPage = this
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
                                    if (controller.view !== ownerPage) {
                                        controller.popups.remove(popup)
                                        popup.destroy()
                                        return true
                                    }
                                    if (target != null) {
                                        controller.navigate(target)
                                    } else if (policy.external(url) || policy.outbound(url)) {
                                        controller.onExternalLink(url)
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
                            if (controller.view !== ownerPage) {
                                callback.onCustomViewHidden()
                                return
                            }
                            controller.closeFullscreen()
                            controller.fullscreen = view
                            controller.fullscreenCallback = callback
                        }
                        override fun onHideCustomView() {
                            if (controller.view === ownerPage) controller.closeFullscreen()
                        }
                        override fun onShowFileChooser(
                            webView: WebView,
                            callback: ValueCallback<Array<Uri>>,
                            params: FileChooserParams,
                        ): Boolean {
                            if (controller.view !== webView || policy.embedded(webView.url.orEmpty()) == null) {
                                callback.onReceiveValue(null)
                                return true
                            }
                            controller.fileCallback?.onReceiveValue(null)
                            controller.fileCallback = callback
                            controller.allowMultipleFiles = params.mode == FileChooserParams.MODE_OPEN_MULTIPLE
                            runCatching { requireNotNull(controller.launchFilePicker).invoke(params.createIntent()) }.onFailure {
                                controller.fileCallback = null
                                callback.onReceiveValue(null)
                            }
                            return true
                        }
                    }
                    AntintwrkNativeWebSignals.attach(this, controller, policy) { controller.onAuthRequired() }
                    if (BuildConfig.ANTINTWRK_NATIVE_AUTH &&
                        runCatching { WebViewFeature.isFeatureSupported(WebViewFeature.WEB_AUTHENTICATION) }.getOrDefault(false)) {
                        WebSettingsCompat.setWebAuthenticationSupport(settings, WebSettingsCompat.WEB_AUTHENTICATION_SUPPORT_FOR_APP)
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            if (!request.isForMainFrame) return false
                            val url = policy.canonical(request.url.toString())
                            if (controller.view !== view) {
                                val target = policy.embedded(url)
                                val destination = controller.nativeSection(url)
                                if (!policy.needsAuth(url) && policy.messengerLink(url) == null && target == url &&
                                    (destination == null || destination == controller.pageSection(view))) {
                                        return false
                                    }
                                controller.pageFailed(view)
                                return true
                            }
                            if (controller.interceptMessenger(url)) return true
                            if (BuildConfig.ANTINTWRK_NATIVE_AUTH && policy.needsAuth(url)) {
                                controller.onAuthRequired()
                                return true
                            }
                            val target = policy.embedded(url)
                            if (target != null) {
                                if (target == url &&
                                    (controller.nativeSection(target) == null || controller.nativeSection(target) == controller.section)) {
                                        controller.loading = true
                                        controller.failed = false
                                        return false
                                    }
                                controller.navigate(target)
                            } else if ((policy.external(url) || policy.outbound(url)) &&
                                (request.hasGesture() || policy.outbound(url) || (request.isRedirect && policy.embedded(view.url.orEmpty()) != null))) {
                                controller.onExternalLink(url)
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
                            controller.pageStarted(view, url)
                            if (controller.view === view) {
                                controller.updateEditor(android.net.Uri.parse(url.orEmpty()).path?.trimEnd('/')?.endsWith("/hero-compose") == true)
                                controller.loading = true
                                controller.failed = false
                            }
                        }
                        override fun onPageFinished(view: WebView, url: String?) {
                            controller.pageFinished(view, url)
                            if (url != null && controller.pageSection(view) == AntintwrkSection.Feed) {
                                view.postVisualStateCallback(0, object : WebView.VisualStateCallback() {
                                    override fun onComplete(requestId: Long) = controller.feedReady(view, url)
                                })
                            }
                            if (controller.view === view) {
                                controller.loading = false
                                url?.let { controller.visited(it, policy) }
                                controller.finishMessengerProfileNavigation(view)
                                AntintwrkWebIme.update(view, controller, policy, install = true)
                            }
                        }
                        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                            if (request.isForMainFrame) controller.pageFailed(view)
                        }
                        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                            if (request.isForMainFrame) {
                                controller.pageFailed(view)
                                if (response.statusCode == 401 && controller.view === view) controller.onAuthRequired()
                            }
                        }
                    }
                    setDownloadListener { url, _, _, _, _ ->
                        if (controller.view === this && (policy.external(url) || policy.outbound(url))) controller.onExternalLink(url)
                    }
                    loadUrl(startUrl)
                }).also { view ->
                    (view.parent as? ViewGroup)?.removeView(view)
                    view.onResume()
                }
            },
            update = {
                it.visibility = if (!active || controller.failed) View.INVISIBLE else View.VISIBLE
                it.onResume()
            },
            onRelease = { view ->
                view.post { if (!view.isAttachedToWindow) view.onPause() }
            },
        )
        }
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
            AntintwrkServiceUnavailable(message = R.string.screen_ant_social_unavailable, onRetry = controller::retry)
        }
    }
}
