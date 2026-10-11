/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
@file:OptIn(ExperimentalCoroutinesApi::class)

package io.element.android.libraries.matrix.impl.timeline

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.room.JoinedRoom
import io.element.android.libraries.matrix.api.timeline.MatrixTimelineItem
import io.element.android.libraries.matrix.api.timeline.Timeline
import io.element.android.libraries.matrix.api.timeline.item.virtual.VirtualTimelineItem
import io.element.android.libraries.matrix.impl.fixtures.factories.aRustEventTimelineItem
import io.element.android.libraries.matrix.impl.fixtures.factories.aRustTimelineItemContentMsgLike
import io.element.android.libraries.matrix.impl.fixtures.fakes.FakeFfiRoomListService
import io.element.android.libraries.matrix.impl.fixtures.fakes.FakeFfiTimeline
import io.element.android.libraries.matrix.impl.fixtures.fakes.FakeFfiTimelineItem
import io.element.android.libraries.matrix.impl.room.RoomContentForwarder
import io.element.android.libraries.matrix.test.room.FakeJoinedRoom
import io.element.android.libraries.matrix.test.room.aRoomInfo
import io.element.android.services.toolbox.api.systemclock.SystemClock
import io.element.android.services.toolbox.test.systemclock.A_FAKE_TIMESTAMP
import io.element.android.services.toolbox.test.systemclock.FakeSystemClock
import io.element.android.tests.testutils.testCoroutineDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.matrix.rustcomponents.sdk.EncryptedMessage
import org.matrix.rustcomponents.sdk.MsgLikeKind
import org.matrix.rustcomponents.sdk.TimelineDiff
import org.matrix.rustcomponents.sdk.TimelineItemContent
import uniffi.matrix_sdk.PaginationStatus
import uniffi.matrix_sdk_crypto.UtdCause
import org.matrix.rustcomponents.sdk.Timeline as InnerTimeline

class RustTimelineTest {
    @Test
    fun `late backup is actively fetched and recovery cancels when the message decrypts`() = runTest {
        assumeTrue(io.element.android.libraries.matrix.api.BuildConfig.ANTINTWRK_NATIVE_AUTH)
        val inner = FakeFfiTimeline()
        val fetches = mutableListOf<EventId>()
        val room = FakeJoinedRoom().apply { givenRoomInfo(aRoomInfo()) }
        val sut = createRustTimeline(inner = inner, joinedRoom = room, fetchMissingEvent = { fetches += it })
        backgroundScope.launch { sut.timelineItems.collect() }
        runCurrent()
        val normal = aRustTimelineItemContentMsgLike() as TimelineItemContent.MsgLike
        val encrypted = normal.copy(content = normal.content.copy(
            kind = MsgLikeKind.UnableToDecrypt(EncryptedMessage.MegolmV1AesSha2("missing-session", UtdCause.UNKNOWN))
        ))
        inner.emitDiff(listOf(TimelineDiff.Reset(listOf(FakeFfiTimelineItem(aRustEventTimelineItem(content = encrypted))))))
        runCurrent()
        assertThat(fetches).hasSize(1)
        assertThat(inner.decryptionRetries).containsExactly(listOf("missing-session"))
        advanceTimeBy(180_000)
        runCurrent()
        assertThat(fetches.size).isGreaterThan(18)
        inner.emitDiff(listOf(TimelineDiff.Reset(listOf(FakeFfiTimelineItem(aRustEventTimelineItem())))))
        runCurrent()
        val completed = fetches.size
        advanceTimeBy(60_000)
        runCurrent()
        assertThat(fetches).hasSize(completed)
    }

    @Test
    fun `ensure that the timeline emits new loading item when pagination does not bring new events`() = runTest {
        val inner = FakeFfiTimeline()
        val systemClock = FakeSystemClock()
        val sut = createRustTimeline(
            inner = inner,
            systemClock = systemClock,
        )
        sut.timelineItems.test {
            // Give time for the listener to be set
            runCurrent()
            inner.emitDiff(
                listOf(
                    TimelineDiff.Reset(emptyList())
                )
            )
            with(awaitItem()) {
                assertThat(size).isEqualTo(2)
                // The loading
                assertThat((get(0) as MatrixTimelineItem.Virtual).virtual).isEqualTo(
                    VirtualTimelineItem.LoadingIndicator(
                        direction = Timeline.PaginationDirection.BACKWARDS,
                        timestamp = A_FAKE_TIMESTAMP,
                    )
                )
                // Typing notification
                assertThat((get(1) as MatrixTimelineItem.Virtual).virtual).isEqualTo(VirtualTimelineItem.TypingNotification)
            }
            systemClock.epochMillisResult = A_FAKE_TIMESTAMP + 1
            // Start pagination
            sut.paginate(Timeline.PaginationDirection.BACKWARDS)
            // Simulate SDK starting pagination
            inner.emitPaginationStatus(PaginationStatus.Paginating)
            // No new events received
            // Simulate SDK stopping pagination, more event to load
            inner.emitPaginationStatus(PaginationStatus.Idle(hitTimelineStart = false))
            // expect an item to be emitted, with an updated timestamp
            with(awaitItem()) {
                assertThat(size).isEqualTo(2)
                // The loading
                assertThat((get(0) as MatrixTimelineItem.Virtual).virtual).isEqualTo(
                    VirtualTimelineItem.LoadingIndicator(
                        direction = Timeline.PaginationDirection.BACKWARDS,
                        timestamp = A_FAKE_TIMESTAMP + 1,
                    )
                )
                // Typing notification
                assertThat((get(1) as MatrixTimelineItem.Virtual).virtual).isEqualTo(VirtualTimelineItem.TypingNotification)
            }
        }
    }
}

private fun TestScope.createRustTimeline(
    inner: InnerTimeline,
    mode: Timeline.Mode = Timeline.Mode.Live,
    systemClock: SystemClock = FakeSystemClock(),
    joinedRoom: JoinedRoom = FakeJoinedRoom().apply { givenRoomInfo(aRoomInfo()) },
    coroutineScope: CoroutineScope = backgroundScope,
    dispatcher: CoroutineDispatcher = testCoroutineDispatchers().io,
    roomContentForwarder: RoomContentForwarder = RoomContentForwarder(FakeFfiRoomListService()),
    fetchMissingEvent: suspend (EventId) -> Unit = {},
): RustTimeline {
    return RustTimeline(
        inner = inner,
        mode = mode,
        systemClock = systemClock,
        joinedRoom = joinedRoom,
        coroutineScope = coroutineScope,
        dispatcher = dispatcher,
        roomContentForwarder = roomContentForwarder,
        fetchMissingEvent = fetchMissingEvent,
    )
}
