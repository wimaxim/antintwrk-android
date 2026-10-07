// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.libraries.pushproviders.firebase

import com.google.firebase.messaging.FirebaseMessaging
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import io.element.android.libraries.push.api.SocialPushRegistrar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@ContributesIntoSet(AppScope::class)
class FirebaseSocialPushRegistrar(private val registerFirebaseSession: RegisterFirebaseSession) : SocialPushRegistrar {
    override val available = true
    override suspend fun register(): Result<Unit> = withContext(Dispatchers.IO) {
        // The FCM SDK resolves the FID synchronously before returning its registration task.
        // Keep that work off the UI thread, including when called from the opt-in dialog.
        registerFirebaseSession().onSuccess {
            FirebaseMessaging.getInstance().isAutoInitEnabled = true
        }
    }
}
