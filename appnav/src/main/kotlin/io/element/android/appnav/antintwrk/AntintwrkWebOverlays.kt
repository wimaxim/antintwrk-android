// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

internal object AntintwrkWebOverlays {
    private var script: String? = null
    private var presentation: String? = null

    fun prepare(page: WebView, policy: AntintwrkWebPolicy) {
        if (runCatching { WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT) }.getOrDefault(false)) {
            WebViewCompat.addDocumentStartJavaScript(page, presentation(page), setOf(policy.origin))
        }
    }

    fun install(page: WebView, policy: AntintwrkWebPolicy) {
        if (!policy.sameOrigin(page.url.orEmpty()) || policy.embedded(page.url.orEmpty()) == null) return
        val source = script ?: page.context.assets.open("antintwrk-overlays.js").bufferedReader().use { it.readText() }.also { script = it }
        page.evaluateJavascript("${presentation(page)}\n$source", null)
    }

    private fun presentation(page: WebView): String = presentation
        ?: page.context.assets.open("antintwrk-presentation.js").bufferedReader().use { it.readText() }.also { presentation = it }
}
