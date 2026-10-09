// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import android.webkit.WebView

/** Only the active, exact-origin social document may receive keyboard layout corrections. */
internal object AntintwrkWebIme {
    private var script: String? = null
    fun update(page: WebView, controller: AntintwrkWebFeedController, policy: AntintwrkWebPolicy, install: Boolean = false) {
        if (controller.view !== page || policy.embedded(page.url.orEmpty()) == null) return
        val bootstrap = if (install) {
            script ?: page.context.assets.open("antintwrk-ime.js").bufferedReader().use { it.readText() }.also { script = it }
        } else {
            ""
        }
        page.evaluateJavascript("$bootstrap\nwindow.antintwrkIme?.update(${controller.imeVisible});", null)
    }
}
