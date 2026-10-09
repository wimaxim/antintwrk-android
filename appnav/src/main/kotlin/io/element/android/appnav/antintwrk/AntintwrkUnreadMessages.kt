// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import io.element.android.libraries.matrix.api.room.CurrentUserMembership
import io.element.android.libraries.matrix.api.roomlist.RoomSummary

/** Message count, independent of mute settings; invitations and spaces are separate. */
internal fun antintwrkUnreadMessages(rooms: List<RoomSummary>): Long = rooms.distinctBy { it.roomId }
    .filter { it.info.currentUserMembership == CurrentUserMembership.JOINED && !it.info.isSpace }
    .fold(0L) { total, room ->
        val count = room.info.numUnreadMessages.coerceAtLeast(0)
        if (Long.MAX_VALUE - total < count) Long.MAX_VALUE else total + count
    }
