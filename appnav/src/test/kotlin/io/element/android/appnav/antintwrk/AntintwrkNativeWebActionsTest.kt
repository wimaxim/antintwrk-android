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
    fun `assistance opens external browser with explicit signed no reward referral`() {
        for (path in listOf("cerebralpalsycommunity.org/ru/give", "helpcp.org/en/help/davidnewman")) {
            val controller = AntintwrkWebFeedController()
            val target = "https://$path?source=heroworld&ref=$referral&donation_only=1"
            var opened: String? = null
            var completed = false
            controller.onExternalLink = { opened = it }
            AntintwrkNativeWebActions.handle(ApplicationProvider.getApplicationContext(), controller, request(target)) { completed = it }
            assertThat(opened).isEqualTo(target)
            assertThat(completed).isTrue()
        }
    }

    @Test
    fun `missing or ambiguous reward attribution and other payment destinations are rejected`() {
        val base = "https://helpcp.org/en/help/davidnewman?source=heroworld&ref=$referral"
        for (target in listOf(
            base,
            "$base&donation_only=0",
            "$base&donation_only=1&donation_only=0",
            "$base&donation_only=1&ref=other",
            "$base&donation_only=1&amount=20",
            "https://helpcp.org/en/help/davidnewman?source=heroworld&ref=invalid&donation_only=1",
            "https://helpcp.org.evil.example/en/help/davidnewman?source=heroworld&ref=$referral&donation_only=1",
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
