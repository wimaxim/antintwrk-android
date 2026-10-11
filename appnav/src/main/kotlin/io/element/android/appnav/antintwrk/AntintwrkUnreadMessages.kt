// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import io.element.android.libraries.matrix.api.room.CurrentUserMembership
import io.element.android.libraries.matrix.api.roomlist.RoomSummary

/** Unread messages plus one per pending chat request, independent of mute settings; spaces are excluded. */
internal fun antintwrkUnreadMessages(rooms: List<RoomSummary>): Long = rooms.distinctBy { it.roomId }
    .filter { !it.info.isSpace }
    .fold(0L) { total, room ->
        val count = when (room.info.currentUserMembership) {
            CurrentUserMembership.INVITED -> 1L
            CurrentUserMembership.JOINED -> room.info.numUnreadMessages.coerceAtLeast(0)
            else -> 0L
        }
        if (Long.MAX_VALUE - total < count) Long.MAX_VALUE else total + count
    }
