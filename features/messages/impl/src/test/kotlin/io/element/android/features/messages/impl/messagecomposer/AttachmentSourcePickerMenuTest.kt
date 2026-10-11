/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
@file:OptIn(ExperimentalTestApi::class)

package io.element.android.features.messages.impl.messagecomposer

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import com.google.common.truth.Truth.assertThat
import io.element.android.features.messages.impl.R
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test

class AttachmentSourcePickerMenuTest : RobolectricTest() {
    @Test
    fun `direct chat omits poll while keeping gallery`() = runAndroidComposeUiTest {
        setContent {
            ElementPreview {
                AttachmentSourcePickerMenu(aMessageComposerState().copy(canCreatePoll = false), {}, {}, false)
            }
        }
        val host: ComponentActivity = requireNotNull(activity)
        onNodeWithText(host.getString(R.string.screen_room_attachment_source_poll)).assertDoesNotExist()
        onNodeWithText(host.getString(R.string.screen_room_attachment_source_gallery)).assertExists()
    }

    @Test
    fun `group poll retains action and navigation`() = runAndroidComposeUiTest {
        val events = mutableListOf<MessageComposerEvent>()
        var opened = false
        setContent {
            ElementPreview {
                AttachmentSourcePickerMenu(aMessageComposerState(eventSink = events::add), {}, { opened = true }, false)
            }
        }
        val host: ComponentActivity = requireNotNull(activity)
        onNodeWithText(host.getString(R.string.screen_room_attachment_source_poll)).performClick()
        assertThat(events).containsExactly(MessageComposerEvent.PickAttachmentSource.Poll)
        assertThat(opened).isTrue()
    }
}
