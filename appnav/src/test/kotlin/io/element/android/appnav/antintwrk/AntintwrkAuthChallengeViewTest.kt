/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "en")
class AntintwrkAuthChallengeViewTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `device challenge clearly identifies email and allows native confirmation`() {
        var confirmed = false
        compose.setContent { AntintwrkAuthChallengeView(AntintwrkAuthStage.DEVICE, "1234", false, false, {}, { confirmed = true }, {}) }
        compose.onNodeWithText("Enter the code sent to your email").assertIsDisplayed()
        compose.onNodeWithText("Continue").performClick()
        assertThat(confirmed).isTrue()
    }

    @Test
    fun `busy two factor challenge disables duplicate submissions and shows the recovery error`() {
        compose.setContent { AntintwrkAuthChallengeView(AntintwrkAuthStage.TOTP, "", true, true, {}, {}, {}) }
        compose.onNodeWithText("Enter your authenticator code").assertIsDisplayed()
        compose.onNodeWithText("Continue").assertIsNotEnabled()
        compose.onNodeWithText("Sign-in failed. Check your details or connection and try again.").assertIsDisplayed()
    }

    @Test
    fun `required setup has no misleading code submission and always provides exit`() {
        var left = false
        compose.setContent { AntintwrkAuthChallengeView(AntintwrkAuthStage.SETUP_REQUIRED, "", false, false, {}, {}, { left = true }) }
        compose.onNodeWithText("Continue").assertDoesNotExist()
        compose.onNodeWithText("Back").performClick()
        assertThat(left).isTrue()
    }
}
