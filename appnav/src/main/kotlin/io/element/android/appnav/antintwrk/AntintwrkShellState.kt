/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

enum class AntintwrkMode { Social, Messenger }
enum class AntintwrkSection { Feed, Communities, Profile }
enum class AntintwrkFeedScope { Recommended, Latest, Network }
enum class AntintwrkOverlay { None, Search, Notifications, Comments, Share }

data class AntintwrkShellState(
    val mode: AntintwrkMode = AntintwrkMode.Social,
    val section: AntintwrkSection = AntintwrkSection.Feed,
    val scope: AntintwrkFeedScope = AntintwrkFeedScope.Recommended,
    val overlay: AntintwrkOverlay = AntintwrkOverlay.None,
    val query: String = "",
    val eventSink: (AntintwrkShellEvent) -> Unit = {},
)

sealed interface AntintwrkShellEvent {
    data class Mode(val value: AntintwrkMode) : AntintwrkShellEvent
    data class Section(val value: AntintwrkSection) : AntintwrkShellEvent
    data class Scope(val value: AntintwrkFeedScope) : AntintwrkShellEvent
    data class Overlay(val value: AntintwrkOverlay) : AntintwrkShellEvent
    data class Query(val value: String) : AntintwrkShellEvent
    data object Back : AntintwrkShellEvent
}

class AntintwrkShellPresenter {
    @Composable
    fun present(): AntintwrkShellState {
        var mode by rememberSaveable { mutableStateOf(AntintwrkMode.Social) }
        var section by rememberSaveable { mutableStateOf(AntintwrkSection.Feed) }
        var scope by rememberSaveable { mutableStateOf(AntintwrkFeedScope.Recommended) }
        var query by rememberSaveable { mutableStateOf("") }
        var overlay by rememberSaveable { mutableStateOf(AntintwrkOverlay.None) }
        return AntintwrkShellState(mode, section, scope, overlay, query) { event ->
            when (event) {
                is AntintwrkShellEvent.Mode -> {
                    mode = event.value
                    overlay = AntintwrkOverlay.None
                }
                is AntintwrkShellEvent.Section -> {
                    section = event.value
                    overlay = AntintwrkOverlay.None
                }
                is AntintwrkShellEvent.Scope -> scope = event.value
                is AntintwrkShellEvent.Overlay -> overlay = event.value
                is AntintwrkShellEvent.Query -> query = event.value.take(200)
                AntintwrkShellEvent.Back -> when {
                    overlay != AntintwrkOverlay.None -> overlay = AntintwrkOverlay.None
                    mode == AntintwrkMode.Messenger -> mode = AntintwrkMode.Social
                    else -> section = AntintwrkSection.Feed
                }
            }
        }
    }
}
