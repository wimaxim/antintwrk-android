// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import android.webkit.WebView

internal object AntintwrkWebOverlays {
    private var script: String? = null
    fun install(page: WebView, policy: AntintwrkWebPolicy) {
        if (policy.embedded(page.url.orEmpty()) == null) return
        val source = script ?: page.context.assets.open("antintwrk-overlays.js").bufferedReader().use { it.readText() }.also { script = it }
        page.evaluateJavascript(source, null)
    }
}
