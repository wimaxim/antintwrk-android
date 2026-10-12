// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.matrix.api.encryption.AntintwrkRecoveryStatus
import io.element.android.libraries.matrix.api.verification.SessionVerifiedStatus
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Rule
import org.junit.Test

class AntintwrkMessengerSessionContentTest : RobolectricTest() {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `automatic preparation leaves content available and does not recreate it on verification`() {
        val status = mutableStateOf<SessionVerifiedStatus>(SessionVerifiedStatus.Unknown)
        compose.setContent {
            ElementPreview {
                AntintwrkMessengerSessionContent(status.value, AntintwrkRecoveryStatus.Phase.Working, {}, Modifier.fillMaxSize()) {
                    var draft by remember { mutableStateOf("draft") }
                    TextButton(draft, onClick = { draft = "edited draft" })
                }
            }
        }
        compose.onNodeWithText("Preparing your messages…").assertIsDisplayed()
        compose.onNodeWithText("draft").performClick()
        compose.runOnIdle { status.value = SessionVerifiedStatus.Verified }
        compose.onNodeWithText("Preparing your messages…").assertDoesNotExist()
        compose.onNodeWithText("edited draft").assertIsDisplayed()
    }

    @Test
    fun `failed automatic restore gives an inline retry without removing chats`() {
        var retries = 0
        compose.setContent {
            ElementPreview {
                AntintwrkMessengerSessionContent(
                    SessionVerifiedStatus.NotVerified,
                    AntintwrkRecoveryStatus.Phase.Failed,
                    { retries++ },
                    Modifier.fillMaxSize()
                ) { Text("Chats remain mounted") }
            }
        }
        compose.onNodeWithText("Chats remain mounted").assertIsDisplayed()
        compose.onNodeWithText("Retry").performClick()
        assertThat(retries).isEqualTo(1)
    }

    @Test
    fun `vault failure does not obscure an already verified device`() {
        compose.setContent {
            ElementPreview {
                AntintwrkMessengerSessionContent(SessionVerifiedStatus.Verified, AntintwrkRecoveryStatus.Phase.Failed, {}, Modifier.fillMaxSize()) {
                    Text("Chats")
                }
            }
        }
        compose.onNodeWithText("Chats").assertIsDisplayed()
        compose.onNodeWithText("Retry").assertDoesNotExist()
        compose.onNodeWithText("Preparing your messages…").assertDoesNotExist()
    }
}
