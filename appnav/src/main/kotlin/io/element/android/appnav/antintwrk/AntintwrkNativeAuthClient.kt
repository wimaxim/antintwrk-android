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
internal data class AntintwrkAuthSession(val stage: AntintwrkAuthStage, val csrf: String)

/** No logging interceptor, password persistence, browser redirects or bearer tokens. */
internal class AntintwrkNativeAuthClient(
    private val origin: String = BuildConfig.ANTINTWRK_SOCIAL_ORIGIN,
    private val http: OkHttpClient = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
        .callTimeout(30, TimeUnit.SECONDS).build(),
) {
    companion object {
        const val USER_AGENT = "ANTINTWRK Android native-client/1.0 (+https://qwertynetworks.com)"
    }

    private suspend fun post(action: String, fields: Map<String, String> = emptyMap()): String = withContext(Dispatchers.IO) {
        val cookies = CookieManager.getInstance()
        val body = FormBody.Builder().add("action", action).apply { fields.forEach { (key, value) -> add(key, value) } }.build()
        val request = Request.Builder().url("$origin/antintwrk_mobile.php")
            .header("Origin", origin).header("X-Antintwrk-Client", "native").header("User-Agent", USER_AGENT)
            .header("Cookie", cookies.getCookie(origin).orEmpty()).post(body).build()
        http.newCall(request).execute().use { response ->
            response.headers.values("Set-Cookie").forEach { cookies.setCookie(origin, it) }
            cookies.flush()
            // Device confirmation deliberately uses the existing engine's success redirect.
            if (response.code == 302 && action == "device-verify") return@withContext ""
            require(response.body.contentLength() <= 65536) { "Response size" }
            val source = response.body.source()
            source.request(65537)
            require(source.buffer.size <= 65536 && response.isSuccessful) { "Request failed" }
            source.buffer.readUtf8()
        }
    }

    suspend fun session(): AntintwrkAuthSession {
        val json = JSONObject(post("session"))
        val csrf = json.getString("csrf")
        require(csrf.matches(Regex("[a-f0-9]{64}")))
        return AntintwrkAuthSession(AntintwrkAuthStage.valueOf(json.getString("stage")), csrf)
    }

    suspend fun login(email: String, password: String, language: String, csrf: String): AntintwrkAuthSession {
        val result = post("login", mapOf("email" to email.trim(), "password" to password, "lng" to language, "csrf" to csrf)).trim()
        require(result.isEmpty() || result == "OK") { "Sign in failed" }
        return session().also { require(it.stage != AntintwrkAuthStage.LOGIN) { "Sign in failed" } }
    }

    suspend fun providerOptions(provider: String, csrf: String): JSONObject = JSONObject(post("$provider-options", mapOf("csrf" to csrf)))
    suspend fun providerSignIn(provider: String, credential: String, ticket: String, csrf: String): AntintwrkAuthSession {
        val result = post(provider, mapOf("credential" to credential, "ticket" to ticket, "csrf" to csrf)).trim()
        if (provider == "passkey") {
            require(result.isEmpty() || result == "OK")
        } else {
            require(JSONObject(result).optBoolean("ok"))
        }
        return session().also { require(it.stage != AntintwrkAuthStage.LOGIN) }
    }

    suspend fun startDevice(csrf: String) {
        post("device-start", mapOf("csrf" to csrf))
    }

    suspend fun confirm(stage: AntintwrkAuthStage, code: String, csrf: String): AntintwrkAuthSession {
        val result = post(if (stage == AntintwrkAuthStage.DEVICE) "device-verify" else "totp", mapOf("code" to code, "csrf" to csrf))
        if (stage == AntintwrkAuthStage.TOTP) require(org.json.JSONArray(result).optString(0) == "OK")
        return session()
    }

    suspend fun logout(csrf: String) {
        try {
            post("logout", mapOf("csrf" to csrf))
        } finally {
            val cookies = CookieManager.getInstance()
            cookies.getCookie(origin).orEmpty().split(';').map { it.substringBefore('=').trim() }
                .filter { it.matches(Regex("[A-Za-z0-9_-]+")) }.forEach { name ->
                    cookies.setCookie(origin, "$name=; Max-Age=0; Path=/; Secure; HttpOnly")
                }
            cookies.flush()
        }
    }
}
