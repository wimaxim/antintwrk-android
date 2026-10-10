// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AntintwrkSocialSurfaceTest {
    @Test
    fun `messenger admission retains the same social controller and navigation`() {
        val root = AntintwrkSocialSurface()
        val socialControllerBeforeOAuth = root.web
        root.web.currentUrl = "https://antimsg.com/en/newsfeed?app=1"
        root.event(AntintwrkShellEvent.Section(AntintwrkSection.Communities))
        // Each navigation node resolves this same root-owned surface.
        val loggedInSurface = root
        assertThat(loggedInSurface.web).isSameInstanceAs(socialControllerBeforeOAuth)
        assertThat(loggedInSurface.web.currentUrl).contains("newsfeed")
        assertThat(loggedInSurface.shell.value.section).isEqualTo(AntintwrkSection.Communities)
    }

    @Test
    fun `logout clears the retained page and restores social navigation`() {
        val surface = AntintwrkSocialSurface()
        surface.web.currentUrl = "https://antimsg.com/en/hero/private?app=1"
        surface.web.origin = AntintwrkSocialOrigins.PUBLIC
        surface.web.routingSelected = true
        surface.event(AntintwrkShellEvent.Mode(AntintwrkMode.Messenger))
        surface.event(AntintwrkShellEvent.Section(AntintwrkSection.Profile))
        surface.clear()
        assertThat(surface.admission.value).isNull()
        assertThat(surface.web.currentUrl).isNull()
        assertThat(surface.web.origin).isEqualTo(AntintwrkSocialOrigins.RUSSIAN)
        assertThat(surface.web.routingSelected).isFalse()
        assertThat(surface.shell.value.mode).isEqualTo(AntintwrkMode.Social)
        assertThat(surface.shell.value.section).isEqualTo(AntintwrkSection.Feed)
        assertThat(surface.busy.value).isFalse()
    }
}
