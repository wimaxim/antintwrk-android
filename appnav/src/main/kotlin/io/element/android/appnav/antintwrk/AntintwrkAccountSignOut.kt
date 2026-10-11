// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

internal object AntintwrkAccountSignOut {
    fun matches(matrixActor: String?, sourceActor: String?, validMatrixSession: Boolean): Boolean =
        validMatrixSession && matrixActor != null && matrixActor == sourceActor &&
            io.element.android.libraries.matrix.api.user.AntintwrkMatrixIdentity.socialId(matrixActor) != null

    /** Called on main only after the original SDK logout actually removes its session. */
    fun afterMatrixLogout(context: Context, matrixActor: String?, validMatrixSession: Boolean, scope: CoroutineScope) {
        val preferences = context.getSharedPreferences("antintwrk-account", Context.MODE_PRIVATE)
        if (!matches(matrixActor, preferences.getString("social-actor", null), validMatrixSession)) return
        val client = AntintwrkNativeAuthClient()
        val push = AntintwrkSocialPushStore(context)
        val fid = push.fid
        val snapshot = client.clearLocalSession()
        synchronized(io.element.android.libraries.push.api.PushAccountPolicyLock.monitor) {
            preferences.edit().putBoolean("logged-out", true).putString("logged-out-actor", matrixActor)
                .remove("social-actor").remove("connect-messenger-for").commit()
            androidx.core.app.NotificationManagerCompat.from(context).cancelAll()
        }
        push.clear()
        AntintwrkIncomingShares.clear()
        AntintwrkSocialPushRoutes.consume()
        AntintwrkMessengerRoutes.clear()
        AntintwrkAppNavigation.clear()
        scope.launch {
            if (preferences.getBoolean("logged-out", false) && preferences.getString("social-actor", null) == null) {
                runCatching { AntintwrkNativeCredentials(context).clear() }
            }
            // Revocation is best effort and uses only the old cookie snapshot. A new
            // social login must never receive cookie headers from this cleanup job.
            runCatching {
                withTimeout(10_000) {
                    val session = client.session(snapshot)
                    if (session.matrixUserId == matrixActor) {
                        if (fid.isNotEmpty()) runCatching { client.push("push-unsubscribe", session.csrf, mapOf("fid" to fid), snapshot) }
                        client.logout(session.csrf, snapshot)
                    }
                }
            }
        }
    }
}
