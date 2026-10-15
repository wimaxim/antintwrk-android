// Copyright 2026 Super David Universe LLC
// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.common.truth.Truth.assertThat
import io.element.android.features.call.api.IncomingCall
import io.element.android.features.call.api.IncomingCallService
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.test.AN_EVENT_ID
import io.element.android.libraries.matrix.test.A_ROOM_ID
import io.element.android.libraries.matrix.test.A_SESSION_ID
import io.element.android.tests.testutils.robolectric.RobolectricTest
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class AntintwrkIncomingCallTest : RobolectricTest() {
    @get:Rule val compose = createComposeRule()
    private val call = IncomingCall(A_SESSION_ID, A_ROOM_ID, AN_EVENT_ID, Long.MAX_VALUE)
    private val resumed = mutableStateOf(true)
    private val actor = mutableStateOf<String?>(A_SESSION_ID.value)
    private val shown = mutableListOf<IncomingCall>()
    private var allowed = true
    private val service = object : IncomingCallService {
        override val ringingCall = MutableStateFlow<IncomingCall?>(null)
        override fun showIncomingCall(call: IncomingCall): Boolean {
            if (allowed) shown += call
            return allowed
        }
    }
    private val surface = AntintwrkSocialSurface(initialBusy = false)

    private fun mount() = compose.setContent {
        AntintwrkIncomingCall(service, actor.value, A_SESSION_ID.value, resumed.value) {
            surface.event(AntintwrkShellEvent.Mode(AntintwrkMode.Messenger))
        }
    }

    @Test
    fun `call reveals Messenger once without replacing Social or resetting its section`() {
        surface.event(AntintwrkShellEvent.Section(AntintwrkSection.Profile))
        surface.web.currentUrl = "https://antintwrk.com/en/hero/123"
        val web = surface.web
        mount()
        compose.runOnIdle { service.ringingCall.value = call }
        compose.runOnIdle {
            assertThat(shown).containsExactly(call)
            assertThat(surface.shell.value.mode).isEqualTo(AntintwrkMode.Messenger)
            assertThat(surface.shell.value.section).isEqualTo(AntintwrkSection.Profile)
            assertThat(surface.web).isSameInstanceAs(web)
            assertThat(web.currentUrl).isEqualTo("https://antintwrk.com/en/hero/123")
            resumed.value = false
        }
        compose.runOnIdle { resumed.value = true }
        compose.runOnIdle {
            assertThat(shown).hasSize(1)
            service.ringingCall.value = null
        }
        compose.runOnIdle { service.ringingCall.value = call.copy(eventId = EventId("\$next")) }
        compose.runOnIdle { assertThat(shown).hasSize(2) }
    }

    @Test
    fun `wrong actor logout background and rejected native presentation do not switch tabs`() {
        resumed.value = false
        mount()
        compose.runOnIdle { service.ringingCall.value = call }
        compose.runOnIdle {
            assertThat(shown).isEmpty()
            actor.value = "@other:example.com"
            resumed.value = true
        }
        compose.runOnIdle {
            assertThat(shown).isEmpty()
            actor.value = null
        }
        compose.runOnIdle {
            assertThat(shown).isEmpty()
            allowed = false
            actor.value = A_SESSION_ID.value
        }
        compose.runOnIdle {
            assertThat(shown).isEmpty()
            assertThat(surface.shell.value.mode).isEqualTo(AntintwrkMode.Social)
            resumed.value = false
            allowed = true
        }
        compose.runOnIdle { resumed.value = true }
        compose.runOnIdle { assertThat(shown).containsExactly(call) }
    }
}
