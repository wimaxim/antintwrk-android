// SPDX-License-Identifier: AGPL-3.0-only
@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
package io.element.android.appnav.antintwrk

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.common.truth.Truth.assertThat
import io.element.android.tests.testutils.robolectric.RobolectricTest
import io.element.android.tests.testutils.setSafeContent
import org.junit.Test

class AntintwrkStartupSplashTest : RobolectricTest() {
    @Test
    fun `background time is excluded and a later resume does not replay completed startup`() = runAndroidComposeUiTest {
        mainClock.autoAdvance = false
        val owner = object : LifecycleOwner {
            override val lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        }
        var ready = false
        setSafeContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                AntintwrkStartupSplash { ready = LocalAntintwrkStartupReady.current }
            }
        }
        mainClock.advanceTimeBy(1_000)
        runOnIdle { owner.lifecycle.currentState = Lifecycle.State.CREATED }
        mainClock.advanceTimeBy(5_000)
        runOnIdle {
            assertThat(ready).isFalse()
            owner.lifecycle.currentState = Lifecycle.State.RESUMED
        }
        mainClock.advanceTimeBy(1_150)
        runOnIdle {
            assertThat(ready).isTrue()
            owner.lifecycle.currentState = Lifecycle.State.CREATED
        }
        mainClock.advanceTimeBy(100)
        runOnIdle { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
        mainClock.advanceTimeBy(100)
        runOnIdle { assertThat(ready).isTrue() }
    }

    @Test
    fun `call permission cannot cover the startup animation before its minimum duration`() = runAndroidComposeUiTest {
        mainClock.autoAdvance = false
        val launches = mutableListOf<android.content.Intent>()
        val gate = AntintwrkCallPermissionEntryGate()
        setSafeContent {
            AntintwrkStartupSplash {
                if (LocalAntintwrkStartupReady.current) {
                    AntintwrkCallPermissionOnEntry(gate, sdk = 34, permissionCheck = { false }, settingsLaunch = launches::add)
                }
            }
        }
        mainClock.advanceTimeBy(1_950)
        runOnIdle { assertThat(launches).isEmpty() }
        mainClock.advanceTimeBy(200)
        runOnIdle { assertThat(launches).hasSize(1) }
    }
    @Test
    fun `startup is mounted immediately but stays behind animation for at least two seconds`() = runAndroidComposeUiTest {
        mainClock.autoAdvance = false
        var mounted = false
        var ready = false
        setSafeContent {
            AntintwrkStartupSplash {
                mounted = true
                ready = LocalAntintwrkStartupReady.current
            }
        }
        runOnIdle {
            assertThat(mounted).isTrue()
            assertThat(ready).isFalse()
        }
        mainClock.advanceTimeBy(1_950)
        runOnIdle { assertThat(ready).isFalse() }
        mainClock.advanceTimeBy(200)
        runOnIdle { assertThat(ready).isTrue() }
        mainClock.advanceTimeBy(3_000)
        runOnIdle { assertThat(ready).isTrue() }
    }

    @Test
    fun `clearing a retained surface invalidates an old admission and its profile`() {
        val surface = AntintwrkSocialSurface()
        surface.web.ownProfileIdentity = "fixture-A"
        surface.admission.value = AntintwrkAuthSession(AntintwrkAuthStage.AUTHENTICATED, "fixture", matrixUserId = "@u42:antintwrk.com")
        val old = surface.admissionEpoch.current
        surface.clear()
        assertThat(surface.admissionEpoch.accepts(old, true)).isFalse()
        assertThat(surface.admission.value).isNull()
        assertThat(surface.web.ownProfileIdentity).isNull()
    }
}
