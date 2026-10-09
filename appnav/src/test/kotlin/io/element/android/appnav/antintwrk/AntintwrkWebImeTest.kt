// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import android.webkit.WebView
import io.element.android.tests.testutils.robolectric.RobolectricTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class AntintwrkWebImeTest : RobolectricTest() {
    private val policy = AntintwrkWebPolicy("https://antimsg.com")
    @Test
    fun `keyboard state reaches only the active embedded page`() {
        val controller = AntintwrkWebFeedController()
        val page = mockk<WebView>(relaxed = true)
        every { page.url } returns policy.feed("en")
        controller.view = page
        controller.imeVisible = true
        AntintwrkWebIme.update(page, controller, policy)
        verify { page.evaluateJavascript(match { it.contains("update(true)") }, null) }
        val hidden = mockk<WebView>(relaxed = true)
        every { hidden.url } returns policy.feed("en")
        AntintwrkWebIme.update(hidden, controller, policy)
        verify(exactly = 0) { hidden.evaluateJavascript(any(), any()) }
    }
    @Test
    fun `foreign document cannot receive the injected correction`() {
        val controller = AntintwrkWebFeedController()
        val page = mockk<WebView>(relaxed = true)
        every { page.url } returns "https://foreign.example/"
        controller.view = page
        AntintwrkWebIme.update(page, controller, policy, install = true)
        verify(exactly = 0) { page.evaluateJavascript(any(), any()) }
    }
}
