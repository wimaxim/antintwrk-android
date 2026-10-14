// Copyright 2026 Maxim Novichkov
// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import io.element.android.appnav.BuildConfig
import io.element.android.libraries.network.antintwrk.AntintwrkRegionalTransport
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.Executor

internal val antintwrkWebTransportReady = MutableStateFlow(true)

/** Apply only to our app's canonical HTTPS hosts, leaving other WebView destinations direct. */
@SuppressLint("RequiresFeature")
fun initializeAntintwrkTransport(context: Context) {
    if (!BuildConfig.ANTINTWRK_NATIVE_AUTH) return
    AntintwrkRegionalTransport.initialize(context.applicationContext)
    if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE) ||
        !WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE_REVERSE_BYPASS)) {
            return
        }
    val proxy = AntintwrkRegionalTransport.proxyUrl ?: return
    antintwrkWebTransportReady.value = false
    val config = ProxyConfig.Builder().addProxyRule(proxy, ProxyConfig.MATCH_HTTPS).apply {
        AntintwrkRegionalTransport.hosts.forEach { addBypassRule(it) }
        setReverseBypassEnabled(true)
    }.build()
    val handler = Handler(Looper.getMainLooper())
    val executor = Executor { runnable -> handler.post(runnable) }
    try {
        ProxyController.getInstance().setProxyOverride(config, executor) { antintwrkWebTransportReady.value = true }
    } catch (_: UnsupportedOperationException) {
        antintwrkWebTransportReady.value = true
    }
}
