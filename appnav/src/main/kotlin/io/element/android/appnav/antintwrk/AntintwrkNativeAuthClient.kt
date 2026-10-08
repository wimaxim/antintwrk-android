/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import android.webkit.CookieManager
import io.element.android.appnav.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

internal enum class AntintwrkAuthStage { LOGIN, DEVICE, TOTP, SETUP_REQUIRED, AUTHENTICATED }
internal data class AntintwrkSocialStatus(val notifications: Int, val profileIdentity: String?)
internal class AntintwrkRegistrationRequired(val ticket: String, val email: String, val firstName: String, val surname: String) : Exception()
internal class AntintwrkRequestError(val code: String) : Exception()
internal class AntintwrkAccountError(val messageForUser: String) : Exception()
internal data class AntintwrkAuthSession(val stage: AntintwrkAuthStage, val csrf: String, val profileIdentity: String? = null, val matrixUserId: String? = null)

/** No logging interceptor, password persistence, browser redirects or bearer tokens. */
internal class AntintwrkNativeAuthClient(
    private val origin: String = BuildConfig.ANTINTWRK_SOCIAL_ORIGIN,
    private val http: OkHttpClient = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
        .callTimeout(30, TimeUnit.SECONDS).build(),
) {
    companion object {
        const val USER_AGENT = "ANTINTWRK Android native-client/1.0 (+https://qwertynetworks.com)"
    }

    private suspend fun post(action: String, fields: Map<String, String> = emptyMap(), cookieSnapshot: String? = null): String = withContext(Dispatchers.IO) {
        val cookies = CookieManager.getInstance()
        val body = FormBody.Builder().add("action", action).apply { fields.forEach { (key, value) -> add(key, value) } }.build()
        val request = Request.Builder().url("$origin/antintwrk_mobile.php")
            .header("Origin", origin).header("X-Antintwrk-Client", "native").header("User-Agent", USER_AGENT)
            .header("Cookie", cookieSnapshot ?: cookies.getCookie(origin).orEmpty()).post(body).build()
        http.newCall(request).execute().use { response ->
            if (cookieSnapshot == null) {
                response.headers.values("Set-Cookie").forEach { cookies.setCookie(origin, it) }
                cookies.flush()
            }
            // The engine completes these actions with a local redirect. Never follow it;
            // callers must still fetch the cookie-backed admission stage.
            if (response.code == 302 && response.header("Location") == "/" &&
                action in setOf("login", "passkey", "device-verify")) {
                    return@withContext ""
                }
            require(response.body.contentLength() <= 65536) { "Response size" }
            val source = response.body.source()
            source.request(65537)
            require(source.buffer.size <= 65536) { "Request failed" }
            val result = source.buffer.readUtf8()
            if (!response.isSuccessful) throw AntintwrkRequestError(runCatching { JSONObject(result).optString("error") }.getOrDefault(""))
            result
        }
    }

    suspend fun messengerHandoff(query: String, csrf: String): JSONObject =
        JSONObject(post("messenger-handoff", mapOf("query" to query, "csrf" to csrf)))

    suspend fun session(): AntintwrkAuthSession {
        val json = JSONObject(post("session"))
        val csrf = json.getString("csrf")
        require(csrf.matches(Regex("[a-f0-9]{64}")))
        return AntintwrkAuthSession(
            AntintwrkAuthStage.valueOf(json.getString("stage")),
            csrf,
            json.optString("profileIdentity").takeIf { it.matches(Regex("[A-Za-z0-9._-]{1,100}")) },
            json.optString("matrixUserId").takeIf { it.matches(Regex("@u[1-9][0-9]*:matrix\\.antimsg\\.com")) }
        )
    }

    suspend fun login(email: String, password: String, language: String, csrf: String): AntintwrkAuthSession {
        val result = post("login", mapOf("email" to email.trim(), "password" to password, "lng" to language, "csrf" to csrf)).trim()
        if (result.isNotEmpty() && result != "OK") throw accountError(result)
        return session().also { require(it.stage != AntintwrkAuthStage.LOGIN) { "Sign in failed" } }
    }

    suspend fun providerOptions(provider: String, csrf: String): JSONObject = JSONObject(post("$provider-options", mapOf("csrf" to csrf)))
    suspend fun providerSignIn(provider: String, credential: String, ticket: String, csrf: String): AntintwrkAuthSession {
        val result = post(provider, mapOf("credential" to credential, "ticket" to ticket, "csrf" to csrf)).trim()
        if (provider == "passkey") {
            require(result.isEmpty() || result == "OK")
        } else {
            val json = JSONObject(result)
            if (json.has("registrationTicket")) {
                throw AntintwrkRegistrationRequired(
                json.getString("registrationTicket"),
                    json.getString("email"),
                    json.optString("firstName"),
                    json.optString("surname"),
            )
            }
            require(json.optBoolean("ok"))
        }
        return session().also { require(it.stage != AntintwrkAuthStage.LOGIN) }
    }

    suspend fun requestAccount(action: String, fields: Map<String, String>, csrf: String) {
        require(action in setOf("register", "reset"))
        val result = post(action, fields + ("csrf" to csrf)).trim()
        if (result != "OK") {
            throw accountError(result)
        }
    }

    private fun accountError(result: String) = AntintwrkAccountError(
        android.text.Html.fromHtml(result, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim().take(300)
    )

    suspend fun registerGoogle(fields: Map<String, String>, ticket: String, csrf: String): AntintwrkAuthSession {
        require(JSONObject(post("google-register", fields + mapOf("ticket" to ticket, "csrf" to csrf))).optBoolean("ok"))
        return session().also { require(it.stage != AntintwrkAuthStage.LOGIN) }
    }

    suspend fun status(csrf: String): AntintwrkSocialStatus {
        val json = JSONObject(post("status", mapOf("csrf" to csrf)))
        return AntintwrkSocialStatus(
            json.getInt("notifications").coerceIn(0, 9999),
            json.optString("profileIdentity").takeIf { it.matches(Regex("[A-Za-z0-9._-]{1,100}")) }
        )
    }

    suspend fun startDevice(csrf: String) {
        post("device-start", mapOf("csrf" to csrf))
    }

    suspend fun push(action: String, csrf: String, fields: Map<String, String> = emptyMap(), cookieSnapshot: String? = null): JSONObject {
        require(action in setOf("push-settings", "push-preferences", "push-subscribe", "push-unsubscribe"))
        return JSONObject(post(action, fields + ("csrf" to csrf), cookieSnapshot))
    }

    suspend fun confirm(stage: AntintwrkAuthStage, code: String, csrf: String): AntintwrkAuthSession {
        val result = post(if (stage == AntintwrkAuthStage.DEVICE) "device-verify" else "totp", mapOf("code" to code, "csrf" to csrf))
        if (stage == AntintwrkAuthStage.TOTP) require(org.json.JSONArray(result).optString(0) == "OK")
        return session()
    }

    /** Clear immediately; return only an in-memory snapshot for bounded server cleanup. */
    fun clearLocalSession(): String {
        val cookies = CookieManager.getInstance()
        val snapshot = cookies.getCookie(origin).orEmpty()
        snapshot.split(';').map { it.substringBefore('=').trim() }
            .filter { it.matches(Regex("[A-Za-z0-9_-]+")) }.forEach { name ->
                cookies.setCookie(origin, "$name=; Max-Age=0; Path=/; Secure; HttpOnly")
            }
        cookies.flush()
        android.webkit.WebStorage.getInstance().deleteOrigin(origin)
        return snapshot
    }

    suspend fun logout(csrf: String, cookieSnapshot: String? = null) {
        try {
            post("logout", mapOf("csrf" to csrf), cookieSnapshot)
        } finally {
            if (cookieSnapshot == null) clearLocalSession()
        }
    }
}
