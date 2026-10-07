// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.libraries.push.api

/** Optional independent social transport; Matrix messages continue through the existing handler. */
interface SocialPushReceiver {
    suspend fun registered(installationId: String)
    fun receive(data: Map<String, String>): Boolean
}

interface SocialPushRegistrar {
    val available: Boolean
    suspend fun register(): Result<Unit>
}
