// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.room.CurrentUserMembership
import io.element.android.libraries.matrix.api.room.RoomNotificationMode
import io.element.android.libraries.matrix.test.room.aRoomSummary
import org.junit.Test

class AntintwrkUnreadMessagesTest {
    @Test
    fun `counts messages including muted rooms instead of notification or room counts`() {
        val rooms = listOf(
            aRoomSummary(roomId = RoomId("!one:example.org"), numUnreadMessages = 7, notificationCount = 1),
            aRoomSummary(roomId = RoomId("!two:example.org"), numUnreadMessages = 3, userDefinedNotificationMode = RoomNotificationMode.MUTE),
        )
        assertThat(antintwrkUnreadMessages(rooms)).isEqualTo(10)
    }

    @Test
    fun `invitations spaces departed rooms and duplicate summaries do not inflate count`() {
        val room = aRoomSummary(numUnreadMessages = 2)
        assertThat(antintwrkUnreadMessages(listOf(
            room,
            room,
            aRoomSummary(roomId = RoomId("!invite:example.org"), numUnreadMessages = 9, currentUserMembership = CurrentUserMembership.INVITED),
            aRoomSummary(roomId = RoomId("!space:example.org"), numUnreadMessages = 9, isSpace = true),
            aRoomSummary(roomId = RoomId("!left:example.org"), numUnreadMessages = 9, currentUserMembership = CurrentUserMembership.LEFT),
        ))).isEqualTo(2)
        assertThat(antintwrkUnreadMessages(emptyList())).isEqualTo(0)
    }

    @Test
    fun `malformed counts cannot overflow or become negative`() {
        assertThat(antintwrkUnreadMessages(listOf(
            aRoomSummary(roomId = RoomId("!one:example.org"), numUnreadMessages = Long.MAX_VALUE),
            aRoomSummary(roomId = RoomId("!two:example.org"), numUnreadMessages = 2),
            aRoomSummary(roomId = RoomId("!three:example.org"), numUnreadMessages = -3),
        ))).isEqualTo(Long.MAX_VALUE)
    }
}
