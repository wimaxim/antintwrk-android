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
    private val clock: () -> Long = android.os.SystemClock::elapsedRealtime,
    private val read: suspend (AntintwrkNativeAuthClient, AntintwrkAuthSession, String) -> AntintwrkAuthSession = { client, session, target ->
        antintwrkRestoreSocialAdmission(session.matrixUserId, primary = { client.routingSession() }, fallback = {
            AntintwrkNativeAuthClient(target).routingSession()
        }, acceptedFallback = {})
    },
) {
    private val preferences = context.getSharedPreferences("antintwrk-social-routing", Context.MODE_PRIVATE)
    fun recordApplied(origin: String) {
        require(origin in AntintwrkSocialOrigins.allowed)
        preferences.edit().putString("working-origin", origin).apply()
    }
    private val lock = Mutex()
    private var initialChoice: String? = null
    private var chosenAt = 0L
    private var checkedAt: Long? = null
    private var outageAt: Long? = null
    fun nextCheckDelay(): Long = checkedAt?.let { (300_000 - (clock() - it)).coerceIn(30_000, 300_000) } ?: 300_000
    private val http = io.element.android.libraries.network.antintwrk.AntintwrkRegionalTransport.configure(
        OkHttpClient.Builder()
    ).followRedirects(false).followSslRedirects(false)
        .callTimeout(4500, TimeUnit.MILLISECONDS).build()

    suspend fun preload(force: Boolean = true) = lock.withLock {
        if (!force && checkedAt?.let { clock() - it < 300_000 } == true) return@withLock
        initialChoice = recommend()
        chosenAt = clock()
        checkedAt = chosenAt
    }

    private suspend fun recommend(): String = withContext(Dispatchers.IO) {
        try {
            lookup?.let { return@withContext it() }
            // A surviving old TCP connection could still use the previous VPN/NAT egress.
            http.connectionPool.evictAll()
            val request = Request.Builder().url("${AntintwrkSocialOrigins.RUSSIAN}/app/geo")
                .header("User-Agent", AntintwrkNativeAuthClient.USER_AGENT).header("Cache-Control", "no-cache").get().build()
            http.newCall(request).execute().use { response ->
                require(response.isSuccessful)
                val source = response.body.source()
                source.request(4097)
                require(source.buffer.size <= 4096)
                AntintwrkSocialOrigins.recommendation(JSONObject(source.buffer.readUtf8())).also {
                    io.element.android.libraries.network.antintwrk.AntintwrkRegionalTransport.recordSocialRecommendation(it)
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            preferences.getString("working-origin", null)?.takeIf { it in AntintwrkSocialOrigins.allowed } ?: AntintwrkSocialOrigins.RUSSIAN
        }
    }

    /** Prepare only; a successful lookup never mutates an open document. */
    suspend fun refresh(
        client: AntintwrkNativeAuthClient,
        admission: AntintwrkAuthSession,
        web: AntintwrkWebFeedController,
        force: Boolean = false,
        unavailable: Boolean = false,
    ) = lock.withLock {
        if (!web.routingSelected || admission.stage != AntintwrkAuthStage.AUTHENTICATED) return@withLock
        val now = clock()
        if (unavailable) {
            if (outageAt?.let { now - it < 30_000 } == true) return@withLock
            outageAt = now
        }
        if (!force && checkedAt?.let { now - it < 300_000 } == true) return@withLock
        val epoch = AntintwrkSocialCookieEpoch.current()
        web.pendingRouting = null
        val target = recommend().let {
            if (unavailable && it == web.origin) AntintwrkSocialOrigins.allowed.single { host -> host != web.origin } else it
        }
        checkedAt = clock()
        initialChoice = target
        chosenAt = checkedAt!!
        if (target == web.origin) return@withLock
        try {
            require(target in AntintwrkSocialOrigins.allowed)
            val source = client.routingClient()
            val fresh = read(source, admission, target)
            require(fresh.stage == AntintwrkAuthStage.AUTHENTICATED && fresh.matrixUserId == admission.matrixUserId)
            val routed = if (target == source.origin) fresh else transfer(source, target, fresh)
            AntintwrkSocialCookieEpoch.check(epoch)
            require(routed.stage == AntintwrkAuthStage.AUTHENTICATED && routed.matrixUserId == admission.matrixUserId)
            web.pendingRouting = AntintwrkPreparedSocialRouting(target, routed, epoch, clock())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Neither a challenge nor an optional handoff failure changes the existing admission.
        }
    }

    suspend fun prepare(client: AntintwrkNativeAuthClient, admission: AntintwrkAuthSession, web: AntintwrkWebFeedController) = lock.withLock {
        require(admission.stage == AntintwrkAuthStage.AUTHENTICATED && admission.matrixUserId != null)
        val epoch = AntintwrkSocialCookieEpoch.current()
        // Pin for the retained surface: resume/network changes never reload pages or discard drafts.
        val target = if (web.routingSelected) {
            web.origin
        } else {
            initialChoice?.takeIf { clock() - chosenAt < 180_000 } ?: recommend()
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
