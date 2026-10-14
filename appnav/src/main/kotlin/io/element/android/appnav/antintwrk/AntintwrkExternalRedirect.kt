// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import android.webkit.CookieManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Resolve our /go,/goto within the authenticated app; browser receives only the external destination. */
internal suspend fun antintwrkExternalRedirect(url: String, policy: AntintwrkWebPolicy): String = withContext(Dispatchers.IO) {
    val http = io.element.android.libraries.network.antintwrk.AntintwrkRegionalTransport.configure(
        OkHttpClient.Builder()
    ).followRedirects(false).followSslRedirects(false).callTimeout(10, TimeUnit.SECONDS).build()
    var current = policy.canonical(url)
    repeat(5) {
        if (!policy.sameOrigin(current)) {
            require(policy.external(current))
            return@withContext current
        }
        require(policy.outbound(current))
        val request = Request.Builder().url(current)
            .header("User-Agent", AntintwrkNativeAuthClient.USER_AGENT)
            .header("Cookie", CookieManager.getInstance().getCookie(current).orEmpty()).get().build()
        current = http.newCall(request).execute().use { response ->
            if (response.code in setOf(408, 500, 502, 503, 504)) throw AntintwrkRequestError("UNAVAILABLE", response.code)
            require(response.code in setOf(301, 302, 303, 307, 308))
            val target = response.header("Location") ?: error("Missing destination")
            response.request.url.resolve(target)?.toString() ?: error("Invalid destination")
        }
    }
    error("Too many outbound redirects")
}
