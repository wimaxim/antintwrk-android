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
    fun `pending requests count once alongside messages and exclude spaces departed rooms and duplicates`() {
        val room = aRoomSummary(numUnreadMessages = 2)
        assertThat(antintwrkUnreadMessages(listOf(
            room,
            room,
            aRoomSummary(roomId = RoomId("!invite:example.org"), numUnreadMessages = 9, currentUserMembership = CurrentUserMembership.INVITED),
            aRoomSummary(roomId = RoomId("!space:example.org"), numUnreadMessages = 9, isSpace = true),
            aRoomSummary(roomId = RoomId("!left:example.org"), numUnreadMessages = 9, currentUserMembership = CurrentUserMembership.LEFT),
        ))).isEqualTo(3)
        assertThat(antintwrkUnreadMessages(emptyList())).isEqualTo(0)
    }

    @Test
    fun `requests with no message count still produce a badge`() {
        val invite = aRoomSummary(currentUserMembership = CurrentUserMembership.INVITED, numUnreadMessages = 0)
        assertThat(antintwrkUnreadMessages(listOf(invite, invite))).isEqualTo(1)
    }

    @Test
    fun `accepting a request replaces its contribution with actual unread messages and declining removes it`() {
        val roomId = RoomId("!request:example.org")
        val existing = aRoomSummary(roomId = RoomId("!existing:example.org"), numUnreadMessages = 4)
        fun count(membership: CurrentUserMembership, unread: Long = 0) = antintwrkUnreadMessages(listOf(
            existing,
            aRoomSummary(roomId = roomId, currentUserMembership = membership, numUnreadMessages = unread),
        ))
        assertThat(count(CurrentUserMembership.INVITED)).isEqualTo(5)
        assertThat(count(CurrentUserMembership.JOINED, unread = 3)).isEqualTo(7)
        assertThat(count(CurrentUserMembership.JOINED)).isEqualTo(4)
        assertThat(count(CurrentUserMembership.LEFT)).isEqualTo(4)
        assertThat(antintwrkUnreadMessages(listOf(existing))).isEqualTo(4)
    }

    @Test
    fun `space invitations do not produce a chat request badge`() {
        assertThat(antintwrkUnreadMessages(listOf(
            aRoomSummary(currentUserMembership = CurrentUserMembership.INVITED, isSpace = true),
        ))).isEqualTo(0)
    }

    @Test
    fun `malformed counts cannot overflow or become negative`() {
        assertThat(antintwrkUnreadMessages(listOf(
            aRoomSummary(roomId = RoomId("!one:example.org"), numUnreadMessages = Long.MAX_VALUE),
            aRoomSummary(roomId = RoomId("!two:example.org"), numUnreadMessages = 2),
            aRoomSummary(roomId = RoomId("!three:example.org"), numUnreadMessages = -3),
            aRoomSummary(roomId = RoomId("!invite:example.org"), currentUserMembership = CurrentUserMembership.INVITED),
        ))).isEqualTo(Long.MAX_VALUE)
    }
}
