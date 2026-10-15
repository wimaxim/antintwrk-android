/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import android.webkit.WebView
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject

/** Exact-origin, main-frame events contain no cookies, credentials or account authority. */
internal object AntintwrkNativeWebSignals {
    fun attach(view: WebView, controller: AntintwrkWebFeedController, policy: AntintwrkWebPolicy, onAuthRequired: () -> Unit) {
        if (!runCatching { WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) }.getOrDefault(false)) return
        WebViewCompat.addWebMessageListener(view, "antintwrkNative", setOf(policy.origin)) { _, message, origin, main, reply ->
            if (controller.view === view &&
                message.type == WebMessageCompat.TYPE_STRING) {
                    accept(controller, policy, view.url.orEmpty(), origin.toString(), main, message.data, onAuthRequired) { json ->
                    if (json.optString("type").startsWith("store-")) {
                        val pageUrl = view.url
                        val callback = controller.onStoreRequest
                        if (callback == null) {
                            reply.postMessage(JSONObject().put("id", json.optString("id")).put("ok", false).toString())
                        } else {
                            callback(json) { result ->
                                if (controller.view === view && view.url == pageUrl) {
                                    runCatching { reply.postMessage(result.put("id", json.optString("id")).toString()) }
                                }
                            }
                        }
                        return@accept
                    }
                    AntintwrkNativeWebActions.handle(view.context, controller, json) { ok ->
                        runCatching { reply.postMessage(JSONObject().put("id", json.optString("id")).put("ok", ok).toString()) }
                    }
                }
                }
        }
    }
    fun accept(
        controller: AntintwrkWebFeedController,
        policy: AntintwrkWebPolicy,
        currentUrl: String,
        origin: String,
        main: Boolean,
        message: String?,
        onAuthRequired: () -> Unit,
        onRequest: (JSONObject) -> Unit = {},
    ) {
        if (!main || !policy.sameOrigin(origin) || policy.embedded(currentUrl) == null || message == null || message.length > 200_000) return
        runCatching {
            val json = JSONObject(message)
            if (json.optString("type") != "download" && message.length > 2048) return
            when (json.optString("type")) {
                "navigation" -> {
                    val url = json.optString("url")
                    if (policy.embedded(url) != null && android.net.Uri.parse(url) == android.net.Uri.parse(currentUrl)) controller.visited(url, policy)
                }
                "share", "copy", "download", "open-external" -> if (json.optString("id").matches(Regex("[A-Za-z0-9-]{1,64}"))) onRequest(json)
                "store-catalog", "store-purchase", "store-restore" -> {
                    // Gratitude checkout is mounted inside feed/profile/publication pages too.
                    // The outer guard keeps the exact origin, main frame and embedded-route policy;
                    // the actor-bound coordinator and server still authorize every store operation.
                    if (json.optString("id").matches(Regex("[A-Za-z0-9-]{1,64}"))) onRequest(json)
                }
                "editor" -> if (json.opt("active") is Boolean) controller.updateEditor(json.getBoolean("active"), json.opt("dialog") == true)
                "draft" -> controller.markDraft()
                "changed" -> controller.refreshVersion++
                "auth-required" -> onAuthRequired()
            }
        }
    }
}
