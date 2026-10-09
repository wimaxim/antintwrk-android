// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import io.element.android.libraries.matrix.api.user.AntintwrkMatrixIdentity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

internal enum class AntintwrkAppDestination { Settings, Support, SignOut }
internal data class AntintwrkAppRequest(val actor: String, val destination: AntintwrkAppDestination, val sequence: Long)

/** Session-bound internal routes; no credentials, external URLs or cross-account replay. */
internal object AntintwrkAppNavigation {
    private val sequence = AtomicLong()
    private val pending = MutableStateFlow<AntintwrkAppRequest?>(null)
    val requests = pending.asStateFlow()
    fun open(actor: String, destination: AntintwrkAppDestination) {
        require(AntintwrkMatrixIdentity.socialId(actor) != null)
        pending.value = AntintwrkAppRequest(actor, destination, sequence.incrementAndGet())
    }
    fun consume(request: AntintwrkAppRequest) {
        pending.compareAndSet(request, null)
    }
    fun clear() {
        pending.value = null
    }
}

internal fun antintwrkAppPreference(route: String): io.element.android.features.preferences.api.PreferencesEntryPoint.InitialTarget? =
    when (route) {
        "native-media" -> io.element.android.features.preferences.api.PreferencesEntryPoint.InitialTarget.MediaSettings
        "native-screen-lock" -> io.element.android.features.preferences.api.PreferencesEntryPoint.InitialTarget.LockScreenSettings
        "native-location" -> io.element.android.features.preferences.api.PreferencesEntryPoint.InitialTarget.LocationSettings
        "native-messenger-notifications" -> io.element.android.features.preferences.api.PreferencesEntryPoint.InitialTarget.NotificationSettings
        "native-moderation" -> io.element.android.features.preferences.api.PreferencesEntryPoint.InitialTarget.ModerationAndSafety
        else -> null
    }
