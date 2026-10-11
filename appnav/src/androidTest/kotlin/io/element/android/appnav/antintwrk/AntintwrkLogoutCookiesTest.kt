// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import android.webkit.CookieManager
import android.webkit.WebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Appnav fixture jar only; no requests, accounts or actual credentials. */
@RunWith(AndroidJUnit4::class)
class AntintwrkLogoutCookiesTest {
    @Test
    fun clearDomainHostPathAndSsoCookiesBeforeAnotherAccount() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val cookies = CookieManager.getInstance()
        var web: WebView? = null
        instrumentation.runOnMainSync { web = WebView(instrumentation.targetContext) }
        try {
            val initial = CountDownLatch(1)
            instrumentation.runOnMainSync { cookies.removeAllCookies { initial.countDown() } }
            assertTrue(initial.await(10, TimeUnit.SECONDS))
            val fixtures = listOf(
                "https://antimsg.com/en/profile" to "fixture_domain=A; Domain=.antimsg.com; Path=/; Secure; HttpOnly",
                "https://antimsg.com/en/profile" to "fixture_path=A; Path=/en; Secure",
                "https://antintwrk.com/en/profile" to "fixture_global=A; Domain=.antintwrk.com; Path=/; Secure; HttpOnly",
                "https://auth.antimsg.com/" to "fixture_sso=A; Path=/; Secure; HttpOnly",
            )
            for ((url, value) in fixtures) {
                val saved = CountDownLatch(1)
                instrumentation.runOnMainSync { cookies.setCookie(url, value) { saved.countDown() } }
                assertTrue(saved.await(10, TimeUnit.SECONDS))
                assertTrue(cookies.getCookie(url).orEmpty().contains("=A"))
            }
            instrumentation.runOnMainSync { AntintwrkNativeAuthClient().clearLocalSession() }
            runBlocking { AntintwrkSocialCookieEpoch.awaitClear() }
            for ((url, _) in fixtures) assertTrue(cookies.getCookie(url).isNullOrEmpty())
            val saved = CountDownLatch(1)
            instrumentation.runOnMainSync {
                cookies.setCookie("https://antimsg.com/", "fixture_next=B; Domain=.antimsg.com; Path=/; Secure; HttpOnly") {
                    saved.countDown()
                }
            }
            assertTrue(saved.await(10, TimeUnit.SECONDS))
            assertTrue(cookies.getCookie("https://antimsg.com/").orEmpty().contains("fixture_next=B"))
        } finally {
            val removed = CountDownLatch(1)
            instrumentation.runOnMainSync {
                web?.destroy()
                cookies.removeAllCookies { removed.countDown() }
            }
            assertTrue(removed.await(10, TimeUnit.SECONDS))
        }
    }
}
