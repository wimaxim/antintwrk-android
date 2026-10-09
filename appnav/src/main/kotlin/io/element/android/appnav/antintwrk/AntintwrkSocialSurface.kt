// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf

/** Root-owned social surface survives connecting/replacing the messenger navigation node. */
internal class AntintwrkSocialSurface(initialBusy: Boolean = true) {
    val admission = mutableStateOf<AntintwrkAuthSession?>(null)
    val busy = mutableStateOf(initialBusy)
    val web = AntintwrkWebFeedController()
    val shell = mutableStateOf(AntintwrkShellState())

    fun clear() {
        admission.value = null
        busy.value = false
        web.destroy()
        shell.value = AntintwrkShellState()
    }

    fun event(event: AntintwrkShellEvent) {
        val current = shell.value
        shell.value = when (event) {
            is AntintwrkShellEvent.Mode -> current.copy(mode = event.value, overlay = AntintwrkOverlay.None)
            is AntintwrkShellEvent.Section -> current.copy(section = event.value, overlay = AntintwrkOverlay.None)
            is AntintwrkShellEvent.Scope -> current.copy(scope = event.value)
            is AntintwrkShellEvent.Overlay -> current.copy(overlay = event.value)
            is AntintwrkShellEvent.Query -> current.copy(query = event.value.take(200))
            AntintwrkShellEvent.Back -> when {
                current.overlay != AntintwrkOverlay.None -> current.copy(overlay = AntintwrkOverlay.None)
                current.mode != AntintwrkMode.Social -> current.copy(mode = AntintwrkMode.Social)
                else -> current.copy(section = AntintwrkSection.Feed)
            }
        }
    }
}

internal val LocalAntintwrkSocialSurface = staticCompositionLocalOf<AntintwrkSocialSurface?> { null }
