// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.libraries.matrix.api.room

import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.UserId
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

/** Explicit intent to write to this peer also accepts their matching personal invitation. */
suspend fun MatrixClient.prepareDirectConversation(roomId: RoomId, recipient: UserId): Boolean {
    require(recipient != sessionId)
    val base = getRoom(roomId) ?: run {
        // Global direct account data may arrive before this room's lazy state.
        // Subscribe to the actual room instead of issuing another create request.
        roomListService.subscribeToVisibleRooms(listOf(roomId))
        withTimeout(5_000) {
            var loaded = getRoom(roomId)
            while (loaded == null) {
                delay(100)
                loaded = getRoom(roomId)
            }
            loaded
        }
    }
    val info = try {
        base.info()
    } finally {
        base.close()
    }
    if (info.currentUserMembership == CurrentUserMembership.INVITED) {
        check((info.isDirect || info.isDm) && !info.isSpace && info.activeMembersCount <= 2 &&
            info.isEncrypted == true && info.inviter?.userId == recipient) { "Invitation does not match the personal route" }
        val joined = joinRoom(roomId).getOrThrow()
        check(joined?.currentUserMembership == CurrentUserMembership.JOINED) { "Accepted conversation has not finished syncing" }
    } else {
        if (info.currentUserMembership == CurrentUserMembership.LEFT) return false
        check(info.currentUserMembership == CurrentUserMembership.JOINED) { "Personal conversation is no longer joined" }
    }
    val room = getJoinedRoom(roomId) ?: error("Personal conversation has not finished syncing")
    try {
        check(!room.info().isSpace && room.info().activeMembersCount <= 2) { "Personal route cannot open a group room" }
        check(room.getUpdatedIsEncrypted().getOrThrow()) { "Managed personal chat must be encrypted" }
        when (room.getUpdatedMember(recipient).getOrThrow().membership) {
            RoomMembershipState.JOIN, RoomMembershipState.INVITE -> {
                val members = room.getMembers(limit = 3).getOrThrow()
                check(
                    members.size == 2 && members.map { it.userId }.toSet() == setOf(UserId(sessionId.value), recipient) &&
                    members.all { it.membership.isActive() }
                ) { "Personal route cannot expose a group conversation" }
                return true
            }
            RoomMembershipState.BAN -> error("Recipient is banned from this room")
            RoomMembershipState.LEAVE, RoomMembershipState.KNOCK -> return false
        }
    } finally {
        room.close()
    }
}
