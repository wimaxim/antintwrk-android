// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test

class AntintwrkSmsInviteTest : RobolectricTest() {
    @Test fun `incompatible SMS handler falls back to a chooser with the invitation`() {
        val launched = mutableListOf<Intent>()
        antintwrkLaunchInvite("+15550123456", "https://antintwrk.com", "Invite friends") {
            launched.add(it)
            if (it.action == Intent.ACTION_SENDTO) throw android.content.ActivityNotFoundException()
        }
        assertThat(launched.map { it.action }).containsExactly(Intent.ACTION_SENDTO, Intent.ACTION_CHOOSER).inOrder()
        val shared = launched.last().getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertThat(shared?.getStringExtra(Intent.EXTRA_TEXT)).isEqualTo("https://antintwrk.com")
    }

    @Test fun `missing selected number still offers an invitation share`() {
        val launched = mutableListOf<Intent>()
        antintwrkLaunchInvite(null, "https://antintwrk.com", "Invite friends", launched::add)
        assertThat(launched.single().action).isEqualTo(Intent.ACTION_CHOOSER)
    }

    @Test fun `invitation opens an SMS draft and encodes the selected phone`() {
        val intent = antintwrkSmsIntent("+1 (555) 012-3456", "Привет! https://antintwrk.com")
        assertThat(intent.action).isEqualTo(Intent.ACTION_SENDTO)
        assertThat(intent.data?.scheme).isEqualTo("smsto")
        assertThat(intent.data?.schemeSpecificPart).isEqualTo("+15550123456")
        assertThat(intent.getStringExtra("sms_body")).isEqualTo("Привет! https://antintwrk.com")
        assertThat(intent.data?.toString()).doesNotContain("antimsg")
    }
    @Test fun `fallback lets the human share the public invitation`() {
        val intent = antintwrkInviteShareIntent("https://antintwrk.com")
        assertThat(intent.action).isEqualTo(Intent.ACTION_SEND)
        assertThat(intent.type).isEqualTo("text/plain")
        assertThat(intent.getStringExtra(Intent.EXTRA_TEXT)).isEqualTo("https://antintwrk.com")
    }
}
