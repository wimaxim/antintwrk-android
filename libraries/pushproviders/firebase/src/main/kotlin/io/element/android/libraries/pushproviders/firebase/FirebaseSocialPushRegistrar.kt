// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.libraries.pushproviders.firebase

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import io.element.android.libraries.push.api.SocialPushRegistrar

@ContributesIntoSet(AppScope::class)
class FirebaseSocialPushRegistrar(private val registerFirebaseSession: RegisterFirebaseSession) : SocialPushRegistrar {
    override val available = true
    override suspend fun register() = registerFirebaseSession()
}
