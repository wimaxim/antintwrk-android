// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import io.element.android.libraries.matrix.api.auth.MatrixAuthenticationService
import io.element.android.libraries.matrix.api.auth.OAuthRedirectUrlProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

@ContributesTo(AppScope::class)
interface AntintwrkMessengerBindings {
    fun matrixAuthenticationService(): MatrixAuthenticationService
    fun oauthRedirectUrlProvider(): OAuthRedirectUrlProvider
    fun sessionStore(): io.element.android.libraries.sessionstorage.api.SessionStore
}

/** OAuth has its own allowed routes; never runs inside a social content WebView. */
@Composable
internal fun AntintwrkMessengerOAuth(
    url: String,
    expectedUserId: String,
    csrf: String,
    client: AntintwrkNativeAuthClient,
    callbackBase: String,
    onComplete: (String) -> Unit,
    onCancel: () -> Unit,
    onError: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val http = remember { OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).callTimeout(20, TimeUnit.SECONDS).build() }
    val view = remember(url) { WebView(context) }
    DisposableEffect(view) {
        onDispose {
        view.stopLoading()
        view.destroy()
    }
    }
    androidx.compose.runtime.key(url, expectedUserId) {
            AndroidView(modifier = Modifier.size(1.dp).alpha(0f).clearAndSetSemantics {}, factory = {
                view.apply {
                    settings.javaScriptEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.userAgentString = AntintwrkNativeAuthClient.USER_AGENT
                    webViewClient = object : WebViewClient() {
                        private var processing = false
                        private fun navigate(target: String): Boolean {
                            val uri = Uri.parse(target)
                            if (uri.buildUpon().clearQuery().fragment(null).build().toString() == callbackBase) {
                                if (!processing) {
                                    processing = true
                                    onComplete(target)
                                }
                                return true
                            }
                            if (uri.scheme != "https" || uri.host != "auth.antimsg.com" || uri.userInfo != null || uri.port !in setOf(-1, 443)) {
                                onError()
                                return true
                            }
                            if (uri.path == "/social-identity/authorize") {
                                if (processing) return true
                                processing = true
                                scope.launch {
                                    try {
                                        val query = uri.encodedQuery ?: error("Missing authorization request")
                                        val handoff = client.messengerHandoff(query, csrf)
                                        require(handoff.getString("matrixUserId") == expectedUserId)
                                        val next = withContext(Dispatchers.IO) {
                                            val body = JSONObject().put("query", query).put("assertion", handoff.getString("assertion")).toString()
                                            val request = Request.Builder().url("https://auth.antimsg.com/social-identity/authorize")
                                                .header("User-Agent", AntintwrkNativeAuthClient.USER_AGENT)
                                                .post(body.toRequestBody("application/json".toMediaType())).build()
                                            http.newCall(request).execute().use { response ->
                                                require(response.code == 302)
                                                response.header("Location") ?: error("Missing callback")
                                            }
                                        }
                                        val callback = Uri.parse(next)
                                        require(
                                            callback.scheme == "https" && callback.host == "auth.antimsg.com" && callback.userInfo == null &&
                                            callback.port in setOf(-1, 443) &&
                                            callback.path?.startsWith("/upstream/callback/") == true
                                        )
                                        processing = false
                                        view.loadUrl(next)
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (_: Exception) {
                                        onError()
                                    }
                                }
                                return true
                            }
                            return false
                        }
                        override fun onPageFinished(view: WebView, url: String) {
                            val uri = Uri.parse(url)
                            if (uri.scheme == "https" && uri.host == "auth.antimsg.com" &&
                                uri.path?.matches(Regex("/consent/[0-9A-HJKMNP-TV-Z]{26}")) == true) {
                                // MAS v1.26: first form continues this app's own pending PKCE grant.
                                // Preserve its server-provided CSRF field; never submit arbitrary forms.
                                view.evaluateJavascript("""(() => {
                                    const form = document.querySelector('form.cpd-form-root[method="POST"]');
                                    if (form && form.querySelector('input[name="csrf"]') && form.querySelector('button[type="submit"]')) form.requestSubmit();
                                })()""".trimIndent(), null)
                            }
                        }
                        override fun shouldOverrideUrlLoading(view: WebView, request: android.webkit.WebResourceRequest): Boolean =
                            navigate(request.url.toString())
                        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean = navigate(url)
                    }
                    loadUrl(url)
                }
            })
    }
}

/** Expire only MAS cookies. Social cookies and every actor's encrypted store stay intact. */
internal suspend fun clearAntintwrkOAuthCookies() {
    val manager = android.webkit.CookieManager.getInstance()
    val origin = "https://auth.antimsg.com"
    val names = manager.getCookie(origin).orEmpty().split(';').map { it.substringBefore('=').trim() }.filter { it.isNotEmpty() }
    for (name in names) {
        for (domain in listOf("", "; Domain=auth.antimsg.com")) {
            kotlinx.coroutines.suspendCancellableCoroutine<Unit> { continuation ->
                manager.setCookie(origin, "$name=; Path=/; Max-Age=0; Secure; HttpOnly$domain") {
                    if (continuation.isActive) continuation.resumeWith(Result.success(Unit))
                }
            }
        }
    }
    manager.flush()
}
