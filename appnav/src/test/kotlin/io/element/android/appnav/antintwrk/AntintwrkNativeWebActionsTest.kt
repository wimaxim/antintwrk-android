// Copyright 2026 Maxim Novichkov
// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.json.JSONObject
import org.junit.Test

class AntintwrkNativeWebActionsTest : RobolectricTest() {
    private val referral = "synthetic_payload." + "a".repeat(43)

    @Test
    fun `assistance opens external browser with scoped signed gift referral`() {
        for (path in listOf("cerebralpalsycommunity.org/ru/give", "helpcp.org/en/help/davidnewman")) {
            val controller = AntintwrkWebFeedController()
            val target = "https://$path?source=heroworld&ref=$referral"
            var opened: String? = null
            var completed = false
            controller.onExternalLink = { opened = it }
            AntintwrkNativeWebActions.handle(ApplicationProvider.getApplicationContext(), controller, request(target)) { completed = it }
            assertThat(opened).isEqualTo(target)
            assertThat(completed).isTrue()
        }
    }

    @Test
    fun `ambiguous referrals and other payment destinations are rejected`() {
        val base = "https://helpcp.org/en/help/davidnewman?source=heroworld&ref=$referral"
        for (target in listOf(
            "$base&source=other",
            "$base&ref=another",
            "$base#fragment",
            "$base&fragment=1",
            "$base&amount=20",
            "https://helpcp.org/en/help/davidnewman?source=heroworld&ref=invalid",
            "https://helpcp.org.evil.example/en/help/davidnewman?source=heroworld&ref=$referral",
            "https://checkout.stripe.com/pay/test",
            "https://antimsg.com/en/ukraine",
            "https://antintwrk.com/en/kindness",
        )) {
            val controller = AntintwrkWebFeedController()
            var opened = false
            var completed = true
            controller.onExternalLink = { opened = true }
            AntintwrkNativeWebActions.handle(ApplicationProvider.getApplicationContext(), controller, request(target)) { completed = it }
            assertThat(opened).isFalse()
            assertThat(completed).isFalse()
        }
    }

    private fun request(url: String) = JSONObject().put("type", "open-external").put("url", url)
}
