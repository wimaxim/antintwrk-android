// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import io.element.android.appnav.BuildConfig
import io.element.android.appnav.R
import io.element.android.libraries.di.annotations.ApplicationContext
import io.element.android.libraries.push.api.SocialPushReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

internal object AntintwrkSocialPushRoutes {
    const val ACTION = "com.antintwrk.app.SOCIAL_NOTIFICATIONS"
    private val requests = MutableStateFlow(0)
    val opens = requests.asStateFlow()
    fun open() {
        requests.value++
    }
    fun consume() {
        requests.value = 0
    }
}

internal class AntintwrkSocialPushStore(val context: Context) {
    private val preferences = context.getSharedPreferences("antintwrk-social-push", Context.MODE_PRIVATE)
    var fid: String
        get() = preferences.getString("fid", "").orEmpty()
        set(value) {
            preferences.edit().putString("fid", value).apply()
        }
    val binding: String get() = preferences.getString("binding", "").orEmpty()
    val generation: String get() = preferences.getString("generation", "").orEmpty()
    val enabled: Boolean get() = preferences.getBoolean("enabled", false)
    fun bind(value: String, expectedGeneration: String) {
        if (generation == expectedGeneration && value.matches(Regex("[a-f0-9]{32}"))) {
            preferences.edit().putString("binding", value).putBoolean("enabled", true).apply()
        }
    }
    fun clear() {
        preferences.edit().remove("binding").putBoolean("enabled", false).putString("generation", UUID.randomUUID().toString()).apply()
        NotificationManagerCompat.from(context).cancel("antintwrk-social", 1)
    }
}

@ContributesBinding(AppScope::class)
class AntintwrkSocialPushReceiver(@ApplicationContext private val context: Context) : SocialPushReceiver {
    override suspend fun registered(installationId: String) {
        if (!BuildConfig.ANTINTWRK_NATIVE_AUTH || !installationId.matches(Regex("[A-Za-z0-9_-]{22}"))) return
        val store = AntintwrkSocialPushStore(context)
        val old = store.fid
        store.fid = installationId
        if (store.enabled) {
            val generation = store.generation
            runCatching {
                val client = AntintwrkNativeAuthClient()
                val session = client.session()
                if (session.stage != AntintwrkAuthStage.AUTHENTICATED) {
                    store.clear()
                    return@runCatching
                }
                val result = client.push("push-subscribe", session.csrf, mapOf("fid" to installationId))
                store.bind(result.getString("binding"), generation)
                if (old != installationId && old.isNotEmpty()) client.push("push-unsubscribe", session.csrf, mapOf("fid" to old))
            }
        }
    }

    override fun receive(data: Map<String, String>): Boolean {
        if (!BuildConfig.ANTINTWRK_NATIVE_AUTH || data["kind"] != "antintwrk.social.v1") return false
        val store = AntintwrkSocialPushStore(context)
        if (!store.enabled || store.binding.isEmpty() || data["binding"] != store.binding) return true
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return true
            }
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return true
        val channel = "antintwrk-social"
        manager.createNotificationChannel(
            NotificationChannel(channel, context.getString(R.string.screen_ant_push_title), NotificationManager.IMPORTANCE_DEFAULT)
        )
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return true
        intent.action = AntintwrkSocialPushRoutes.ACTION
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(context, 491, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, channel).setSmallIcon(R.drawable.antintwrk_notification)
            .setContentTitle("antintwrk").setContentText(context.getString(R.string.screen_ant_push_new))
            .setContentIntent(pending).setAutoCancel(true).setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build()
        runCatching { manager.notify("antintwrk-social", 1, notification) }
        return true
    }
}
