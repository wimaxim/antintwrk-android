// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.libraries.pushproviders.unifiedpush

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import io.element.android.libraries.push.api.SocialPushRegistrar

@ContributesIntoSet(AppScope::class)
class UnsupportedSocialPushRegistrar : SocialPushRegistrar {
    override val available = false
    override suspend fun register(): Result<Unit> = Result.failure(UnsupportedOperationException())
}
