// Copyright 2026 Super David Universe LLC
// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.features.call.impl

import android.content.Context
import android.content.Intent
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import io.element.android.features.call.api.IncomingCall
import io.element.android.features.call.api.IncomingCallService
import io.element.android.features.call.impl.ui.IncomingCallActivity
import io.element.android.features.call.impl.utils.ActiveCall
import io.element.android.features.call.impl.utils.ActiveCallManager
import io.element.android.features.call.impl.utils.CallState
import io.element.android.libraries.di.annotations.ApplicationContext
import io.element.android.services.appnavstate.api.AppForegroundStateService
import io.element.android.services.toolbox.api.systemclock.SystemClock
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber

@ContributesBinding(AppScope::class)
class DefaultIncomingCallService(
    @ApplicationContext private val context: Context,
    private val activeCallManager: ActiveCallManager,
    private val foregroundService: AppForegroundStateService,
    private val clock: SystemClock,
) : IncomingCallService {
    override val ringingCall = combine(activeCallManager.activeCall, foregroundService.isInForeground) { active, foreground ->
        active.incomingCall().takeIf { foreground }
    }.distinctUntilChanged()

    override fun showIncomingCall(call: IncomingCall): Boolean {
        val active = activeCallManager.activeCall.value
        val data = (active?.callState as? CallState.Ringing)?.notificationData ?: return false
        if (active.incomingCall() != call || call.expirationTimestamp <= clock.epochMillis() || !foregroundService.isInForeground.value) return false
        return try {
            context.startActivity(Intent(context, IncomingCallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(IncomingCallActivity.EXTRA_NOTIFICATION_DATA, data)
            })
            true
        } catch (_: Exception) {
            Timber.w("Could not display the foreground incoming call")
            false
        }
    }
}

private fun ActiveCall?.incomingCall(): IncomingCall? = (this?.callState as? CallState.Ringing)?.notificationData?.let {
    IncomingCall(it.sessionId, it.roomId, it.eventId, it.expirationTimestamp)
}
