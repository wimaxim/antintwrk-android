/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
@file:OptIn(ExperimentalTestApi::class)

package io.element.android.appnav.antintwrk

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import com.google.common.truth.Truth.assertThat
import io.element.android.compound.theme.ElementTheme
import io.element.android.tests.testutils.robolectric.RobolectricTest
import io.element.android.tests.testutils.setSafeContent
import org.junit.Test

class AntintwrkWelcomeViewTest : RobolectricTest() {
    @Test
    fun `sign in offers both providers without requiring an email`() = runAndroidComposeUiTest {
        val events = mutableListOf<AntintwrkWelcomeEvent>()
        setSafeContent {
            ElementTheme { AntintwrkWelcomeView(AntintwrkWelcomeState(page = AntintwrkPage.Login, eventSink = events::add)) }
        }
        onNodeWithText("Continue with Google").assertIsDisplayed().performClick()
        onNodeWithText("Sign in with a passkey").assertIsDisplayed().performClick()
        assertThat(events).containsExactly(AntintwrkWelcomeEvent.GoogleSignIn, AntintwrkWelcomeEvent.PasskeySignIn).inOrder()
    }

    @Test
    fun `registration offers Google but not a passkey for an existing account`() = runAndroidComposeUiTest {
        setSafeContent {
            ElementTheme { AntintwrkWelcomeView(AntintwrkWelcomeState(page = AntintwrkPage.Register)) }
        }
        onNodeWithText("Continue with Google").assertIsDisplayed()
        onNodeWithText("Sign in with a passkey").assertDoesNotExist()
    }

    @Test
    fun `welcome retains two account actions without a provider list`() = runAndroidComposeUiTest {
        setSafeContent { ElementTheme { AntintwrkWelcomeView(AntintwrkWelcomeState()) } }
        onNodeWithText("Create account").assertIsDisplayed()
        onNodeWithText("Sign in").assertIsDisplayed()
        onNodeWithText("Continue with Google").assertDoesNotExist()
        onNodeWithText("Sign in with a passkey").assertDoesNotExist()
    }
}
