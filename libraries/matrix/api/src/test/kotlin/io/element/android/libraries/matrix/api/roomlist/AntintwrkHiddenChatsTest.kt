// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.libraries.matrix.api.roomlist

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.paths.SessionPaths
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.libraries.matrix.test.room.aRemoteLatestEvent
import io.element.android.libraries.matrix.test.room.aRoomSummary
import io.element.android.libraries.matrix.test.roomlist.FakeDynamicRoomList
import io.element.android.libraries.matrix.test.roomlist.FakeRoomListService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AntintwrkHiddenChatsTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun `hide persists only this account and explicit reopen preserves membership`() = runTest {
        val summary = aRoomSummary(isDirect = true, activeMembersCount = 2, latestEvent = aRemoteLatestEvent(timestamp = 100))
        val dm = summary.copy(info = summary.info.copy(isDm = true))
        val rooms = FakeDynamicRoomList(summaries = MutableStateFlow(listOf(dm)))
        val paths = SessionPaths(temporary.newFolder(), temporary.newFolder())
        val client = FakeMatrixClient(sessionPaths = paths, roomListService = FakeRoomListService(allRooms = rooms))
        val visibility = AntintwrkHiddenChats.forClient(client)
        assertThat(visibility.hide(summary.roomId).isSuccess).isTrue()
        assertThat(visibility.state.value.keys).containsExactly(summary.roomId.value)
        val restored = AntintwrkHiddenChats.forClient(FakeMatrixClient(sessionPaths = paths))
        assertThat(restored.state.value.keys).containsExactly(summary.roomId.value)
        val other = AntintwrkHiddenChats.forClient(FakeMatrixClient(sessionPaths = SessionPaths(temporary.newFolder(), temporary.newFolder())))
        assertThat(other.state.value).isEmpty()
        visibility.reveal(summary.roomId)
        assertThat(visibility.state.value).isEmpty()
        assertThat(rooms.summaries.replayCache.last().single().info.currentUserMembership).isEqualTo(summary.info.currentUserMembership)
    }

    @Test fun `old events and own activity do not restore but a new received message restores automatically`() = runTest {
        val summary = aRoomSummary(isDirect = true, activeMembersCount = 2, latestEvent = aRemoteLatestEvent(timestamp = 100))
        val dm = summary.copy(info = summary.info.copy(isDm = true))
        val source = MutableStateFlow(listOf(dm))
        val rooms = FakeDynamicRoomList(summaries = source)
        val client = FakeMatrixClient(
            sessionPaths = SessionPaths(temporary.newFolder(), temporary.newFolder()),
            roomListService = FakeRoomListService(allRooms = rooms),
        )
        val visibility = AntintwrkHiddenChats.forClient(client)
        visibility.hide(summary.roomId).getOrThrow()
        assertThat(AntintwrkHiddenChats.shouldRestore(dm, 100)).isFalse()
        assertThat(AntintwrkHiddenChats.shouldRestore(dm.copy(latestEvent = aRemoteLatestEvent(timestamp = 90)), 100)).isFalse()
        assertThat(AntintwrkHiddenChats.shouldRestore(dm.copy(latestEvent = aRemoteLatestEvent(timestamp = 101, isOwn = true)), 100)).isFalse()
        visibility.observe(kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.Dispatchers.Default))
        source.value = listOf(dm.copy(latestEvent = aRemoteLatestEvent(timestamp = 101)))
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            withTimeout(5_000) { visibility.state.first { it.isEmpty() } }
        }
        assertThat(AntintwrkHiddenChats.forClient(FakeMatrixClient(sessionPaths = client.sessionPaths)).state.value).isEmpty()
    }

    @Test fun `groups are never silently hidden`() = runTest {
        val rooms = FakeDynamicRoomList(summaries = MutableStateFlow(listOf(aRoomSummary(isDirect = false, activeMembersCount = 3))))
        val client = FakeMatrixClient(
            sessionPaths = SessionPaths(temporary.newFolder(), temporary.newFolder()),
            roomListService = FakeRoomListService(allRooms = rooms),
        )
        val visibility = AntintwrkHiddenChats.forClient(client)
        assertThat(visibility.hide(rooms.summaries.replayCache.last().single().roomId).isFailure).isTrue()
        assertThat(visibility.state.value).isEmpty()
    }
}
