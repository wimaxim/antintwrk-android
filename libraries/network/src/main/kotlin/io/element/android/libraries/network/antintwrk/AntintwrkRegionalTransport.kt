// Copyright 2026 Maxim Novichkov
// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.libraries.network.antintwrk

import android.content.Context
import io.element.android.libraries.matrix.api.BuildConfig
import okhttp3.OkHttpClient
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketAddress
import java.net.URI
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicLong

/** Changes only the TCP destination. Inner HTTPS, issuer, RP, cookies and keys stay canonical. */
object AntintwrkRegionalTransport {
    val hosts = setOf("antimsg.com", "matrix.antimsg.com", "auth.antimsg.com", "push.antimsg.com", "rtc.antimsg.com")
    private val directHosts = setOf("antintwrk.com", "rtc.ru.antimsg.com", "rtc.la.antimsg.com")
    private val health = AntintwrkEdgeHealth()
    private val routes = AntintwrkRegionalRoutes(healthy = health::reachable)
    private var preferences: android.content.SharedPreferences? = null
    private val tunnel by lazy {
        AntintwrkConnectTunnel(hosts + directHosts) { host ->
            if (host in hosts) {
                routes.connect(host)
            } else {
                Socket().apply {
                try {
                    connect(InetSocketAddress(host, 443), 3000)
                } catch (error: IOException) {
                    close()
                    throw error
                }
            }
            }
        }
    }
    val proxyUrl: String? get() = if (BuildConfig.ANTINTWRK_NATIVE_AUTH) "http://127.0.0.1:${tunnel.port}" else null
    private val systemSelector = ProxySelector.getDefault()
    private val selector = object : ProxySelector() {
        override fun select(uri: URI): List<Proxy> = if (BuildConfig.ANTINTWRK_NATIVE_AUTH && uri.scheme == "https" &&
            uri.host in hosts && uri.port in setOf(-1, 443)) {
            listOf(Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", tunnel.port)))
        } else {
            systemSelector?.select(uri) ?: listOf(Proxy.NO_PROXY)
        }
        override fun connectFailed(uri: URI, sa: SocketAddress, ioe: IOException) = Unit
    }

    fun initialize(context: Context) {
        if (!BuildConfig.ANTINTWRK_NATIVE_AUTH) return
        preferences = context.getSharedPreferences("antintwrk-messenger-routing", Context.MODE_PRIVATE)
        routes.preferred = preferences?.getString("region", null)?.let { runCatching { AntintwrkRegion.valueOf(it) }.getOrNull() }
            ?: AntintwrkRegion.RU
    }

    /** Called only after validated, HTTPS IP geolocation, never from profile language/GPS. */
    fun recordSocialRecommendation(origin: String) {
        if (!BuildConfig.ANTINTWRK_NATIVE_AUTH) return
        routes.preferred = when (origin) {
            "https://antintwrk.com" -> AntintwrkRegion.LA
            "https://antimsg.com" -> AntintwrkRegion.RU
            else -> return
        }
        preferences?.edit()?.putString("region", routes.preferred.name)?.apply()
    }

    fun configure(builder: OkHttpClient.Builder): OkHttpClient.Builder =
        if (BuildConfig.ANTINTWRK_NATIVE_AUTH) builder.proxySelector(selector) else builder
}

internal enum class AntintwrkRegion(val edge: String) {
    RU("connect.ru.antimsg.com"),
    LA("connect.la.antimsg.com")
}

internal class AntintwrkRegionalRoutes(
    private val resolve: (String) -> Array<InetAddress> = InetAddress::getAllByName,
    private val dial: (AntintwrkRegion, InetAddress) -> Socket = ::antintwrkOpenEdge,
    private val healthy: (AntintwrkRegion, InetAddress, String) -> Boolean = { _, _, _ -> true },
    private val clock: () -> Long = System::nanoTime,
) {
    @Volatile var preferred = AntintwrkRegion.RU
    private val ruFailedUntil = AtomicLong()
    private val laFailedUntil = AtomicLong()
    private val ruSocialFailedUntil = AtomicLong()
    private val laSocialFailedUntil = AtomicLong()
    private fun deadline(region: AntintwrkRegion, host: String) = if (host == "antimsg.com") {
        if (region == AntintwrkRegion.RU) ruSocialFailedUntil else laSocialFailedUntil
    } else {
        if (region == AntintwrkRegion.RU) ruFailedUntil else laFailedUntil
    }

    fun connect(host: String): Socket {
        val first = preferred
        val second = if (first == AntintwrkRegion.RU) AntintwrkRegion.LA else AntintwrkRegion.RU
        val order = if (deadline(first, host).get() > clock()) listOf(second, first) else listOf(first, second)
        for (region in order) {
            try {
                for (address in resolve(region.edge)) {
                    try {
                        if (!healthy(region, address, host)) continue
                        return dial(region, address).also { deadline(region, host).set(0) }
                    } catch (_: IOException) {
                        // Retry only before CONNECT succeeds: no HTTP body has been sent.
                    }
                }
            } catch (_: IOException) {
                // DNS failure may be regional too. No request or credentials enter logs.
            }
            deadline(region, host).set(clock() + 30_000_000_000L)
        }
        throw IOException("Regional connection unavailable")
    }
}

/** App-private, loopback-only CONNECT. Cannot tunnel arbitrary hosts, ports or plaintext requests. */
internal class AntintwrkConnectTunnel(
    private val allowedHosts: Set<String>,
    private val connect: (String) -> Socket,
) : AutoCloseable {
    private val listener = ServerSocket().apply { bind(InetSocketAddress("127.0.0.1", 0), 32) }
    val port: Int get() = listener.localPort
    private val slots = Semaphore(64)
    private val workers = Executors.newCachedThreadPool { runnable -> Thread(runnable, "antintwrk-transport").apply { isDaemon = true } }
    init {
        workers.execute {
            while (!listener.isClosed) {
                val client = try {
                    listener.accept()
                } catch (_: IOException) {
                    break
                }
                if (!slots.tryAcquire()) {
                    client.close()
                } else {
                    workers.execute {
                        try {
                            serve(client)
                        } finally {
                            client.close()
                            slots.release()
                        }
                    }
                }
            }
        }
    }

    private fun authority(client: Socket): String? {
        client.soTimeout = 10_000
        val input = client.getInputStream()
        val header = StringBuilder()
        while (header.length < 8192) {
            val byte = input.read()
            if (byte < 0 || byte == 0 || byte > 127) return null
            header.append(byte.toChar())
            if (header.endsWith("\r\n\r\n")) {
                val first = header.toString().substringBefore("\r\n")
                val match = Regex("CONNECT ([a-z0-9.-]+):443 HTTP/1\\.[01]").matchEntire(first) ?: return null
                return match.groupValues[1].takeIf { it in allowedHosts }
            }
        }
        return null
    }

    private fun serve(client: Socket) {
        var connected = false
        try {
            val host = authority(client) ?: run {
                client.getOutputStream().write("HTTP/1.1 403 Forbidden\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
                return
            }
            connect(host).use { upstream ->
                // No TLS termination, response inspection, content logging or stream replay.
                client.soTimeout = 660_000
                upstream.soTimeout = 660_000
                client.getOutputStream().write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray())
                connected = true
                workers.execute {
                    try {
                        client.getInputStream().copyTo(upstream.getOutputStream(), 16_384)
                        upstream.shutdownOutput()
                    } catch (_: IOException) {
                        runCatching { upstream.close() }
                    }
                }
                upstream.getInputStream().copyTo(client.getOutputStream(), 16_384)
            }
        } catch (_: IOException) {
            if (!connected) {
                runCatching {
                client.getOutputStream().write("HTTP/1.1 502 Bad Gateway\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
            }
            }
        }
    }

    override fun close() {
        listener.close()
        workers.shutdownNow()
    }
}
