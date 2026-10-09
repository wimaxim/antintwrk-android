// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.features.logout.api

/** Host-owned recovery check before a secondary feature can revoke a managed session. */
fun interface ManagedLogoutGuard {
    suspend fun prepare(userId: String): Boolean
}
