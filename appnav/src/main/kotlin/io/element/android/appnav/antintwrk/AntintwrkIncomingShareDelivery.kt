// Copyright 2026 Maxim Novichkov
// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import org.json.JSONArray
import org.json.JSONObject

internal data class AntintwrkShareResourceLease(val page: WebView, val request: AntintwrkIncomingShare, val origin: String, val expires: Long)

internal fun AntintwrkWebFeedController.clearIncomingDelivery() {
    incomingResourceLease = null
    incomingDelivery?.directory?.deleteRecursively()
    incomingDelivery = null
    incomingImportBusy = false
    incomingEvaluationBusy = false
}

/** Streams staged media to Chromium, without base64, HTTP traffic, cookies or a general-purpose file bridge. */
internal fun AntintwrkWebFeedController.incomingResource(page: WebView, resource: WebResourceRequest): WebResourceResponse? {
    if (resource.url.path?.startsWith("/_native-share/") != true ||
        AntintwrkSocialOrigins.allowed.none { AntintwrkWebPolicy(it).sameOrigin(resource.url.toString()) }) {
            return null
        }
    val denied = { WebResourceResponse("text/plain", "UTF-8", 403, "Forbidden", mapOf("Cache-Control" to "no-store"), "".byteInputStream()) }
    val lease = incomingResourceLease ?: return denied()
    val prefix = "${lease.origin}/_native-share/${lease.request.id}/"
    if (!resource.url.toString().startsWith(prefix)) return denied()
    if (lease.page !== page || resource.isForMainFrame || resource.method != "GET" || lease.request.actor != incomingActor ||
        android.os.SystemClock.elapsedRealtime() > lease.expires) {
            return denied()
        }
    val index = resource.url.toString().removePrefix(prefix).toIntOrNull() ?: return denied()
    val file = lease.request.files.getOrNull(index) ?: return denied()
    return runCatching {
        WebResourceResponse(
            file.mime,
            null,
            200,
            "OK",
            mapOf("Cache-Control" to "no-store", "X-Content-Type-Options" to "nosniff"),
            requireNotNull(page.context.contentResolver.openInputStream(file.uri))
        )
    }.getOrElse { denied() }
}

internal fun AntintwrkWebFeedController.deliverIncomingShare(page: WebView) {
    val request = incomingDelivery ?: return
    if (!incomingImportBusy || incomingEvaluationBusy || view !== page || loading || failed || incomingActor == null || incomingActor != request.actor ||
        routedSession?.matrixUserId != request.actor || !AntintwrkWebPolicy(origin).sameOrigin(page.url.orEmpty()) ||
        Uri.parse(page.url).pathSegments.getOrNull(1) != "hero-compose") {
            return
        }
    val target = page.url
    val lease = AntintwrkShareResourceLease(page, request, origin, android.os.SystemClock.elapsedRealtime() + 120_000)
    incomingResourceLease = lease
    val files = JSONArray()
    request.files.forEachIndexed { index, file ->
        files.put(
            JSONObject().put("url", "$origin/_native-share/${request.id}/$index").put("mime", file.mime)
            .put("name", "$index.${antintwrkSharedExtension(file.mime)}")
        )
    }
    val payload = JSONObject().put("id", request.id).put("text", request.text).put("files", files)
    val adapter = page.context.assets.open("antintwrk-incoming-share.js").bufferedReader().use { it.readText() }
    incomingEvaluationBusy = true
    page.evaluateJavascript("$adapter\nwindow.antintwrkImportNativeShare($payload);") {}
    fun poll() {
        if (!hasPage(page) || page.url != target || incomingDelivery?.id != request.id || incomingActor != request.actor) {
            if (incomingDelivery?.id == request.id) clearIncomingDelivery()
            return
        }
        page.evaluateJavascript("JSON.stringify(window.antintwrkNativeShareStatus || null)") { raw ->
            if (!hasPage(page) || page.url != target || incomingDelivery?.id != request.id || incomingActor != request.actor) return@evaluateJavascript
            val status = runCatching { JSONObject(JSONArray("[$raw]").getString(0)) }.getOrNull()
            if (status?.optString("id") == request.id || android.os.SystemClock.elapsedRealtime() > lease.expires) {
                incomingResourceLease = null
                incomingImportBusy = false
                incomingEvaluationBusy = false
                if (status?.optString("result") != "ok") {
                    incomingDelivery = null
                    AntintwrkIncomingShares.failedDelivery(request, status?.optString("result") == "draft")
                }
            } else {
                page.postDelayed({ poll() }, 200)
            }
        }
    }
    page.postDelayed({ poll() }, 100)
}
