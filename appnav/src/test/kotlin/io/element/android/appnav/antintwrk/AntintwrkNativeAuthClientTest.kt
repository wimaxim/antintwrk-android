/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import android.webkit.CookieManager
import com.google.common.truth.Truth.assertThat
import io.element.android.appnav.BuildConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AntintwrkNativeAuthClientTest {
    private val server = MockWebServer()
    private val csrf = "a".repeat(64)
    private lateinit var client: AntintwrkNativeAuthClient

    @Before
    fun setup() {
        server.start()
        client = AntintwrkNativeAuthClient(server.url("/").toString().trimEnd('/'))
    }

    @After
    fun cleanup() {
        server.shutdown()
    }

    private fun session(stage: String) = MockResponse().setBody("{\"stage\":\"$stage\",\"csrf\":\"$csrf\"}")

    @Test
    fun `expired and rate limited requests remain distinct and never trigger a login retry`() = runTest {
        for ((status, code) in listOf(403 to "SESSION_EXPIRED", 429 to "RATE_LIMITED", 503 to "UNAVAILABLE")) {
            server.enqueue(MockResponse().setResponseCode(status).setBody("""{"error":"$code"}"""))
            val error = runCatching { client.login("member@example.invalid", "fixture", "en", csrf) }.exceptionOrNull()
            assertThat(error).isInstanceOf(AntintwrkRequestError::class.java)
            assertThat((error as AntintwrkRequestError).code).isEqualTo(code)
            server.takeRequest()
        }
        assertThat(server.requestCount).isEqualTo(3)
    }

    @Test
    fun `bootstrap accepts only declared server stages and sends protected POST headers`() = runTest {
        for (stage in AntintwrkAuthStage.entries) {
            server.enqueue(session(stage.name))
            assertThat(client.session().stage).isEqualTo(stage)
            val request = server.takeRequest()
            assertThat(request.method).isEqualTo("POST")
            assertThat(request.getHeader("X-Antintwrk-Client")).isEqualTo("native")
            assertThat(request.getHeader("User-Agent")).isEqualTo(AntintwrkNativeAuthClient.USER_AGENT)
            assertThat(request.getHeader("User-Agent"))
                .isEqualTo("ANTINTWRK Android native-client/1.0 app/${BuildConfig.ANTINTWRK_APP_VERSION} (+https://qwertynetworks.com)")
            assertThat(request.body.readUtf8()).isEqualTo("action=session")
        }
    }

    @Test
    fun `password login requires subsequent server admission rather than successful HTTP alone`() = runTest {
        server.enqueue(MockResponse().setBody(""))
        server.enqueue(session("LOGIN"))
        assertThat(runCatching { client.login("a@example.invalid", "test-value", "ru", csrf) }.isFailure).isTrue()
        server.takeRequest()
        server.takeRequest()
        server.enqueue(MockResponse().setBody("OK"))
        server.enqueue(session("TOTP"))
        assertThat(client.login("a@example.invalid", "test-value", "ru", csrf).stage).isEqualTo(AntintwrkAuthStage.TOTP)
        val body = server.takeRequest().body.readUtf8()
        assertThat(body).contains("csrf=$csrf")
        assertThat(body).contains("lng=ru")
        server.takeRequest()
    }

    @Test
    fun `malformed unknown oversized and failing responses never admit a session`() = runTest {
        for (response in listOf(
                session("UNKNOWN"),
                MockResponse().setBody("{}"),
                MockResponse().setResponseCode(503),
            MockResponse().setChunkedBody("x".repeat(65537), 1024)
            )) {
            server.enqueue(response)
            assertThat(runCatching { client.session() }.isFailure).isTrue()
            server.takeRequest()
        }
    }

    @Test
    fun `device success redirect is not followed and still requires server admission`() = runTest {
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/"))
        server.enqueue(session("AUTHENTICATED"))
        assertThat(client.confirm(AntintwrkAuthStage.DEVICE, "1234", csrf).stage).isEqualTo(AntintwrkAuthStage.AUTHENTICATED)
        assertThat(server.takeRequest().body.readUtf8()).contains("action=device-verify")
        assertThat(server.takeRequest().body.readUtf8()).isEqualTo("action=session")
    }

    @Test
    fun `local engine login redirects require cookie backed admission and are never followed`() = runTest {
        for (stage in listOf("DEVICE", "TOTP", "AUTHENTICATED", "LOGIN")) {
            server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/"))
            server.enqueue(session(stage))
            val result = runCatching { client.login("member@example.invalid", "fixture-password", "en", csrf) }
            if (stage == "LOGIN") {
                assertThat(result.isFailure).isTrue()
            } else {
                assertThat(result.getOrThrow().stage.name).isEqualTo(stage)
            }
            assertThat(server.takeRequest().path).isEqualTo("/antintwrk_mobile.php")
            assertThat(server.takeRequest().body.readUtf8()).isEqualTo("action=session")
        }
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/"))
        server.enqueue(session("TOTP"))
        assertThat(client.providerSignIn("passkey", "fixture", "ticket", csrf).stage).isEqualTo(AntintwrkAuthStage.TOTP)
        server.takeRequest()
        server.takeRequest()
    }

    @Test
    fun `unexpected redirects never trigger admission or follow external links`() = runTest {
        for (location in listOf("https://example.invalid", "/other", "//example.invalid")) {
            server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", location))
            assertThat(runCatching { client.login("member@example.invalid", "fixture", "en", csrf) }.isFailure).isTrue()
            server.takeRequest()
        }
        assertThat(server.requestCount).isEqualTo(3)
    }

    @Test
    fun `engine password refusal and lockout are displayed as plain text without granting admission`() = runTest {
        for (message in listOf("Incorrect username or password!", "Your account is locked for 15 minutes.")) {
            server.enqueue(MockResponse().setBody("<b>$message</b>"))
            val error = runCatching { client.login("member@example.invalid", "fixture", "en", csrf) }.exceptionOrNull()
            assertThat(error).isInstanceOf(AntintwrkAccountError::class.java)
            assertThat((error as AntintwrkAccountError).messageForUser).isEqualTo(message)
            server.takeRequest()
        }
        assertThat(server.requestCount).isEqualTo(2)
    }

    @Test
    fun `logout clears local social cookies even if server is unavailable`() = runTest {
        val origin = server.url("/").toString().trimEnd('/')
        val cookies = CookieManager.getInstance()
        cookies.setCookie(origin, "native_test=value; Path=/")
        server.enqueue(MockResponse().setResponseCode(503))
        runCatching { client.logout(csrf) }
        assertThat(cookies.getCookie(origin).orEmpty()).doesNotContain("native_test=value")
    }

    @Test
    fun `provider response alone never grants access and challenge binding is sent to server`() = runTest {
        server.enqueue(MockResponse().setBody("{\"nonce\":\"fixture\",\"ticket\":\"ticket\",\"clientId\":\"fixture-client\"}"))
        assertThat(client.providerOptions("google", csrf).getString("nonce")).isEqualTo("fixture")
        assertThat(server.takeRequest().body.readUtf8()).contains("action=google-options")
        server.enqueue(MockResponse().setBody("{\"ok\":true}"))
        server.enqueue(session("LOGIN"))
        assertThat(runCatching { client.providerSignIn("google", "fixture-token", "ticket", csrf) }.isFailure).isTrue()
        val request = server.takeRequest().body.readUtf8()
        assertThat(request).contains("ticket=ticket")
        assertThat(request).contains("csrf=$csrf")
        server.takeRequest()
        server.enqueue(MockResponse().setBody(""))
        server.enqueue(session("TOTP"))
        assertThat(client.providerSignIn("passkey", "fixture-assertion", "ticket", csrf).stage).isEqualTo(AntintwrkAuthStage.TOTP)
        server.takeRequest()
        server.takeRequest()
    }
    @Test
    fun `registration and reset use protected transport and do not imply admission`() = runTest {
        for (action in listOf("register", "reset")) {
            server.enqueue(MockResponse().setBody("OK"))
            client.requestAccount(action, mapOf("email" to "member@example.invalid", "agreement" to "1"), csrf)
            val request = server.takeRequest()
            assertThat(request.method).isEqualTo("POST")
            assertThat(request.body.readUtf8()).contains("action=$action")
            assertThat(request.getHeader("Cookie")).isNotNull()
        }
        server.enqueue(MockResponse().setBody("Please use another email."))
        assertThat(runCatching { client.requestAccount("register", emptyMap(), csrf) }.exceptionOrNull())
            .isInstanceOf(AntintwrkAccountError::class.java)
    }

    @Test
    fun `google newcomer requires a separate consent-bound registration request`() = runTest {
        server.enqueue(MockResponse().setBody("""{"registrationTicket":"one-time","email":"new@example.invalid","firstName":"New"}"""))
        val error = runCatching { client.providerSignIn("google", "opaque", "ticket", csrf) }.exceptionOrNull()
        assertThat(error).isInstanceOf(AntintwrkRegistrationRequired::class.java)
        assertThat((error as AntintwrkRegistrationRequired).ticket).isEqualTo("one-time")
        server.takeRequest()
        server.enqueue(MockResponse().setBody("""{"ok":true}"""))
        server.enqueue(session("TOTP"))
        assertThat(client.registerGoogle(mapOf("agreement" to "1"), "one-time", csrf).stage).isEqualTo(AntintwrkAuthStage.TOTP)
        assertThat(server.takeRequest().body.readUtf8()).contains("action=google-register")
        server.takeRequest()
    }

    @Test
    fun `invalid profile errors never admit an account`() = runTest {
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"error":"PROFILE"}"""))
        val error = runCatching { client.requestAccount("register", emptyMap(), csrf) }.exceptionOrNull()
        assertThat(error).isInstanceOf(AntintwrkRequestError::class.java)
        assertThat((error as AntintwrkRequestError).code).isEqualTo("PROFILE")
    }

    @Test
    fun `logout clears the whole private cookie jar including both domains and SSO`() {
        val cookies = mockk<CookieManager>(relaxed = true)
        every { cookies.getCookie("https://antimsg.com") } returns "fixture_source=ru"
        every { cookies.getCookie("https://antintwrk.com") } returns "fixture_target=public"
        mockkStatic(CookieManager::class)
        every { CookieManager.getInstance() } returns cookies
        every { cookies.removeAllCookies(any()) } answers {
            firstArg<android.webkit.ValueCallback<Boolean>>().onReceiveValue(true)
        }
        try {
            AntintwrkNativeAuthClient().clearLocalSession()
            verify(exactly = 1) { cookies.removeAllCookies(any()) }
            verify(exactly = 0) { cookies.setCookie(any(), any()) }
        } finally {
            unmockkStatic(CookieManager::class)
        }
    }

    @Test
    fun `new admission waits for the asynchronous cookie removal callback`() = runTest {
        val cookies = mockk<CookieManager>(relaxed = true)
        var callback: android.webkit.ValueCallback<Boolean>? = null
        every { cookies.removeAllCookies(any()) } answers { callback = firstArg() }
        mockkStatic(CookieManager::class)
        every { CookieManager.getInstance() } returns cookies
        try {
            client.clearLocalSession()
            server.enqueue(session("LOGIN"))
            val pending = async(start = CoroutineStart.UNDISPATCHED) { client.session() }
            assertThat(server.requestCount).isEqualTo(0)
            requireNotNull(callback).onReceiveValue(true)
            assertThat(pending.await().stage).isEqualTo(AntintwrkAuthStage.LOGIN)
            assertThat(server.takeRequest().getHeader("Cookie").orEmpty()).isEmpty()
        } finally {
            callback?.onReceiveValue(true)
            unmockkStatic(CookieManager::class)
        }
    }

    @Test
    fun `old server logout response cannot replace the next account cookies`() = runTest {
        val origin = server.url("/").toString()
        CookieManager.getInstance().setCookie(origin, "fixture_actor=B; Path=/")
        server.enqueue(MockResponse().setBody("OK").setHeader("Set-Cookie", "fixture_actor=A; Path=/"))
        client.logout(csrf, "fixture_actor=A")
        assertThat(server.takeRequest().getHeader("Cookie")).isEqualTo("fixture_actor=A")
        assertThat(CookieManager.getInstance().getCookie(origin)).contains("fixture_actor=B")
    }

    @Test
    fun `an HTTP response arriving after logout cannot restore a cookie`() = runTest {
        val release = CountDownLatch(1)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                check(release.await(10, TimeUnit.SECONDS))
                return session("LOGIN").setHeader("Set-Cookie", "late_fixture=denied; Path=/")
            }
        }
        val pending = async(start = CoroutineStart.UNDISPATCHED) { runCatching { client.session() } }
        withContext(Dispatchers.IO) { checkNotNull(server.takeRequest(10, TimeUnit.SECONDS)) }
        client.clearLocalSession()
        release.countDown()
        assertThat(pending.await().isFailure).isTrue()
        assertThat(CookieManager.getInstance().getCookie(server.url("/").toString()).orEmpty()).doesNotContain("late_fixture=")
    }
}
