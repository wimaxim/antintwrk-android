// SPDX-License-Identifier: AGPL-3.0-only
@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package io.element.android.features.createroom.impl.addpeople

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import com.google.common.truth.Truth.assertThat
import io.element.android.features.invitepeople.api.InvitePeopleEvent
import io.element.android.features.invitepeople.api.InvitePeopleState
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.tests.testutils.InstrumentationStringProvider
import io.element.android.tests.testutils.robolectric.RobolectricTest
import io.element.android.tests.testutils.setSafeContent
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

class RequiredGroupPeopleTest : RobolectricTest() {
    @Test
    fun `selection has no Skip and requires two distinct people before continuing`() = runAndroidComposeUiTest {
        val users = mutableStateOf(persistentListOf<MatrixUser>())
        var accepted: List<MatrixUser>? = null
        setSafeContent {
            val state = object : InvitePeopleState {
                override val canInvite = true
                override val selectedUsers = users.value
                override val isSearchActive = false
                override val sendInvitesAction = AsyncAction.Success(Unit)
                override val createRoomFromDmAction: AsyncAction<RoomId> = AsyncAction.Uninitialized
                override val eventSink: (InvitePeopleEvent) -> Unit = { error("Selection must not send invites") }
            }
            AddPeopleView(
                state = state,
                onFinish = { error("Mandatory selection must not skip or finish early") },
                onPeopleSelected = { accepted = it },
                invitePeopleView = {},
            )
        }
        val strings = InstrumentationStringProvider()
        onNodeWithText(strings.getString(CommonStrings.action_skip)).assertDoesNotExist()
        val next = onNodeWithText(strings.getString(CommonStrings.action_next))
        next.assertIsNotEnabled()
        val first = MatrixUser(UserId("@one:antintwrk.com"))
        runOnIdle { users.value = persistentListOf(first) }
        next.assertIsNotEnabled()
        runOnIdle { users.value = persistentListOf(first, first.copy(displayName = "Updated")) }
        next.assertIsNotEnabled()
        val second = MatrixUser(UserId("@two:antintwrk.com"))
        runOnIdle { users.value = persistentListOf(first, second) }
        next.assertIsEnabled().performClick()
        assertThat(accepted).containsExactly(first, second).inOrder()
    }
}
