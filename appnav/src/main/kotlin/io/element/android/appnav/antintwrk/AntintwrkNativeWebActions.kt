/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONObject

internal data class AntintwrkWebDocument(val content: String, val complete: (Boolean) -> Unit)

/** Called only after exact-origin/main-frame validation; never reads session or clipboard data. */
internal object AntintwrkNativeWebActions {
    private fun publicSocialLink(url: String): String {
        val uri = Uri.parse(url)
        if (uri.scheme != "https" || uri.host != "antimsg.com" || uri.userInfo != null) return url
        require(AntintwrkWebPolicy("https://antimsg.com").embedded(url) != null)
        return uri.buildUpon().authority("antintwrk.com").clearQuery().apply {
            uri.queryParameterNames.filter {
                it !in
                setOf("app", "native_legal", "token", "csrf", "ticket", "code", "access_token", "id_token", "refresh_token", "state", "session")
            }.forEach { name ->
                uri.getQueryParameters(name).forEach { appendQueryParameter(name, it) }
            }
        }.build().toString()
    }
    fun handle(context: Context, controller: AntintwrkWebFeedController, json: JSONObject, complete: (Boolean) -> Unit) {
        runCatching {
            when (json.optString("type")) {
                "copy" -> {
                    val original = json.getString("text")
                    val text = if (original.startsWith("https://antimsg.com/")) publicSocialLink(original) else original
                    require(text.length <= 1800)
                    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("antintwrk", text))
                    complete(true)
                }
                "share" -> {
                    val uri = Uri.parse(json.getString("url"))
                    require(uri.scheme in setOf("https", "http") && uri.userInfo == null)
                    val url = Uri.parse(publicSocialLink(uri.toString())).buildUpon().clearQuery().apply {
                        uri.queryParameterNames.filter {
                            it !in
                            setOf("app", "native_legal", "token", "csrf", "ticket", "code", "access_token", "id_token", "refresh_token", "state", "session")
                        }.forEach { name ->
                            uri.getQueryParameters(name).forEach { appendQueryParameter(name, it) }
                        }
                    }.build().toString()
                    val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url)
                        .putExtra(Intent.EXTRA_SUBJECT, json.optString("title").take(200))
                    context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    complete(true)
                }
                "download" -> {
                    require(controller.document == null)
                    val content = json.getString("content")
                    require(content.length <= 180_000)
                    val filename = json.optString("filename").take(100).replace(Regex("[^A-Za-z0-9._-]"), "_")
                    require(filename.endsWith(".html"))
                    controller.document = AntintwrkWebDocument(content, complete)
                    requireNotNull(controller.launchDocumentSave).invoke(filename)
                }
                else -> complete(false)
            }
        }.onFailure {
            if (controller.document?.complete === complete) controller.document = null
            complete(false)
        }
    }
}
