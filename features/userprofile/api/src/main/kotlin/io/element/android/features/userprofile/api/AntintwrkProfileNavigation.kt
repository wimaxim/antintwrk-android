// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.features.userprofile.api

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

/** Memory-only actor-bound route; credentials and arbitrary web URLs are never accepted. */
object AntintwrkProfileNavigation {
    data class Request(val actor: String, val socialId: String, val sequence: Long)
    private val sequence = AtomicLong()
    private val pending = MutableStateFlow<Request?>(null)
    val requests = pending.asStateFlow()
    fun socialId(matrixId: String): String? = Regex("@u([1-9][0-9]{0,17}):matrix\\.antimsg\\.com").matchEntire(matrixId)?.groupValues?.get(1)
    fun open(actor: String, target: String) {
        if (socialId(actor) == null) return
        val id = socialId(target) ?: return
        pending.value = Request(actor, id, sequence.incrementAndGet())
    }
    fun consume(request: Request) {
        pending.compareAndSet(request, null)
    }
    fun clear() {
        pending.value = null
    }
}
