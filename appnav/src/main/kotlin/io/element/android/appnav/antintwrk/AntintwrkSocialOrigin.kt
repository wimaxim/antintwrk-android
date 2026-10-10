// Copyright 2026 Maxim Novichkov
// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

internal object AntintwrkSocialOrigins {
    const val RUSSIAN = "https://antimsg.com"
    const val PUBLIC = "https://antintwrk.com"
    val allowed = setOf(RUSSIAN, PUBLIC)

    fun recommendation(json: JSONObject): String {
        require(json.get("version") == 1 && json.get("detected") is Boolean)
        val detected = json.getBoolean("detected")
        val country = if (json.isNull("country")) null else json.getString("country")
        require(if (detected) country != null && country.matches(Regex("[A-Z]{2}")) && country != "ZZ" else country == null)
        val expected = if (!detected || country == "RU") RUSSIAN else PUBLIC
        require(json.getString("social_origin") == expected)
        return expected
    }
}

/** No cookies, device/profile country, GPS, redirects or caller-provided IP. */
internal class AntintwrkSocialOriginCoordinator(
    context: Context,
    private val lookup: (suspend () -> String)? = null,
    private val transfer: suspend (AntintwrkNativeAuthClient, String, AntintwrkAuthSession) -> AntintwrkAuthSession =
        { client, target, admission -> client.transferSocialSession(target, admission) },
) {
    private val preferences = context.getSharedPreferences("antintwrk-social-routing", Context.MODE_PRIVATE)
    private val lock = Mutex()
    private var initialChoice: String? = null
    private var chosenAt = 0L
    private val http = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
        .callTimeout(2500, TimeUnit.MILLISECONDS).build()

    suspend fun preload() = lock.withLock {
        initialChoice = recommend()
        chosenAt = android.os.SystemClock.elapsedRealtime()
    }

    private suspend fun recommend(): String = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("${AntintwrkSocialOrigins.RUSSIAN}/app/geo")
                .header("User-Agent", AntintwrkNativeAuthClient.USER_AGENT).header("Cache-Control", "no-cache").get().build()
            http.newCall(request).execute().use { response ->
                require(response.isSuccessful)
                val source = response.body.source()
                source.request(4097)
                require(source.buffer.size <= 4096)
                AntintwrkSocialOrigins.recommendation(JSONObject(source.buffer.readUtf8()))
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            preferences.getString("working-origin", null)?.takeIf { it in AntintwrkSocialOrigins.allowed } ?: AntintwrkSocialOrigins.RUSSIAN
        }
    }

    suspend fun prepare(client: AntintwrkNativeAuthClient, admission: AntintwrkAuthSession, web: AntintwrkWebFeedController) = lock.withLock {
        require(admission.stage == AntintwrkAuthStage.AUTHENTICATED && admission.matrixUserId != null)
        val epoch = AntintwrkSocialCookieEpoch.current()
        // Pin for the retained surface: resume/network changes never reload pages or discard drafts.
        val target = if (web.routingSelected) {
            web.origin
        } else {
            lookup?.invoke() ?: initialChoice?.takeIf { android.os.SystemClock.elapsedRealtime() - chosenAt < 180_000 } ?: recommend()
        }
        try {
            require(target in AntintwrkSocialOrigins.allowed)
            val routed = if (target == client.origin) admission else transfer(client, target, admission)
            synchronized(AntintwrkSocialCookieEpoch.monitor) { AntintwrkSocialCookieEpoch.check(epoch) }
            require(routed.stage == AntintwrkAuthStage.AUTHENTICATED && routed.matrixUserId == admission.matrixUserId)
            web.origin = target
            web.routedSession = routed
            preferences.edit().putString("working-origin", target).apply()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            synchronized(AntintwrkSocialCookieEpoch.monitor) { AntintwrkSocialCookieEpoch.check(epoch) }
            // A failed optional initial handoff retains the already authenticated Russian entry.
            // Never switch the origin underneath an existing document.
            if (web.currentUrl != null) return@withLock
            web.origin = client.origin
            web.routedSession = admission
            preferences.edit().putString("working-origin", client.origin).apply()
        }
        web.routingSelected = true
    }
}
