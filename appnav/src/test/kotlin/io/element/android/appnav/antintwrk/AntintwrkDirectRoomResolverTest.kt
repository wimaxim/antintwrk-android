// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.RoomMembershipState
import io.element.android.libraries.matrix.test.A_ROOM_ID
import io.element.android.libraries.matrix.test.A_USER_ID_2
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.libraries.matrix.test.room.FakeBaseRoom
import io.element.android.libraries.matrix.test.room.FakeJoinedRoom
import io.element.android.libraries.matrix.test.room.aRoomMember
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AntintwrkDirectRoomResolverTest {
    @Test
    fun `joined and invited recipients reuse encrypted history`() = runTest {
        for (membership in listOf(RoomMembershipState.JOIN, RoomMembershipState.INVITE)) {
            val client = FakeMatrixClient()
            val base = FakeBaseRoom(
                getUpdatedMemberResult = { Result.success(aRoomMember(userId = it, membership = membership)) },
                getMembersResult = { Result.success(listOf(aRoomMember(UserId(client.sessionId.value)), aRoomMember(A_USER_ID_2, membership = membership))) },
            )
            base.givenRoomInfo(base.info().copy(isEncrypted = true, activeMembersCount = 2))
            client.givenFindDmResult(Result.success(A_ROOM_ID))
            client.givenGetRoomResult(A_ROOM_ID, FakeJoinedRoom(baseRoom = base))
            client.createDmResult = { _, _ -> error("Must reuse existing conversation") }
            assertThat(AntintwrkDirectRoomResolver(client).resolve(A_USER_ID_2)).isEqualTo(A_ROOM_ID)
        }
    }

    @Test
    fun `departed recipient starts a new encrypted conversation`() = runTest {
        val client = FakeMatrixClient()
        val base = FakeBaseRoom(getUpdatedMemberResult = { Result.success(aRoomMember(userId = it, membership = RoomMembershipState.LEAVE)) })
        base.givenRoomInfo(base.info().copy(isEncrypted = true, activeMembersCount = 1))
        client.givenFindDmResult(Result.success(A_ROOM_ID))
        client.givenGetRoomResult(A_ROOM_ID, FakeJoinedRoom(baseRoom = base))
        val replacement = RoomId("!replacement:matrix.antimsg.com")
        var created = false
        client.createDmResult = { user, encrypted ->
            assertThat(user).isEqualTo(A_USER_ID_2)
            assertThat(encrypted).isTrue()
            created = true
            Result.success(replacement)
        }
        assertThat(AntintwrkDirectRoomResolver(client).resolve(A_USER_ID_2)).isEqualTo(replacement)
        assertThat(created).isTrue()
    }

    @Test
    fun `ban outage unencrypted and group rooms cannot create replacements`() = runTest {
        for (scenario in 0..3) {
            val client = FakeMatrixClient()
            val base = FakeBaseRoom(getUpdatedMemberResult = {
                if (scenario == 1) {
                    Result.failure(IllegalStateException("synthetic outage"))
                } else {
                    Result.success(aRoomMember(userId = it, membership = RoomMembershipState.BAN))
                }
            })
            base.givenRoomInfo(base.info().copy(isEncrypted = scenario != 2, activeMembersCount = if (scenario == 3) 3 else 2))
            client.givenFindDmResult(Result.success(A_ROOM_ID))
            client.givenGetRoomResult(A_ROOM_ID, FakeJoinedRoom(baseRoom = base))
            var created = false
            client.createDmResult = { _, _ ->
                created = true
                Result.success(A_ROOM_ID)
            }
            assertThat(runCatching { AntintwrkDirectRoomResolver(client).resolve(A_USER_ID_2) }.isFailure).isTrue()
            assertThat(created).isFalse()
        }
    }

    @Test
    fun `concurrent opening does not create two rooms in one client flow`() = runTest {
        val client = FakeMatrixClient()
        val base = FakeBaseRoom(
            getUpdatedMemberResult = { Result.success(aRoomMember(userId = it, membership = RoomMembershipState.INVITE)) },
            getMembersResult = { Result.success(listOf(aRoomMember(UserId(client.sessionId.value)), aRoomMember(A_USER_ID_2))) },
        )
        base.givenRoomInfo(base.info().copy(isEncrypted = true, activeMembersCount = 2))
        client.givenFindDmResult(Result.success(null))
        var created = 0
        client.createDmResult = { _, encrypted ->
            assertThat(encrypted).isTrue()
            created++
            client.givenGetRoomResult(A_ROOM_ID, FakeJoinedRoom(baseRoom = base))
            client.givenFindDmResult(Result.success(A_ROOM_ID))
            Result.success(A_ROOM_ID)
        }
        val resolver = AntintwrkDirectRoomResolver(client)
        val first = async { resolver.resolve(A_USER_ID_2) }
        val second = async { resolver.resolve(A_USER_ID_2) }
        assertThat(first.await()).isEqualTo(A_ROOM_ID)
        assertThat(second.await()).isEqualTo(A_ROOM_ID)
        assertThat(created).isEqualTo(1)
    }

    @Test
    fun `fresh members reject a group even if cached room count is still two`() = runTest {
        val client = FakeMatrixClient()
        val base = FakeBaseRoom(
            getUpdatedMemberResult = { Result.success(aRoomMember(userId = it)) },
            getMembersResult = {
                Result.success(listOf(aRoomMember(UserId(client.sessionId.value)), aRoomMember(A_USER_ID_2), aRoomMember(UserId("@third:matrix.antimsg.com"))))
            },
        )
        base.givenRoomInfo(base.info().copy(isEncrypted = true, activeMembersCount = 2))
        client.givenFindDmResult(Result.success(A_ROOM_ID))
        client.givenGetRoomResult(A_ROOM_ID, FakeJoinedRoom(baseRoom = base))
        client.createDmResult = { _, _ -> error("Must not replace a group route") }
        assertThat(runCatching { AntintwrkDirectRoomResolver(client).resolve(A_USER_ID_2) }.isFailure).isTrue()
    }
}
