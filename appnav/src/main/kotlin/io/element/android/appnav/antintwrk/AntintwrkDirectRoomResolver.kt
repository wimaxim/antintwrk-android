// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.RoomMembershipState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The SDK remains authoritative; a stale m.direct entry must not resurrect a departed room. */
internal class AntintwrkDirectRoomResolver(private val client: MatrixClient) {
    private val mutex = Mutex()
    suspend fun resolve(recipient: UserId): RoomId = mutex.withLock {
        require(recipient.value != client.sessionId.value)
        val existing = client.findDM(recipient).getOrThrow()
        if (existing != null) {
            val room = client.getJoinedRoom(existing) ?: error("Room has not finished syncing")
            try {
                check(room.roomInfoFlow.value.activeMembersCount <= 2) { "Personal route cannot open a group room" }
                check(room.getUpdatedIsEncrypted().getOrThrow()) { "Managed personal chat must be encrypted" }
                when (room.getUpdatedMember(recipient).getOrThrow().membership) {
                    RoomMembershipState.JOIN, RoomMembershipState.INVITE -> {
                        val members = room.getMembers(limit = 3).getOrThrow()
                        check(
                            members.size == 2 && members.map { it.userId }.toSet() == setOf(UserId(client.sessionId.value), recipient) &&
                            members.all { it.membership.isActive() }
                        ) { "Personal route cannot expose a group conversation" }
                        return@withLock existing
                    }
                    RoomMembershipState.BAN -> error("Recipient is banned from this room")
                    RoomMembershipState.LEAVE, RoomMembershipState.KNOCK -> Unit
                }
            } finally {
                room.close()
            }
        }
        client.createDM(recipient, isEncrypted = true).getOrThrow()
    }
}
