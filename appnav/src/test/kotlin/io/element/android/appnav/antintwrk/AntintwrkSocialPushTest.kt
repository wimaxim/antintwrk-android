// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.element.android.appnav.BuildConfig
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.robolectric.Shadows.shadowOf

class AntintwrkSocialPushTest : RobolectricTest() {
    @Test
    fun `social notifications use an alerting channel without a visible activity`() {
        assumeTrue(BuildConfig.ANTINTWRK_NATIVE_AUTH)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        shadowOf(context as android.app.Application).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        val store = AntintwrkSocialPushStore(context)
        store.clear()
        store.bind("a".repeat(32), store.generation)
        val system = context.getSystemService(NotificationManager::class.java)
        system.createNotificationChannel(NotificationChannel("antintwrk-social", "Old", NotificationManager.IMPORTANCE_DEFAULT))
        AntintwrkSocialPushReceiver(context).receive(mapOf("kind" to "antintwrk.social.v1", "binding" to store.binding))
        assertThat(system.getNotificationChannel("antintwrk-social-alerts-v2").importance).isEqualTo(NotificationManager.IMPORTANCE_HIGH)
        store.clear()
    }

    @Test
    fun `a muted old social channel stays muted after migration`() {
        assumeTrue(BuildConfig.ANTINTWRK_NATIVE_AUTH)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        shadowOf(context as android.app.Application).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        val store = AntintwrkSocialPushStore(context)
        store.clear()
        store.bind("a".repeat(32), store.generation)
        val system = context.getSystemService(NotificationManager::class.java)
        system.createNotificationChannel(NotificationChannel("antintwrk-social", "Old", NotificationManager.IMPORTANCE_NONE))
        AntintwrkSocialPushReceiver(context).receive(mapOf("kind" to "antintwrk.social.v1", "binding" to store.binding))
        assertThat(system.getNotificationChannel("antintwrk-social").importance).isEqualTo(NotificationManager.IMPORTANCE_NONE)
        assertThat(system.getNotificationChannel("antintwrk-social-alerts-v2")).isNull()
        store.clear()
    }

    @Test
    fun `Matrix payload is not consumed by the social receiver`() {
        val receiver = AntintwrkSocialPushReceiver(ApplicationProvider.getApplicationContext())
        assertThat(receiver.receive(mapOf("room_id" to "!room:example.org", "event_id" to "event"))).isFalse()
    }

    @Test
    fun `logout invalidates a pending registration and account binding`() {
        val store = AntintwrkSocialPushStore(ApplicationProvider.getApplicationContext())
        store.clear()
        val generation = store.generation
        store.bind("a".repeat(32), generation)
        assertThat(store.enabled).isTrue()
        store.clear()
        store.bind("a".repeat(32), generation)
        assertThat(store.enabled).isFalse()
        assertThat(store.binding).isEmpty()
    }

    @Test
    fun `social payload with a stale account binding is silently consumed`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = AntintwrkSocialPushStore(context)
        store.clear()
        store.bind("a".repeat(32), store.generation)
        assertThat(
            AntintwrkSocialPushReceiver(context).receive(mapOf("kind" to "antintwrk.social.v1", "binding" to "b".repeat(32)))
        ).isEqualTo(io.element.android.appnav.BuildConfig.ANTINTWRK_NATIVE_AUTH)
        store.clear()
    }
}
