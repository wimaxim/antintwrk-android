// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@OptIn(ExperimentalCoroutinesApi::class)
class AntintwrkGeoNetworkTest {
    @Test
    fun `initial and repeated links are quiet but changed subnet network and reconnect trigger checks`() = runTest {
        val manager = mockk<ConnectivityManager>()
        val context = mockk<Context>()
        val callback = slot<ConnectivityManager.NetworkCallback>()
        every { context.getSystemService(ConnectivityManager::class.java) } returns manager
        every { manager.registerDefaultNetworkCallback(capture(callback)) } just Runs
        every { manager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>()) } just Runs
        val monitor = AntintwrkGeoNetworkMonitor(context)
        var events = 0
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        var job = backgroundScope.launch(dispatcher) { monitor.changes.collect { events++ } }
        val first = mockk<Network>()
        val second = mockk<Network>()
        val link = mockk<LinkProperties>()
        val identical = mockk<LinkProperties>()
        val changed = mockk<LinkProperties>()
        every { link.toString() } returns "link-one"
        every { identical.toString() } returns "link-one"
        every { changed.toString() } returns "link-two"
        val normal = mockk<NetworkCapabilities>()
        val vpn = mockk<NetworkCapabilities>()
        every { normal.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true
        every { vpn.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true
        every { normal.hasTransport(any()) } returns false
        every { vpn.hasTransport(any()) } answers { firstArg<Int>() == NetworkCapabilities.TRANSPORT_VPN }
        callback.captured.onAvailable(first)
        callback.captured.onCapabilitiesChanged(first, normal)
        callback.captured.onLinkPropertiesChanged(first, link)
        callback.captured.onLinkPropertiesChanged(first, identical)
        assertThat(events).isEqualTo(0)
        callback.captured.onLinkPropertiesChanged(first, changed)
        assertThat(events).isEqualTo(1)
        callback.captured.onCapabilitiesChanged(first, vpn)
        callback.captured.onCapabilitiesChanged(first, vpn)
        assertThat(events).isEqualTo(2)
        callback.captured.onAvailable(second)
        assertThat(events).isEqualTo(3)
        callback.captured.onLost(second)
        callback.captured.onAvailable(second)
        assertThat(events).isEqualTo(4)
        val old = callback.captured
        job.cancelAndJoin()
        verify(exactly = 1) { manager.unregisterNetworkCallback(old) }
        old.onAvailable(first) // An already queued callback cannot alter a resumed monitor.
        job = backgroundScope.launch(dispatcher) { monitor.changes.collect { events++ } }
        callback.captured.onAvailable(second)
        assertThat(events).isEqualTo(4)
        callback.captured.onAvailable(first)
        assertThat(events).isEqualTo(5)
        job.cancelAndJoin()
    }
}
