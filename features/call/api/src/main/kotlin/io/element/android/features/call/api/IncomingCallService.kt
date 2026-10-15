// Copyright 2026 Super David Universe LLC
// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.features.call.api

import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import kotlinx.coroutines.flow.Flow

data class IncomingCall(
    val sessionId: SessionId,
    val roomId: RoomId,
    val eventId: EventId,
    val expirationTimestamp: Long,
)

/** Foreground presentation only: displaying a call never answers it. */
interface IncomingCallService {
    val ringingCall: Flow<IncomingCall?>
    fun showIncomingCall(call: IncomingCall): Boolean
}
