// SPDX-License-Identifier: AGPL-3.0-only
@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package io.element.android.appnav.antintwrk

import android.content.Intent
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.tests.testutils.robolectric.RobolectricTest
import io.element.android.tests.testutils.setSafeContent
import org.junit.Test
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

class AntintwrkInviteShareTest : RobolectricTest() {
    @Test
    fun `settings invite immediately opens unrestricted text sharesheet`() = runAndroidComposeUiTest {
        setSafeContent {
            val invite = rememberAntintwrkInviteShare()
            TextButton(onClick = invite) { Text("Invite") }
        }
        onNodeWithText("Invite").performClick()
        val chooser = shadowOf(activity!!).nextStartedActivity
        assertThat(chooser.action).isEqualTo(Intent.ACTION_CHOOSER)
        val share = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertThat(share.action).isEqualTo(Intent.ACTION_SEND)
        assertThat(share.type).isEqualTo("text/plain")
        assertThat(share.`package`).isNull()
        assertThat(share.component).isNull()
        assertThat(
            share.getStringExtra(Intent.EXTRA_TEXT)
        ).isEqualTo(io.element.android.tests.testutils.InstrumentationStringProvider().getString(CommonStrings.screen_ant_invite_message))
        assertThat(share.getStringExtra(Intent.EXTRA_TEXT)).contains("https://antintwrk.com")
        assertThat(shadowOf(activity!!).nextStartedActivity).isNull()
    }

    @Config(qualifiers = "ru")
    @Test
    fun `settings invite shares the localized invitation`() = runAndroidComposeUiTest {
        setSafeContent {
            val invite = rememberAntintwrkInviteShare()
            TextButton(onClick = invite) { Text("Invite") }
        }
        onNodeWithText("Invite").performClick()
        val chooser = shadowOf(activity!!).nextStartedActivity
        val share = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertThat(share.getStringExtra(Intent.EXTRA_TEXT)).startsWith("Привет!")
        assertThat(chooser.getStringExtra(Intent.EXTRA_TITLE)).isEqualTo("Пригласить друзей")
    }
}
