// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.prepareDirectConversation
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The SDK remains authoritative; a stale m.direct entry must not resurrect a departed room. */
internal class AntintwrkDirectRoomResolver(private val client: MatrixClient) {
    private val mutex = Mutex()
    suspend fun resolve(recipient: UserId): RoomId = mutex.withLock {
        require(recipient.value != client.sessionId.value)
        val existing = client.findDM(recipient).getOrThrow()
        if (existing != null && client.prepareDirectConversation(existing, recipient)) return@withLock existing
        client.createDM(recipient, isEncrypted = true).getOrThrow()
    }
}
