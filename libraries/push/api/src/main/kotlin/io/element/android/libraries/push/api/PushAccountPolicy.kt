// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.libraries.push.api

/** Optional app admission boundary, evaluated before queuing account notifications. */
interface PushAccountPolicy {
    fun isAllowed(userId: String): Boolean
}

/** Serialises admission changes with final notification posting. No network in this lock. */
object PushAccountPolicyLock {
    val monitor = Any()
}
