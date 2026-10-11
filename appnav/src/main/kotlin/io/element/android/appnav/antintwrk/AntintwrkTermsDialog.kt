// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
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
    val language = LocalConfiguration.current.locales[0].language.takeIf { it in AntintwrkAppLocale.languages } ?: "en"
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
        AntintwrkBrandTheme {
            Column(Modifier.fillMaxSize().background(AntintwrkBackground).safeDrawingPadding()) {
                TextButton(stringResource(R.string.screen_antintwrk_back), onDismiss)
                Box(Modifier.weight(1f)) {
                    AndroidView(modifier = Modifier.fillMaxSize(), factory = {
                        view.apply {
                            settings.javaScriptEnabled = false
                            settings.textZoom = 100
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.setSupportZoom(false)
                            settings.builtInZoomControls = false
                            settings.displayZoomControls = false
                            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
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
                                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                    val url = request.url.toString()
                                    val legal = policy.legalReader(url) ?: return true
                                    if (legal != url) {
                                        view.loadUrl(legal)
                                        return true
                                    }
                                    return false
                                }
                            }
                            loadUrl(requireNotNull(policy.legalReader(policy.route(language, "terms"))))
                        }
                    })
                    if (loading) AntintwrkLoadingView(Modifier.fillMaxSize())
                    if (failed) {
                        Column(Modifier.align(Alignment.Center)) {
                            Text(stringResource(R.string.screen_ant_auth_request_error))
                            TextButton(stringResource(R.string.screen_ant_web_retry), { view.reload() })
                        }
                    }
                }
            }
        }
    }
}
