// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.features.roomdetails.impl

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test

class AntintwrkSmsInviteTest : RobolectricTest() {
    @Test fun `invitation opens an SMS draft and encodes the selected phone`() {
        val intent = antintwrkSmsIntent("+1 (555) 012-3456", "Привет! https://antintwrk.com")
        assertThat(intent.action).isEqualTo(Intent.ACTION_SENDTO)
        assertThat(intent.data?.scheme).isEqualTo("smsto")
        assertThat(intent.data?.schemeSpecificPart).isEqualTo("+15550123456")
        assertThat(intent.getStringExtra("sms_body")).isEqualTo("Привет! https://antintwrk.com")
        assertThat(intent.data?.toString()).doesNotContain("antimsg")
    }
}
