// Copyright 2026 Super David Universe LLC
// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.element.android.libraries.androidutils.R as AndroidUtilsR

/** App-wide OS permission, independent of social and messenger account preparation. */
internal class AntintwrkCallPermissionEntryGate {
    private var askedThisEntry = false
    private var settingsInFlight = false

    fun shouldRequest(sdk: Int, granted: Boolean): Boolean {
        if (sdk < 34 || granted || askedThisEntry || settingsInFlight) return false
        askedThisEntry = true
        settingsInFlight = true
        return true
    }

    fun settingsReturned() {
        settingsInFlight = false
    }

    fun restoreSettingsLaunch() {
        askedThisEntry = true
        settingsInFlight = true
    }

    fun stopped(changingConfigurations: Boolean) {
        if (!changingConfigurations && !settingsInFlight) askedThisEntry = false
    }
}

// Preserve entry/return tracking through Activity rotations, but not across a fresh process.
private object AntintwrkCallPermissionEntries {
    val gate = AntintwrkCallPermissionEntryGate()
}

@Composable
internal fun AntintwrkCallPermissionOnEntry(
    gate: AntintwrkCallPermissionEntryGate = AntintwrkCallPermissionEntries.gate,
    sdk: Int = Build.VERSION.SDK_INT,
    permissionCheck: (() -> Boolean)? = null,
    settingsLaunch: ((Intent) -> Unit)? = null,
) {
    if (LocalInspectionMode.current || sdk < 34) return
    val context = LocalContext.current
    val activity = LocalActivity.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var awaitingSettings by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        awaitingSettings = false
        gate.settingsReturned()
    }
    val currentCheck by rememberUpdatedState(permissionCheck ?: { NotificationManagerCompat.from(context).canUseFullScreenIntent() })
    val currentLaunch by rememberUpdatedState(settingsLaunch ?: { intent: Intent -> launcher.launch(intent) })

    DisposableEffect(lifecycle, gate, sdk) {
        if (awaitingSettings) gate.restoreSettingsLaunch()
        fun checkOnEntry() {
            // An unknown platform error must not prevent login or repeatedly open Settings.
            val granted = runCatching { currentCheck() }.getOrDefault(true)
            if (!gate.shouldRequest(sdk, granted)) return
            awaitingSettings = true
            try {
                currentLaunch(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, "package:${context.packageName}".toUri()))
            } catch (_: ActivityNotFoundException) {
                try {
                    currentLaunch(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                } catch (_: ActivityNotFoundException) {
                    awaitingSettings = false
                    gate.settingsReturned()
                    android.widget.Toast.makeText(context, AndroidUtilsR.string.error_no_compatible_app_found, android.widget.Toast.LENGTH_LONG).show()
                } catch (_: SecurityException) {
                    awaitingSettings = false
                    gate.settingsReturned()
                }
            } catch (_: SecurityException) {
                awaitingSettings = false
                gate.settingsReturned()
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> checkOnEntry()
                Lifecycle.Event.ON_STOP -> gate.stopped(activity?.isChangingConfigurations == true)
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState == Lifecycle.State.RESUMED) checkOnEntry()
        onDispose { lifecycle.removeObserver(observer) }
    }
}
