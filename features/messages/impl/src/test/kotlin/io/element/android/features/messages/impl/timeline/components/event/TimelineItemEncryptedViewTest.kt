// SPDX-License-Identifier: AGPL-3.0-only
@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
package io.element.android.features.messages.impl.timeline.components.event

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemEncryptedContent
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.BuildConfig
import io.element.android.libraries.matrix.api.timeline.item.event.UnableToDecryptContent
import io.element.android.libraries.matrix.api.timeline.item.event.UtdCause
import io.element.android.tests.testutils.robolectric.RobolectricTest
import io.element.android.tests.testutils.setSafeContent
import org.junit.Test

class TimelineItemEncryptedViewTest : RobolectricTest() {
    private val progress = SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)
    @Test
    fun `history waiting after joining uses the managed recovery indicator`() = runAndroidComposeUiTest {
        setSafeContent {
            ElementTheme {
                TimelineItemEncryptedView(
                    TimelineItemEncryptedContent(UnableToDecryptContent.Data.MegolmV1AesSha2("fixture-session", UtdCause.SentBeforeWeJoined)),
                    onContentLayoutChange = {},
                )
            }
        }
        onAllNodes(progress).assertCountEquals(if (BuildConfig.ANTINTWRK_NATIVE_AUTH) 1 else 0)
    }
    @Test
    fun `pending keys show accessible progress without hiding readable messages and stop after decryption`() = runAndroidComposeUiTest {
        val pending = mutableStateOf(true)
        setSafeContent {
            ElementTheme {
                Column {
                    Text("Already readable")
                    if (pending.value) {
                        TimelineItemEncryptedView(
                        TimelineItemEncryptedContent(UnableToDecryptContent.Data.MegolmV1AesSha2("fixture-session", UtdCause.Unknown)),
                        onContentLayoutChange = {},
                    )
                    } else {
                        Text("Decrypted message")
                    }
                }
            }
        }
        onNodeWithText("Already readable").assertIsDisplayed()
        onAllNodes(progress).assertCountEquals(if (BuildConfig.ANTINTWRK_NATIVE_AUTH) 1 else 0)
        if (BuildConfig.ANTINTWRK_NATIVE_AUTH) onNodeWithText("Loading message keys…").assertIsDisplayed()
        runOnIdle { pending.value = false }
        onAllNodes(progress).assertCountEquals(0)
        onNodeWithText("Already readable").assertIsDisplayed()
        onNodeWithText("Decrypted message").assertIsDisplayed()
    }

    @Test
    fun `a key explicitly withheld by the sender stays an error instead of false loading`() = runAndroidComposeUiTest {
        setSafeContent {
            ElementTheme {
                TimelineItemEncryptedView(
                    TimelineItemEncryptedContent(UnableToDecryptContent.Data.MegolmV1AesSha2("fixture-session", UtdCause.WithheldBySender)),
                    onContentLayoutChange = {},
                )
            }
        }
        onAllNodes(progress).assertCountEquals(0)
    }

    @Test
    fun `an unknown pending encrypted payload has a loading indicator in the managed client`() = runAndroidComposeUiTest {
        setSafeContent {
            ElementTheme {
                TimelineItemEncryptedView(TimelineItemEncryptedContent(UnableToDecryptContent.Data.Unknown), onContentLayoutChange = {})
            }
        }
        onAllNodes(progress).assertCountEquals(if (BuildConfig.ANTINTWRK_NATIVE_AUTH) 1 else 0)
    }
}
