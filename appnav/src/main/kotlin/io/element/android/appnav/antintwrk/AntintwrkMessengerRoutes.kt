// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

internal data class AntintwrkMessageRequest(val actor: String, val recipient: String, val sequence: Long)

/** Memory-only pending route survives the SDK's replacement of the login node. */
internal object AntintwrkMessengerRoutes {
    private val sequence = AtomicLong()
    private val pending = MutableStateFlow<AntintwrkMessageRequest?>(null)
    val requests = pending.asStateFlow()

    fun open(actor: String, recipient: String) {
        require(io.element.android.libraries.matrix.api.user.AntintwrkMatrixIdentity.sameRealm(actor, recipient))
        require(actor != recipient)
        pending.value = AntintwrkMessageRequest(actor, recipient, sequence.incrementAndGet())
    }
    fun consume(request: AntintwrkMessageRequest) {
        pending.compareAndSet(request, null)
    }
    fun discardOtherActor(actor: String?) {
        val request = pending.value ?: return
        if (request.actor != actor) pending.compareAndSet(request, null)
    }
    fun clear() {
        pending.value = null
    }
}
