// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner

internal val LocalAntintwrkStartupReady = staticCompositionLocalOf { true }
private val startupProcess = java.util.UUID.randomUUID().toString()

/** Startup runs underneath; count only rendered foreground animation, once per Activity launch. */
@Composable
fun AntintwrkStartupSplash(content: @Composable () -> Unit) {
    var displayedNanos by rememberSaveable { mutableLongStateOf(0L) }
    var savedProcess by rememberSaveable { mutableStateOf(startupProcess) }
    if (savedProcess != startupProcess) {
        savedProcess = startupProcess
        displayedNanos = 0L
    }
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val ready = displayedNanos >= 2_000_000_000L
    LaunchedEffect(lifecycle, ready) {
        if (ready || lifecycle != Lifecycle.State.RESUMED) return@LaunchedEffect
        var previous = withFrameNanos { it }
        while (displayedNanos < 2_000_000_000L) {
            val next = withFrameNanos { it }
            displayedNanos += (next - previous).coerceIn(0, 100_000_000L)
            previous = next
        }
    }
    Box(Modifier.fillMaxSize()) {
        Box(if (ready) Modifier else Modifier.clearAndSetSemantics {}) {
            CompositionLocalProvider(LocalAntintwrkStartupReady provides ready) { content() }
        }
        if (!ready) {
            AntintwrkLoadingView(Modifier.fillMaxSize().pointerInput(Unit) {
            awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } }
        })
        }
    }
}
