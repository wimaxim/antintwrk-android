// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import java.util.concurrent.atomic.AtomicBoolean

/** Volatile link metadata only; the server determines the public connection country. */
internal class AntintwrkGeoNetworkMonitor(context: Context) {
    private val manager = context.getSystemService(ConnectivityManager::class.java)
    private var network: Network? = null
    private var properties: String? = null
    private var capabilities: List<Boolean>? = null
    private var disconnected = false
    val changes = callbackFlow {
        val active = AtomicBoolean(true)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(value: Network) {
                if (!active.get()) return
                val changed = network != null && (value != network || disconnected)
                if (value != network) {
                    properties = null
                    capabilities = null
                }
                network = value
                disconnected = false
                if (changed) trySend(Unit)
            }
            override fun onLinkPropertiesChanged(value: Network, link: LinkProperties) {
                if (!active.get()) return
                val fingerprint = link.toString()
                if (value == network && properties != fingerprint) {
                    val changed = properties != null
                    properties = fingerprint
                    if (changed) trySend(Unit)
                }
            }
            override fun onCapabilitiesChanged(value: Network, state: NetworkCapabilities) {
                if (!active.get()) return
                val ready = state.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                val fingerprint = listOf(ready) + listOf(
                    NetworkCapabilities.TRANSPORT_CELLULAR,
                    NetworkCapabilities.TRANSPORT_WIFI,
                    NetworkCapabilities.TRANSPORT_ETHERNET,
                    NetworkCapabilities.TRANSPORT_VPN
                ).map(state::hasTransport)
                if (value == network && fingerprint != capabilities) {
                    val changed = capabilities != null
                    capabilities = fingerprint
                    if (ready && changed) trySend(Unit)
                }
            }
            override fun onLost(value: Network) {
                if (!active.get()) return
                if (value == network) {
                    disconnected = true
                }
            }
        }
        manager.registerDefaultNetworkCallback(callback)
        awaitClose {
            active.set(false)
            manager.unregisterNetworkCallback(callback)
        }
    }.conflate()
}
