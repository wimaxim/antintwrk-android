// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.loggedin

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.sync.SyncState
import io.element.android.libraries.matrix.api.verification.SessionVerifiedStatus
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.libraries.matrix.test.sync.FakeSyncService
import io.element.android.libraries.matrix.test.verification.FakeSessionVerificationService
import io.element.android.tests.testutils.lambda.lambdaRecorder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ManagedSendQueuesTest {
    private val verification = FakeSessionVerificationService()
    private val client = FakeMatrixClient(sessionVerificationService = verification)
    private val sync = FakeSyncService(initialSyncState = SyncState.Running)
    private val disabled = MutableSharedFlow<RoomId>(extraBufferCapacity = 1)
    private val calls = mutableListOf<Boolean>()
    private val sut = SendQueues(client, sync)

    init {
        client.sendQueueDisabledFlow = disabled
        client.setAllSendQueuesEnabledLambda = lambdaRecorder { enabled: Boolean ->
            calls.add(enabled)
            Unit
        }
    }

    @Test
    fun `fresh login waits for verification even when sync and retry events are ready`() = runTest {
        sut.launchIn(backgroundScope, managed = true)
        runCurrent()
        assertThat(calls).containsExactly(false)
        disabled.emit(RoomId("!room:example.org"))
        advanceTimeBy(10_000)
        runCurrent()
        assertThat(calls).doesNotContain(true)
        verification.emitVerifiedStatus(SessionVerifiedStatus.NotVerified)
        runCurrent()
        advanceTimeBy(10_000)
        runCurrent()
        assertThat(calls).doesNotContain(true)
    }

    @Test
    fun `automatic recovery resumes sending without another network or queue event`() = runTest {
        sut.launchIn(backgroundScope, managed = true)
        runCurrent()
        verification.emitVerifiedStatus(SessionVerifiedStatus.Verified)
        runCurrent()
        advanceTimeBy(SEND_QUEUES_RETRY_DELAY_MILLIS - 1)
        runCurrent()
        assertThat(calls).containsExactly(false)
        advanceTimeBy(1)
        runCurrent()
        assertThat(calls).containsExactly(false, true).inOrder()
    }

    @Test
    fun `verified device remains paused while offline and resumes on sync alone`() = runTest {
        sync.emitSyncState(SyncState.Offline)
        verification.emitVerifiedStatus(SessionVerifiedStatus.Verified)
        sut.launchIn(backgroundScope, managed = true)
        runCurrent()
        advanceTimeBy(10_000)
        runCurrent()
        assertThat(calls).containsExactly(false)
        sync.emitSyncState(SyncState.Running)
        runCurrent()
        advanceTimeBy(SEND_QUEUES_RETRY_DELAY_MILLIS)
        runCurrent()
        assertThat(calls).containsExactly(false, true).inOrder()
    }

    @Test
    fun `loss of verification pauses immediately rather than after network debounce`() = runTest {
        verification.emitVerifiedStatus(SessionVerifiedStatus.Verified)
        sut.launchIn(backgroundScope, managed = true)
        runCurrent()
        advanceTimeBy(SEND_QUEUES_RETRY_DELAY_MILLIS)
        runCurrent()
        assertThat(calls).containsExactly(true)
        verification.emitVerifiedStatus(SessionVerifiedStatus.NotVerified)
        runCurrent()
        assertThat(calls).containsExactly(true, false).inOrder()
    }

    @Test
    fun `verification revoked during enable delay cannot enable the outbox`() = runTest {
        sut.launchIn(backgroundScope, managed = true)
        runCurrent()
        verification.emitVerifiedStatus(SessionVerifiedStatus.Verified)
        runCurrent()
        advanceTimeBy(SEND_QUEUES_RETRY_DELAY_MILLIS / 2)
        verification.emitVerifiedStatus(SessionVerifiedStatus.Unknown)
        runCurrent()
        advanceTimeBy(10_000)
        runCurrent()
        assertThat(calls).containsExactly(false, false).inOrder()
    }

    @Test
    fun `cancellation after the SDK enable side effect still reapplies the pause`() = runTest {
        val slowClient = object : MatrixClient by client {
            override suspend fun setAllSendQueuesEnabled(enabled: Boolean) {
                calls.add(enabled)
                if (enabled) {
                    verification.emitVerifiedStatus(SessionVerifiedStatus.NotVerified)
                    awaitCancellation()
                }
            }
        }
        SendQueues(slowClient, sync).launchIn(backgroundScope, managed = true)
        runCurrent()
        verification.emitVerifiedStatus(SessionVerifiedStatus.Verified)
        runCurrent()
        advanceTimeBy(SEND_QUEUES_RETRY_DELAY_MILLIS)
        runCurrent()
        assertThat(calls).containsExactly(false, true, false).inOrder()
    }

    @Test
    fun `queue retry event still reenables a verified session`() = runTest {
        verification.emitVerifiedStatus(SessionVerifiedStatus.Verified)
        sut.launchIn(backgroundScope, managed = true)
        runCurrent()
        advanceTimeBy(SEND_QUEUES_RETRY_DELAY_MILLIS)
        runCurrent()
        disabled.emit(RoomId("!room:example.org"))
        runCurrent()
        advanceTimeBy(SEND_QUEUES_RETRY_DELAY_MILLIS)
        runCurrent()
        assertThat(calls).containsExactly(true, true).inOrder()
    }

    @Test
    fun `cancelled account scope cannot enable an old session later`() = runTest {
        sut.launchIn(backgroundScope, managed = true)
        runCurrent()
        verification.emitVerifiedStatus(SessionVerifiedStatus.Verified)
        runCurrent()
        backgroundScope.cancel()
        advanceTimeBy(10_000)
        runCurrent()
        assertThat(calls).containsExactly(false)
    }
}
