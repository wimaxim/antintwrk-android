// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.libraries.matrix.api.encryption

import io.element.android.libraries.matrix.api.user.AntintwrkMatrixIdentity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** In-memory, actor-bound UX state. Contains no keys, credentials or message data. */
object AntintwrkRecoveryStatus {
    enum class Phase { Working, Ready, Failed }

    private val mutablePhases = MutableStateFlow<Map<String, Phase>>(emptyMap())
    private val mutableRetries = MutableStateFlow<Map<String, Long>>(emptyMap())
    val phases = mutablePhases.asStateFlow()
    val retries = mutableRetries.asStateFlow()

    fun update(actor: String, phase: Phase) {
        if (AntintwrkMatrixIdentity.socialId(actor) != null) mutablePhases.update { it + (actor to phase) }
    }

    fun retry(actor: String) {
        if (AntintwrkMatrixIdentity.socialId(actor) != null) mutableRetries.update { it + (actor to ((it[actor] ?: 0L) + 1L)) }
    }
}
