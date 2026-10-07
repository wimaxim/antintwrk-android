// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import io.element.android.appnav.R
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.push.api.SocialPushRegistrar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@ContributesTo(AppScope::class)
interface AntintwrkSocialPushBindings {
    fun socialPushRegistrars(): Set<SocialPushRegistrar>
}

/** Kept composed above the admission/loading gate so permission results are not lost on resume. */
@Composable
internal fun AntintwrkPushSettings(
    client: AntintwrkNativeAuthClient,
    csrf: String?,
    open: Boolean,
    visible: Boolean,
    registrar: SocialPushRegistrar,
    store: AntintwrkSocialPushStore,
    onDismiss: () -> Unit,
    onAuthRequired: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var preferences by remember { mutableStateOf<Map<String, Boolean>?>(null) }
    var available by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var deviceEnabled by remember { mutableStateOf(store.enabled) }
    fun permitted() = NotificationManagerCompat.from(context).areNotificationsEnabled() &&
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
    suspend fun execute(request: suspend (String) -> Unit) {
        busy = true
        failed = false
        try {
            request(requireNotNull(csrf))
            deviceEnabled = store.enabled && permitted() && preferences?.get("enabled") == true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: AntintwrkRequestError) {
            failed = true
            if (error.code == "AUTH_REQUIRED") onAuthRequired()
        } catch (_: Exception) {
            failed = true
        } finally {
            busy = false
        }
    }
    fun enable() {
        scope.launch {
            execute { token ->
                val old = store.fid
                store.clear()
                val generation = store.generation
                store.fid = ""
                registrar.register().getOrThrow()
                withTimeout(15_000) { while (store.fid.isEmpty()) delay(100) }
                val result = client.push("push-subscribe", token, mapOf("fid" to store.fid))
                val values = requireNotNull(preferences) + ("enabled" to true)
                client.push("push-preferences", token, values.mapValues { if (it.value) "1" else "0" })
                store.bind(result.getString("binding"), generation)
                if (old.isNotEmpty() && old != store.fid) client.push("push-unsubscribe", token, mapOf("fid" to old))
                preferences = values
            }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && csrf != null) enable() else failed = true
    }
    suspend fun load() {
        preferences = null
        execute { token ->
            val result = client.push("push-settings", token)
            val values = result.getJSONObject("preferences")
            preferences = listOf("enabled", "friends", "messages", "comments", "votes", "important").associateWith { values.getBoolean(it) }
            available = result.getBoolean("available") && registrar.available
        }
    }
    LaunchedEffect(open) { if (open && csrf != null) load() }
    if (open && visible && csrf != null) {
        AntintwrkBrandTheme {
        AlertDialog(
            onDismissRequest = { if (!busy) onDismiss() },
            title = { Text(stringResource(R.string.screen_ant_push_title)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.screen_ant_push_device), modifier = Modifier.weight(1f))
                        Switch(modifier = Modifier.semantics {
                            contentDescription = context.getString(
                            R.string.screen_ant_push_device
                        )
                        }, checked = deviceEnabled, enabled =
                            !busy && available && preferences != null, onCheckedChange = { enabled ->
                            if (enabled) {
                                if (permitted()) {
                                    enable()
                                } else if (Build.VERSION.SDK_INT >= 33) {
                                    permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    failed = true
                                }
                            } else {
                                val fid = store.fid
                                store.clear()
                                deviceEnabled = false
                                scope.launch { execute { token -> client.push("push-unsubscribe", token, mapOf("fid" to fid)) } }
                            }
                        })
                    }
                    if (!registrar.available) Text(stringResource(R.string.screen_ant_push_unavailable))
                    if (!permitted()) {
                        TextButton(stringResource(R.string.screen_ant_push_system_settings), onClick = {
                        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                    })
                    }
                    if (failed) {
                        Text(stringResource(R.string.screen_ant_push_failed))
                        TextButton(stringResource(R.string.screen_ant_web_retry), enabled = !busy, onClick = { scope.launch { load() } })
                    }
                    if (busy) Text(stringResource(R.string.screen_ant_push_loading))
                    Text(stringResource(R.string.screen_ant_push_categories))
                    listOf(
                        "friends" to R.string.screen_ant_push_friends,
                        "messages" to R.string.screen_ant_push_messages,
                        "comments" to R.string.screen_ant_push_comments,
                        "votes" to R.string.screen_ant_push_votes,
                        "important" to R.string.screen_ant_push_important,
                    ).forEach { (key, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(modifier = Modifier.semantics {
                                contentDescription = context.getString(
                                label
                            )
                            }, checked = preferences?.get(key) == true, enabled =
                                !busy && preferences != null, onCheckedChange = { value ->
                                preferences = preferences?.plus(key to value)
                            })
                            Text(stringResource(label))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(stringResource(android.R.string.ok), enabled = !busy && preferences != null, onClick = {
                    scope.launch {
                        execute { token ->
                        client.push("push-preferences", token, requireNotNull(preferences).mapValues { if (it.value) "1" else "0" })
                        onDismiss()
                    }
                    }
                })
            },
            dismissButton = { TextButton(stringResource(android.R.string.cancel), enabled = !busy, onClick = onDismiss) },
        )
    }
    }
}
