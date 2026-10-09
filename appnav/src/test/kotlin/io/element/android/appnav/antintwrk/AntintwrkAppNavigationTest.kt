// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import io.element.android.features.preferences.api.PreferencesEntryPoint.InitialTarget
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AntintwrkAppNavigationTest {
    @After fun clear() = AntintwrkAppNavigation.clear()

    @Test fun `stale consumer cannot clear a newer account request`() {
        AntintwrkAppNavigation.open("@u18:antintwrk.com", AntintwrkAppDestination.SignOut)
        val old = requireNotNull(AntintwrkAppNavigation.requests.value)
        AntintwrkAppNavigation.open("@u19:antintwrk.com", AntintwrkAppDestination.Settings)
        AntintwrkAppNavigation.consume(old)
        assertEquals("@u19:antintwrk.com", AntintwrkAppNavigation.requests.value?.actor)
        val current = requireNotNull(AntintwrkAppNavigation.requests.value)
        assertTrue(current.sequence > old.sequence)
        AntintwrkAppNavigation.consume(current)
        assertNull(AntintwrkAppNavigation.requests.value)
    }

    @Test fun `foreign identities cannot request managed app actions`() {
        assertThrows(IllegalArgumentException::class.java) {
            AntintwrkAppNavigation.open("@user:example.org", AntintwrkAppDestination.SignOut)
        }
        assertNull(AntintwrkAppNavigation.requests.value)
    }

    @Test fun `common menu routes only to approved useful preferences`() {
        assertEquals(InitialTarget.MediaSettings, antintwrkAppPreference("native-media"))
        assertEquals(InitialTarget.LockScreenSettings, antintwrkAppPreference("native-screen-lock"))
        assertEquals(InitialTarget.LocationSettings, antintwrkAppPreference("native-location"))
        assertEquals(InitialTarget.NotificationSettings, antintwrkAppPreference("native-messenger-notifications"))
        assertEquals(InitialTarget.ModerationAndSafety, antintwrkAppPreference("native-moderation"))
        listOf("native-encryption", "native-logout", "native-labs", "native-developer", "https://evil.example").forEach {
            assertNull(antintwrkAppPreference(it))
        }
    }
}
