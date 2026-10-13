/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
@file:OptIn(ExperimentalTestApi::class)

package io.element.android.appnav.antintwrk

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import com.google.common.truth.Truth.assertThat
import io.element.android.compound.theme.ElementTheme
import io.element.android.tests.testutils.robolectric.RobolectricTest
import io.element.android.tests.testutils.setSafeContent
import org.junit.Test

class AntintwrkWelcomeViewTest : RobolectricTest() {
    @Test
    fun `guest sign in opens terms from the notice without sending a login`() = termsNotice(AntintwrkPage.Login)

    @Test
    fun `first registration screen opens terms without starting registration`() = termsNotice(AntintwrkPage.Register)

    @Test
    fun `reading terms at final registration preserves the unchecked consent and form`() = termsNotice(AntintwrkPage.Profile)

    private fun termsNotice(page: AntintwrkPage) = runAndroidComposeUiTest {
        val events = mutableListOf<AntintwrkWelcomeEvent>()
        setSafeContent {
            ElementTheme {
                AntintwrkWelcomeView(AntintwrkWelcomeState(
                    page = page,
                    nativeLogin = true,
                    email = "test@example.test",
                    firstName = "Test",
                    agreed = false,
                    eventSink = events::add,
                ))
            }
        }
        val sentence = if (page == AntintwrkPage.Login) {
            "By signing in, you agree to the Terms\u00A0of\u00A0ANTINTWRK."
        } else {
            "By registering, you agree to the Terms\u00A0of\u00A0ANTINTWRK."
        }
        val notice = onNodeWithText(sentence, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        // Invoke the actual accessible link action: Robolectric's stub font gives it zero width.
        notice.onChildren().onFirst().performSemanticsAction(SemanticsActions.OnClick) { it() }
        mainClock.advanceTimeBy(500)
        onNodeWithTag("antintwrk-terms-reader").assertExists()
        onNodeWithTag("antintwrk-terms-back").performClick()
        onNodeWithTag("antintwrk-terms-reader").assertDoesNotExist()
        onNodeWithText("test@example.test").assertExists()
        if (page == AntintwrkPage.Profile) onNode(isToggleable()).assertIsOff()
        runOnIdle { assertThat(events).isEmpty() }
    }

    @Test
    fun `sign in offers both providers without requiring an email`() = runAndroidComposeUiTest {
        val events = mutableListOf<AntintwrkWelcomeEvent>()
        setSafeContent {
            ElementTheme { AntintwrkWelcomeView(AntintwrkWelcomeState(page = AntintwrkPage.Login, eventSink = events::add)) }
        }
        onNodeWithText("Continue with Google").assertIsDisplayed().performClick()
        onNodeWithText("Sign in with a passkey").performScrollTo().assertIsDisplayed().performClick()
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
