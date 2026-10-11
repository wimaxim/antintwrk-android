/*
 * Copyright (c) 2026 Super David Universe LLC.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package io.element.android.features.messages.impl.topbars

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import io.element.android.features.messages.impl.SharedHistoryIcon
import io.element.android.libraries.designsystem.components.avatar.anAvatarData
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.tests.testutils.robolectric.RobolectricTest
import io.element.android.tests.testutils.setSafeContent
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

class MessagesEncryptionHeaderTest : RobolectricTest() {
    @Test
    fun `confirmed encrypted room shows status and keeps profile click`() = runAndroidComposeUiTest {
        var profileOpened = false
        setSafeContent {
            MessagesViewTopBar(
                roomName = "Alice",
                roomAvatar = anAvatarData(),
                isTombstoned = false,
                heroes = persistentListOf(),
                dmUserIdentityState = null,
                sharedHistoryIcon = SharedHistoryIcon.NONE,
                dmUserStatus = null,
                isEncrypted = true,
                onRoomDetailsClick = { profileOpened = true },
                onBackClick = {},
                menuActions = {},
            )
        }
        onNodeWithText(activity!!.getString(CommonStrings.screen_ant_encrypted_chat), useUnmergedTree = true).assertExists()
        onNodeWithText("Alice", useUnmergedTree = true).performClick()
        org.junit.Assert.assertTrue(profileOpened)
    }

    @Test
    fun `encryption status follows sync and never claims unknown or unencrypted room`() = runAndroidComposeUiTest {
        val encrypted = mutableStateOf<Boolean?>(null)
        setSafeContent {
            MessagesViewTopBar(
                roomName = "Alice",
                roomAvatar = anAvatarData(),
                isTombstoned = false,
                heroes = persistentListOf(),
                dmUserIdentityState = null,
                sharedHistoryIcon = SharedHistoryIcon.NONE,
                dmUserStatus = null,
                isEncrypted = encrypted.value,
                onRoomDetailsClick = {},
                onBackClick = {},
                menuActions = {},
            )
        }
        val label = activity!!.getString(CommonStrings.screen_ant_encrypted_chat)
        onNodeWithText(label, useUnmergedTree = true).assertDoesNotExist()
        runOnIdle { encrypted.value = false }
        onNodeWithText(label, useUnmergedTree = true).assertDoesNotExist()
        runOnIdle { encrypted.value = true }
        onNodeWithText(label, useUnmergedTree = true).assertExists()
    }
}
