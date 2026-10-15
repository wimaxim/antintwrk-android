// Copyright 2026 Super David Universe LLC
// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import io.element.android.features.call.api.IncomingCallService
import io.element.android.libraries.matrix.api.core.EventId

/** Observe the retained messenger without routing through Chats or reloading Social. */
@Composable
internal fun AntintwrkIncomingCall(
    service: IncomingCallService,
    socialActor: String?,
    matrixActor: String?,
    resumed: Boolean,
    onRevealMessenger: () -> Unit,
) {
    val call by service.ringingCall.collectAsState(initial = null)
    var presented by remember(socialActor, matrixActor) { mutableStateOf<EventId?>(null) }
    val reveal by rememberUpdatedState(onRevealMessenger)
    LaunchedEffect(call, socialActor, matrixActor, resumed) {
        val incoming = call ?: return@LaunchedEffect
        if (!resumed || socialActor == null || socialActor != matrixActor || incoming.sessionId.value != socialActor || presented == incoming.eventId) {
            return@LaunchedEffect
        }
        if (service.showIncomingCall(incoming)) {
            presented = incoming.eventId
            reveal()
        }
    }
}
