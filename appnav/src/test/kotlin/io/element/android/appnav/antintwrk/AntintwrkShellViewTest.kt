/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
@file:OptIn(ExperimentalTestApi::class)

package io.element.android.appnav.antintwrk

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import com.google.common.truth.Truth.assertThat
import io.element.android.compound.theme.ElementTheme
import io.element.android.tests.testutils.robolectric.RobolectricTest
import io.element.android.tests.testutils.setSafeContent
import org.junit.Test
import org.robolectric.annotation.Config

class AntintwrkShellViewTest : RobolectricTest() {
    @Test
    @Config(qualifiers = "w320dp-h480dp")
    fun `feed position survives switching to messenger and another social section`() = runAndroidComposeUiTest {
        val presenter = AntintwrkShellPresenter()
        setSafeContent { ElementTheme { AntintwrkShellView(presenter.present()) } }
        onNodeWithTag("antintwrk-feed").performScrollToNode(hasTestTag("demo-post-2"))
        val position = onNodeWithTag("demo-post-2").getBoundsInRoot().top
        onNode(hasText("Messenger") and hasClickAction()).performClick()
        onNode(hasText("Social") and hasClickAction()).performClick()
        assertThat(onNodeWithTag("demo-post-2").getBoundsInRoot().top).isEqualTo(position)
        onNode(hasText("Communities") and hasClickAction()).performClick()
        onNode(hasText("Feed") and hasClickAction()).performClick()
        assertThat(onNodeWithTag("demo-post-2").getBoundsInRoot().top).isEqualTo(position)
    }

    @Test
    fun `social section survives mode switching and messenger does not show social navigation`() = runAndroidComposeUiTest {
        val presenter = AntintwrkShellPresenter()
        setSafeContent { ElementTheme { AntintwrkShellView(presenter.present()) } }
        onNode(hasText("Communities") and hasClickAction()).performClick()
        onNodeWithText("Your communities will appear here once account access is connected.").assertIsDisplayed()
        onNode(hasText("Messenger") and hasClickAction()).performClick()
        onNodeWithText("Chats and calls will appear here once your account is connected.").assertIsDisplayed()
        onNodeWithContentDescription("Search").assertDoesNotExist()
        onNodeWithContentDescription("Notifications").assertDoesNotExist()
        onNode(hasText("Feed") and hasClickAction()).assertDoesNotExist()
        onNode(hasText("Social") and hasClickAction()).performClick()
        onNode(hasText("Communities") and hasClickAction()).assertIsSelected()
    }

    @Test
    fun `search filters demo publications and handles no results without leaving the section`() = runAndroidComposeUiTest {
        val presenter = AntintwrkShellPresenter()
        setSafeContent { ElementTheme { AntintwrkShellView(presenter.present()) } }
        onNode(hasText("Profile") and hasClickAction()).performClick()
        onNodeWithContentDescription("Search").performClick()
        onNode(hasSetTextAction()).performTextInput("zzzzzz")
        onNodeWithText("No demo publications found.").assertIsDisplayed()
        onNodeWithText("Close").performClick()
        onNode(hasText("Profile") and hasClickAction()).assertIsSelected()
    }

    @Test
    fun `search matches localized demo text`() = runAndroidComposeUiTest {
        val presenter = AntintwrkShellPresenter()
        setSafeContent { ElementTheme { AntintwrkShellView(presenter.present()) } }
        onNode(hasText("Profile") and hasClickAction()).performClick()
        onNodeWithContentDescription("Search").performClick()
        onNode(hasSetTextAction()).performTextInput("  CONVERSATIONS  ")
        onNodeWithText("A place for conversations").assertIsDisplayed()
        onNodeWithText("Find your community").assertDoesNotExist()
    }

    @Test
    fun `local vote and feed scope survive switching away from the feed`() = runAndroidComposeUiTest {
        val presenter = AntintwrkShellPresenter()
        setSafeContent { ElementTheme { AntintwrkShellView(presenter.present()) } }
        onNodeWithText("My network").performClick()
        onNodeWithTag("demo-post-2").assertDoesNotExist()
        onNodeWithTag("antintwrk-feed").performScrollToNode(hasContentDescription("Interesting"))
        onAllNodesWithContentDescription("Interesting")[0].performClick()
        onNodeWithTag("demo-vote-1").assertTextEquals("1")
        onNode(hasText("Messenger") and hasClickAction()).performClick()
        onNode(hasText("Social") and hasClickAction()).performClick()
        onNodeWithText("My network").assertIsSelected()
        onNodeWithTag("demo-vote-1").assertTextEquals("1")
        onNode(hasText("Communities") and hasClickAction()).performClick()
        onNode(hasText("Feed") and hasClickAction()).performClick()
        onNodeWithTag("demo-vote-1").assertTextEquals("1")
    }

    @Test
    fun `notifications close without switching the current social section`() = runAndroidComposeUiTest {
        val presenter = AntintwrkShellPresenter()
        setSafeContent { ElementTheme { AntintwrkShellView(presenter.present()) } }
        onNode(hasText("Profile") and hasClickAction()).performClick()
        onNodeWithContentDescription("Notifications").performClick()
        onNodeWithText("Notifications will appear here once the server is connected.").assertIsDisplayed()
        onNodeWithText("Close").performClick()
        onNode(hasText("Profile") and hasClickAction()).assertIsSelected()
    }
}
