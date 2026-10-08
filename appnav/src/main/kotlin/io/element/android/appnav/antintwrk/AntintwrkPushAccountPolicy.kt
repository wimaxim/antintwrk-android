// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import io.element.android.appnav.BuildConfig
import io.element.android.libraries.di.annotations.ApplicationContext
import io.element.android.libraries.push.api.PushAccountPolicy

@ContributesIntoSet(AppScope::class)
@Inject
class AntintwrkPushAccountPolicy(@ApplicationContext private val context: Context) : PushAccountPolicy {
    companion object {
        internal fun allows(userId: String, sourceActor: String?, loggedOut: Boolean): Boolean =
            !loggedOut && sourceActor != null && sourceActor == userId
    }
    override fun isAllowed(userId: String): Boolean {
        if (!BuildConfig.ANTINTWRK_NATIVE_AUTH) return true
        val preferences = context.getSharedPreferences("antintwrk-account", Context.MODE_PRIVATE)
        return allows(userId, preferences.getString("social-actor", null), preferences.getBoolean("logged-out", false))
    }
}
