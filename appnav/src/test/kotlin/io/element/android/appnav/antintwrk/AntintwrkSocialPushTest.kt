// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test

class AntintwrkSocialPushTest : RobolectricTest() {
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
        assertThat(AntintwrkSocialPushReceiver(context).receive(mapOf("kind" to "antintwrk.social.v1", "binding" to "b".repeat(32)))).isTrue()
        store.clear()
    }
}
