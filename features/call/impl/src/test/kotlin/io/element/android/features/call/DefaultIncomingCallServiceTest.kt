// Copyright 2026 Super David Universe LLC
// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.features.call

import androidx.core.content.IntentCompat
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.element.android.features.call.api.CallData
import io.element.android.features.call.api.IncomingCall
import io.element.android.features.call.impl.DefaultIncomingCallService
import io.element.android.features.call.impl.notifications.CallNotificationData
import io.element.android.features.call.impl.notifications.aCallNotificationData
import io.element.android.features.call.impl.ui.IncomingCallActivity
import io.element.android.features.call.impl.utils.ActiveCall
import io.element.android.features.call.impl.utils.CallState
import io.element.android.features.call.utils.FakeActiveCallManager
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.test.AN_EVENT_ID
import io.element.android.libraries.matrix.test.A_ROOM_ID
import io.element.android.libraries.matrix.test.A_SESSION_ID
import io.element.android.services.appnavstate.test.FakeAppForegroundStateService
import io.element.android.services.toolbox.test.systemclock.A_FAKE_TIMESTAMP
import io.element.android.services.toolbox.test.systemclock.FakeSystemClock
import io.element.android.tests.testutils.robolectric.RobolectricTest
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

class DefaultIncomingCallServiceTest : RobolectricTest() {
    private val manager = FakeActiveCallManager()
    private val foreground = FakeAppForegroundStateService()
    private val clock = FakeSystemClock()
    private val service = DefaultIncomingCallService(InstrumentationRegistry.getInstrumentation().targetContext, manager, foreground, clock)
    private val incoming = IncomingCall(A_SESSION_ID, A_ROOM_ID, AN_EVENT_ID, A_FAKE_TIMESTAMP + 30_000)
    private val notification = aCallNotificationData(expirationTimestamp = incoming.expirationTimestamp)
    private val callData = CallData(A_SESSION_ID, A_ROOM_ID, true)

    @Test
    fun `foreground shows answer-decline screen without accepting the call`() {
        val active = ActiveCall(callData, CallState.Ringing(notification))
        manager.setActiveCall(active)
        assertThat(service.showIncomingCall(incoming)).isTrue()
        val intent = shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity
        assertThat(intent.component?.className).isEqualTo(IncomingCallActivity::class.java.name)
        assertThat(IntentCompat.getParcelableExtra(intent, IncomingCallActivity.EXTRA_NOTIFICATION_DATA, CallNotificationData::class.java))
            .isEqualTo(notification)
        assertThat(manager.activeCall.value).isEqualTo(active)
    }

    @Test
    fun `stale expired foreign and background presentations never start a screen`() {
        manager.setActiveCall(ActiveCall(callData, CallState.Ringing(notification)))
        assertThat(service.showIncomingCall(incoming.copy(sessionId = SessionId("@other:example.com")))).isFalse()
        clock.epochMillisResult = incoming.expirationTimestamp
        assertThat(service.showIncomingCall(incoming)).isFalse()
        clock.epochMillisResult = A_FAKE_TIMESTAMP
        foreground.givenIsInForeground(false)
        assertThat(service.showIncomingCall(incoming)).isFalse()
        foreground.givenIsInForeground(true)
        manager.setActiveCall(ActiveCall(callData, CallState.InCall))
        assertThat(service.showIncomingCall(incoming)).isFalse()
        manager.setActiveCall(null)
        assertThat(service.showIncomingCall(incoming)).isFalse()
        assertThat(shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity).isNull()
    }

    @Test
    fun `existing ringing call becomes presentable when process foreground state catches up`() = runTest {
        foreground.givenIsInForeground(false)
        manager.setActiveCall(ActiveCall(callData, CallState.Ringing(notification)))
        service.ringingCall.test {
            assertThat(awaitItem()).isNull()
            foreground.givenIsInForeground(true)
            assertThat(awaitItem()).isEqualTo(incoming)
            assertThat(service.showIncomingCall(incoming)).isTrue()
            foreground.givenIsInForeground(false)
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `observable includes only ringing and clears when accepted or ended`() = runTest {
        service.ringingCall.test {
            assertThat(awaitItem()).isNull()
            manager.setActiveCall(ActiveCall(callData, CallState.Ringing(notification)))
            assertThat(awaitItem()).isEqualTo(incoming)
            manager.setActiveCall(ActiveCall(callData, CallState.InCall))
            assertThat(awaitItem()).isNull()
            manager.setActiveCall(null)
            expectNoEvents()
        }
    }
}
