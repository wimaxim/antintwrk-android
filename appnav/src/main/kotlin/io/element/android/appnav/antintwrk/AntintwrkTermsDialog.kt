// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.element.android.appnav.BuildConfig
import io.element.android.appnav.R
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton

@Composable
internal fun AntintwrkTermsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val language = LocalConfiguration.current.locales[0].language.takeIf { it in setOf("en", "ru", "uk", "de", "es", "fr", "it", "ar", "zh") } ?: "en"
    val policy = remember { AntintwrkWebPolicy(BuildConfig.ANTINTWRK_SOCIAL_ORIGIN) }
    val view = remember { WebView(context) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    DisposableEffect(view) {
        onDispose {
        view.stopLoading()
        view.destroy()
    }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize()) {
            TextButton(stringResource(R.string.screen_antintwrk_back), onDismiss)
            Box(Modifier.weight(1f)) {
            AndroidView(modifier = Modifier.fillMaxSize(), factory = {
                view.apply {
                    settings.javaScriptEnabled = false
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.userAgentString = AntintwrkNativeAuthClient.USER_AGENT
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                            loading = true
                            failed = false
                        }
                        override fun onPageFinished(view: WebView, url: String) {
                            loading = false
                        }
                        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                            if (request.isForMainFrame) {
                                failed = true
                                loading = false
                            }
                        }
                        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                            if (request.isForMainFrame) {
                                failed = true
                                loading = false
                            }
                        }
                        override fun shouldOverrideUrlLoading(view: WebView, request: android.webkit.WebResourceRequest): Boolean {
                            val url = request.url.toString()
                            val uri = Uri.parse(url)
                            val parts = uri.pathSegments
                            if (!policy.sameOrigin(url) || parts.lastOrNull() !in setOf("terms", "useragreement", "privacy", "privacypolicy")) return true
                            if (uri.getQueryParameter("native_legal") != "1") {
                                view.loadUrl(uri.buildUpon().appendQueryParameter("native_legal", "1").build().toString())
                                return true
                            }
                            return false
                        }
                    }
                    loadUrl("${BuildConfig.ANTINTWRK_SOCIAL_ORIGIN}/$language/terms?native_legal=1")
                }
            })
                if (loading) AntintwrkLoadingView(Modifier.fillMaxSize())
                if (failed) {
                    Column(Modifier.align(Alignment.Center)) {
                    Text(stringResource(R.string.screen_ant_auth_session_check_error))
                    TextButton(stringResource(R.string.screen_ant_web_retry), { view.reload() })
                }
                }
            }
        }
    }
}
