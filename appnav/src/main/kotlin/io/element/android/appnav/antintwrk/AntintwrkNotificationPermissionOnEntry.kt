// Copyright 2026 Super David Universe LLC
// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

internal class AntintwrkNotificationEntryGate {
    private var asked = false
    fun shouldRequest(sdk: Int, granted: Boolean, previouslyAsked: Boolean): Boolean {
        if (sdk < 33 || granted || previouslyAsked || asked) return false
        asked = true
        return true
    }
}

/** Native social entry bypasses stock FTUE, so the application owns this OS prompt. */
@Composable
internal fun AntintwrkNotificationPermissionOnEntry(
    sdk: Int = Build.VERSION.SDK_INT,
    granted: (() -> Boolean)? = null,
    previouslyAsked: (() -> Boolean)? = null,
    request: ((String) -> Unit)? = null,
) {
    if (LocalInspectionMode.current || sdk < 33) return
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("antintwrk-permissions", Context.MODE_PRIVATE) }
    val gate = remember { AntintwrkNotificationEntryGate() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val currentGranted by rememberUpdatedState(granted ?: {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    })
    val currentAsked by rememberUpdatedState(previouslyAsked ?: { preferences.getBoolean("notifications-asked", false) })
    val currentRequest by rememberUpdatedState(request ?: { permission: String ->
        // Persist before launching; process/rotation/own dialog resume cannot repeat it.
        preferences.edit().putBoolean("notifications-asked", true).apply()
        launcher.launch(permission)
    })
    DisposableEffect(lifecycle, gate) {
        fun check() {
            if (gate.shouldRequest(sdk, currentGranted(), currentAsked())) {
                runCatching { currentRequest(Manifest.permission.POST_NOTIFICATIONS) }
            }
        }
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) check() }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState == Lifecycle.State.RESUMED) check()
        onDispose { lifecycle.removeObserver(observer) }
    }
}
