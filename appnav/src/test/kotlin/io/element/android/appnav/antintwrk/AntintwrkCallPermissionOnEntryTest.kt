// SPDX-License-Identifier: AGPL-3.0-only
@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package io.element.android.appnav.antintwrk

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.common.truth.Truth.assertThat
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test

class AntintwrkCallPermissionOnEntryTest : RobolectricTest() {
    @Test
    fun `entry opens own full screen permission page before any account exists`() = runAndroidComposeUiTest<ComponentActivity> {
        val launches = mutableListOf<Intent>()
        val gate = AntintwrkCallPermissionEntryGate()
        val recompose = mutableIntStateOf(0)
        setContent {
            recompose.intValue
            AntintwrkCallPermissionOnEntry(gate, sdk = 34, permissionCheck = { false }, settingsLaunch = launches::add)
        }
        runOnIdle {
            assertThat(launches).hasSize(1)
            assertThat(launches.single().action).isEqualTo(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
            assertThat(launches.single().data.toString()).isEqualTo("package:${activity!!.packageName}")
            recompose.intValue++
        }
        waitForIdle()
        runOnIdle { assertThat(launches).hasSize(1) }
    }

    @Test
    fun `granted permission never launches settings`() = runAndroidComposeUiTest<ComponentActivity> {
        val launches = mutableListOf<Intent>()
        setContent {
            AntintwrkCallPermissionOnEntry(AntintwrkCallPermissionEntryGate(), sdk = 34, permissionCheck = { true }, settingsLaunch = launches::add)
        }
        runOnIdle { assertThat(launches).isEmpty() }
    }

    @Test
    fun `older Android does not query or open this special permission`() = runAndroidComposeUiTest<ComponentActivity> {
        setContent {
            AntintwrkCallPermissionOnEntry(
                AntintwrkCallPermissionEntryGate(),
                sdk = 33,
                permissionCheck = { error("Must not check on older Android") },
                settingsLaunch = { error("Must not launch") }
            )
        }
    }

    @Test
    fun `missing special settings falls back to own notification settings`() = runAndroidComposeUiTest<ComponentActivity> {
        val launches = mutableListOf<Intent>()
        setContent {
            AntintwrkCallPermissionOnEntry(AntintwrkCallPermissionEntryGate(), sdk = 34, permissionCheck = { false }, settingsLaunch = {
                launches.add(it)
                if (launches.size == 1) throw ActivityNotFoundException()
            })
        }
        runOnIdle {
            assertThat(launches).hasSize(2)
            assertThat(launches.last().action).isEqualTo(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            assertThat(launches.last().getStringExtra(Settings.EXTRA_APP_PACKAGE)).isEqualTo(activity!!.packageName)
        }
    }

    @Test
    fun `own settings roundtrip is suppressed then next real app entry rechecks`() = runAndroidComposeUiTest<ComponentActivity> {
        val owner = object : LifecycleOwner {
            override val lifecycle = LifecycleRegistry(this)
        }
        val gate = AntintwrkCallPermissionEntryGate()
        val launches = mutableListOf<Intent>()
        runOnIdle { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                AntintwrkCallPermissionOnEntry(gate, sdk = 34, permissionCheck = { false }, settingsLaunch = launches::add)
            }
        }
        runOnIdle {
            assertThat(launches).hasSize(1)
            owner.lifecycle.currentState = Lifecycle.State.CREATED
            gate.settingsReturned()
            owner.lifecycle.currentState = Lifecycle.State.RESUMED
            assertThat(launches).hasSize(1)
            owner.lifecycle.currentState = Lifecycle.State.CREATED
            owner.lifecycle.currentState = Lifecycle.State.RESUMED
            assertThat(launches).hasSize(2)
        }
    }

    @Test
    fun `rotation does not create a new entry after refusal`() {
        val gate = AntintwrkCallPermissionEntryGate()
        assertThat(gate.shouldRequest(34, false)).isTrue()
        gate.settingsReturned()
        gate.stopped(changingConfigurations = true)
        assertThat(gate.shouldRequest(34, false)).isFalse()
        gate.stopped(changingConfigurations = false)
        assertThat(gate.shouldRequest(34, false)).isTrue()
    }

    @Test
    fun `saved pending settings prevents duplicate launch after process recreation`() {
        val restored = AntintwrkCallPermissionEntryGate()
        restored.restoreSettingsLaunch()
        restored.stopped(changingConfigurations = false)
        assertThat(restored.shouldRequest(34, false)).isFalse()
        restored.settingsReturned()
        assertThat(restored.shouldRequest(34, false)).isFalse()
        restored.stopped(changingConfigurations = false)
        assertThat(restored.shouldRequest(34, false)).isTrue()
    }

    @Test
    fun `revocation is detected on next entry even if permission was initially granted`() {
        val gate = AntintwrkCallPermissionEntryGate()
        assertThat(gate.shouldRequest(34, true)).isFalse()
        gate.stopped(changingConfigurations = false)
        assertThat(gate.shouldRequest(34, false)).isTrue()
    }

    @Test
    fun `platform access failure does not block app entry`() = runAndroidComposeUiTest<ComponentActivity> {
        setContent {
            AntintwrkCallPermissionOnEntry(
                AntintwrkCallPermissionEntryGate(),
                sdk = 34,
                permissionCheck = { throw SecurityException() },
                settingsLaunch = { error("Must not launch") }
            )
        }
    }
}
