/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import android.webkit.CookieManager
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
    fun `bootstrap accepts only declared server stages and sends protected POST headers`() = runTest {
        for (stage in AntintwrkAuthStage.entries) {
            server.enqueue(session(stage.name))
            assertThat(client.session().stage).isEqualTo(stage)
            val request = server.takeRequest()
            assertThat(request.method).isEqualTo("POST")
            assertThat(request.getHeader("X-Antintwrk-Client")).isEqualTo("native")
            assertThat(request.getHeader("User-Agent")).isEqualTo(AntintwrkNativeAuthClient.USER_AGENT)
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
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "https://example.invalid"))
        server.enqueue(session("AUTHENTICATED"))
        assertThat(client.confirm(AntintwrkAuthStage.DEVICE, "1234", csrf).stage).isEqualTo(AntintwrkAuthStage.AUTHENTICATED)
        assertThat(server.takeRequest().body.readUtf8()).contains("action=device-verify")
        assertThat(server.takeRequest().body.readUtf8()).isEqualTo("action=session")
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
}
