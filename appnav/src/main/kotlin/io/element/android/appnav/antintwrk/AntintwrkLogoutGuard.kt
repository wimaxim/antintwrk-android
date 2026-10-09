// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import io.element.android.features.logout.api.ManagedLogoutGuard
import io.element.android.libraries.matrix.api.MatrixClientProvider
import io.element.android.libraries.matrix.api.core.SessionId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

/** Also covers Forgot PIN; never use forced local deletion to bypass failed custody/revocation. */
@ContributesBinding(AppScope::class)
class AntintwrkLogoutGuard(private val clientProvider: MatrixClientProvider) : ManagedLogoutGuard {
    override suspend fun prepare(userId: String): Boolean {
        val client = clientProvider.getOrRestore(SessionId(userId)).getOrThrow()
        if (!AntintwrkAccountRecovery(client.encryptionService, userId).prepare()) return false
        withTimeout(30_000) { client.sessionVerificationService.sessionVerifiedStatus.first { it.isVerified() } }
        return true
    }
}
