// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.domerrors.SecurityError
import androidx.credentials.exceptions.publickeycredential.GetPublicKeyCredentialDomException
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AntintwrkNativeCredentialsTest {
    private val csrf = "a".repeat(64)
    private val assertion = """{"id":"fixture","rawId":"fixture","type":"public-key","response":{}}"""

    @Test
    fun `successful system passkey continues into cookie backed admission and retains challenges`() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val manager = mockk<CredentialManager>()
        coEvery {
            manager.getCredential(any<android.content.Context>(), any<GetCredentialRequest>())
        } returns GetCredentialResponse(PublicKeyCredential(assertion))
        val credentials = AntintwrkNativeCredentials(context, manager)
        val server = MockWebServer()
        server.start()
        try {
            val client = AntintwrkNativeAuthClient(server.url("/").toString().trimEnd('/'))
            for (stage in listOf("AUTHENTICATED", "TOTP", "DEVICE", "LOGIN")) {
                server.enqueue(MockResponse().setBody("""{"stage":"LOGIN","csrf":"$csrf"}"""))
                server.enqueue(MockResponse().setBody("""{"ticket":"one-use","options":{"challenge":"fixture","rpId":"antintwrk.com"}}"""))
                server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/"))
                server.enqueue(MockResponse().setBody("""{"stage":"$stage","csrf":"$csrf"}"""))
                val outcome = runCatching { credentials.signIn(context, client, "passkey") }
                if (stage == "LOGIN") {
                    assertThat(outcome.isFailure).isTrue()
                    assertThat(outcome.exceptionOrNull()).isNotInstanceOf(AntintwrkCredentialDismissed::class.java)
                } else {
                    assertThat(outcome.getOrThrow().stage.name).isEqualTo(stage)
                }
                assertThat(server.takeRequest().body.readUtf8()).isEqualTo("action=session")
                assertThat(server.takeRequest().body.readUtf8()).contains("action=passkey-options")
                val proof = server.takeRequest().body.readUtf8()
                assertThat(proof).contains("action=passkey&credential=")
                assertThat(proof).contains("ticket=one-use&csrf=$csrf")
                assertThat(server.takeRequest().body.readUtf8()).isEqualTo("action=session")
            }
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `dismissing the system picker submits no credential and is distinct from a server failure`() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val manager = mockk<CredentialManager>()
        coEvery { manager.getCredential(any<android.content.Context>(), any<GetCredentialRequest>()) } throws GetCredentialCancellationException()
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"stage":"LOGIN","csrf":"$csrf"}"""))
            server.enqueue(MockResponse().setBody("""{"ticket":"one-use","options":{"challenge":"fixture","rpId":"antintwrk.com"}}"""))
            val error = runCatching {
                AntintwrkNativeCredentials(context, manager).signIn(context, AntintwrkNativeAuthClient(server.url("/").toString().trimEnd('/')), "passkey")
            }.exceptionOrNull()
            assertThat(error).isInstanceOf(AntintwrkCredentialDismissed::class.java)
            assertThat(server.requestCount).isEqualTo(2)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `provider failures remain distinct and never send a credential or expose provider data`() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val manager = mockk<CredentialManager>()
        val server = MockWebServer()
        server.start()
        try {
            for ((providerError, expectedCode) in listOf(
                NoCredentialException("private-provider-data") to "PK-NO-KEY",
                GetPublicKeyCredentialDomException(SecurityError(), "private-provider-data") to "PK-SECURITY",
            )) {
                coEvery { manager.getCredential(any<android.content.Context>(), any<GetCredentialRequest>()) } throws providerError
                server.enqueue(MockResponse().setBody("""{"stage":"LOGIN","csrf":"$csrf"}"""))
                server.enqueue(MockResponse().setBody("""{"ticket":"one-use","options":{"challenge":"fixture","rpId":"antintwrk.com"}}"""))
                val failure = runCatching {
                    AntintwrkNativeCredentials(context, manager).signIn(context, AntintwrkNativeAuthClient(server.url("/").toString().trimEnd('/')), "passkey")
                }.exceptionOrNull() as AntintwrkPasskeyFailure
                assertThat(failure.code).isEqualTo(expectedCode)
                assertThat(failure.verification).isFalse()
                assertThat(failure.message).isNull()
                assertThat(failure.cause).isNull()
                assertThat(server.takeRequest().body.readUtf8()).isEqualTo("action=session")
                assertThat(server.takeRequest().body.readUtf8()).contains("action=passkey-options")
            }
            assertThat(server.requestCount).isEqualTo(4)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `server rejects a returned credential without admitting or repeating it`() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val manager = mockk<CredentialManager>()
        coEvery { manager.getCredential(any<android.content.Context>(), any<GetCredentialRequest>()) } returns
            GetCredentialResponse(PublicKeyCredential(assertion))
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"stage":"LOGIN","csrf":"$csrf"}"""))
            server.enqueue(MockResponse().setBody("""{"ticket":"one-use","options":{"challenge":"fixture","rpId":"antintwrk.com"}}"""))
            server.enqueue(MockResponse().setResponseCode(400).setBody("""{"error":"CREDENTIAL_INVALID"}"""))
            val failure = runCatching {
                AntintwrkNativeCredentials(context, manager).signIn(context, AntintwrkNativeAuthClient(server.url("/").toString().trimEnd('/')), "passkey")
            }.exceptionOrNull() as AntintwrkPasskeyFailure
            assertThat(failure.code).isEqualTo("PK-VERIFY")
            assertThat(failure.verification).isTrue()
            assertThat(server.requestCount).isEqualTo(3)
        } finally {
            server.shutdown()
        }
    }
}
