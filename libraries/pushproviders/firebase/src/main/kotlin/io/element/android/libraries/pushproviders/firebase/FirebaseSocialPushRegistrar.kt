// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.libraries.pushproviders.firebase

import com.google.firebase.messaging.FirebaseMessaging
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import io.element.android.libraries.push.api.SocialPushRegistrar

@ContributesIntoSet(AppScope::class)
class FirebaseSocialPushRegistrar(private val registerFirebaseSession: RegisterFirebaseSession) : SocialPushRegistrar {
    override val available = true
    override suspend fun register(): Result<Unit> = registerFirebaseSession().onSuccess {
        // Called after explicit device opt-in; keep the FID fresh while the app is closed.
        FirebaseMessaging.getInstance().isAutoInitEnabled = true
    }
}
