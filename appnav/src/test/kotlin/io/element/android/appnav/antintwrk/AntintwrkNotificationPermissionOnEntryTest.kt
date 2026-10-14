// Copyright 2026 Super David Universe LLC
// SPDX-License-Identifier: AGPL-3.0-only
@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package io.element.android.appnav.antintwrk

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import com.google.common.truth.Truth.assertThat
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test

class AntintwrkNotificationPermissionOnEntryTest : RobolectricTest() {
    @Test
    fun `first entry requests ordinary permission without waiting for messenger`() = runAndroidComposeUiTest<ComponentActivity> {
        val requests = mutableListOf<String>()
        val recompose = mutableIntStateOf(0)
        setContent {
            recompose.intValue
            AntintwrkNotificationPermissionOnEntry(33, granted = { false }, previouslyAsked = { false }, request = requests::add)
        }
        runOnIdle { recompose.intValue++ }
        waitForIdle()
        runOnIdle { assertThat(requests).containsExactly(Manifest.permission.POST_NOTIFICATIONS) }
    }

    @Test
    fun `grant refusal and older platform do not produce repeated prompts`() {
        assertThat(AntintwrkNotificationEntryGate().shouldRequest(33, true, false)).isFalse()
        assertThat(AntintwrkNotificationEntryGate().shouldRequest(33, false, true)).isFalse()
        assertThat(AntintwrkNotificationEntryGate().shouldRequest(32, false, false)).isFalse()
        val gate = AntintwrkNotificationEntryGate()
        assertThat(gate.shouldRequest(34, false, false)).isTrue()
        assertThat(gate.shouldRequest(34, false, false)).isFalse()
    }
}
