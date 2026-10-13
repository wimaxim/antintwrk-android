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
import androidx.compose.ui.test.performScrollTo
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
    fun `social verification remains accessible while messenger settings are unavailable`() = runAndroidComposeUiTest {
        var destination = ""
        setSafeContent { ElementTheme { AntintwrkAppSettings({}, { destination = it }, {}, messengerSettingsReady = false) } }
        onNodeWithText("Account verification").performScrollTo().assertIsDisplayed().performClick()
        runOnIdle { assertThat(destination).isEqualTo("verification") }
    }

    @Test
    @Config(qualifiers = "w320dp-h480dp")
    fun `information and participation links can be reached on a small settings screen`() = runAndroidComposeUiTest {
        var destination = ""
        setSafeContent { ElementTheme { AntintwrkAppSettings({}, { destination = it }, {}) } }
        for ((label, route) in listOf(
            "Friend requests" to "friend-requests",
            "My referrals" to "my-referrals",
            "HERO WORLD" to "native-game",
            "Contacts" to "contacts",
            "About antintwrk" to "about",
            "Manifesto" to "hero/maxim-novichkov/1755",
            "Community rules" to "community",
            "Safety" to "safety",
            "Copyright / DMCA" to "dmca",
        )) {
            onNodeWithText(label).performScrollTo().assertIsDisplayed().performClick()
            runOnIdle { assertThat(destination).isEqualTo(route) }
        }
        onNodeWithText("Sign out").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `compact menu opens shared settings instead of listing account actions`() = runAndroidComposeUiTest {
        var route = ""
        setSafeContent { ElementTheme { AntintwrkSocialMenu(onRoute = { route = it }) } }
        onNodeWithContentDescription("More").performClick()
        onNodeWithText("Settings").assertIsDisplayed().performClick()
        runOnIdle { assertThat(route).isEqualTo("native-settings") }
        onNodeWithText("Sign out").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w411dp-h900dp")
    fun `shared settings language chooser contains all social languages`() = runAndroidComposeUiTest {
        setSafeContent { ElementTheme { AntintwrkAppSettings({}, {}, {}) } }
        onNodeWithText("Language").performClick()
        mainClock.advanceTimeBy(500)
        waitForIdle()
        onNodeWithText("Use device language", substring = true).assertExists()
        onNodeWithText("Русский").assertExists()
        onNodeWithText("Українська").assertExists()
        onNodeWithText("English").assertExists()
        runOnIdle { assertThat(AntintwrkAppLocale.languages.size).isEqualTo(9) }
    }

    @Test
    fun `messenger initializes behind social and stays composed across mode changes`() = runAndroidComposeUiTest {
        val presenter = AntintwrkShellPresenter()
        var created = 0
        var disposed = 0
        setSafeContent {
            ElementTheme {
                AntintwrkShellView(presenter.present(), messengerContent = {
                    androidx.compose.runtime.DisposableEffect(Unit) {
                        created++
                        onDispose { disposed++ }
                    }
                    io.element.android.libraries.designsystem.theme.components.Text("Prepared messenger")
                })
            }
        }
        runOnIdle { assertThat(created).isEqualTo(1) }
        onNodeWithText("Prepared messenger").assertDoesNotExist()
        onNode(hasText("Messenger") and hasClickAction()).performClick()
        onNodeWithText("Prepared messenger").assertIsDisplayed()
        onNode(hasText("Social") and hasClickAction()).performClick()
        onNode(hasText("Messenger") and hasClickAction()).performClick()
        runOnIdle {
            assertThat(created).isEqualTo(1)
            assertThat(disposed).isEqualTo(0)
        }
    }

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
