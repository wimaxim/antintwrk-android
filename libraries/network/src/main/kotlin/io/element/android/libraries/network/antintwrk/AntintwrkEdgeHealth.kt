// Copyright 2026 Maxim Novichkov
// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.libraries.network.antintwrk

import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.Socket
import java.net.URI
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/** Preserve configured Wi-Fi/PAC proxy transport without altering system settings. */
internal fun antintwrkOpenEdge(region: AntintwrkRegion, address: InetAddress): Socket {
    val parent = ProxySelector.getDefault()?.select(URI("https://${region.edge}"))?.firstOrNull() ?: Proxy.NO_PROXY
    val socket = if (parent.type() == Proxy.Type.SOCKS) Socket(parent) else Socket()
    try {
        val destination = if (parent.type() == Proxy.Type.HTTP) parent.address() else InetSocketAddress(address, 443)
        socket.connect(destination, 1500)
        if (parent.type() == Proxy.Type.HTTP) {
            socket.soTimeout = 2000
            socket.getOutputStream().write("CONNECT ${region.edge}:443 HTTP/1.1\r\nHost: ${region.edge}:443\r\n\r\n".toByteArray())
            val header = antintwrkReadHeader(socket)
            if (!Regex("HTTP/1\\.[01] 200(?: .*)?").matches(header.substringBefore("\r\n"))) throw IOException("Parent connection unavailable")
            socket.soTimeout = 0
        }
        return socket
    } catch (error: IOException) {
        socket.close()
        throw error
    }
}

internal fun antintwrkReadHeader(socket: Socket): String {
    val header = StringBuilder()
    while (header.length < 4096) {
        val byte = socket.getInputStream().read()
        if (byte < 0 || byte > 127 || byte == 0) throw IOException("Invalid service response")
        header.append(byte.toChar())
        if (header.endsWith("\r\n\r\n")) return header.toString()
    }
    throw IOException("Oversized service response")
}

/** Anonymous HTTPS readiness; never cookies, bearer credentials, or user content. */
internal class AntintwrkEdgeHealth {
    private val cache = mutableMapOf<Pair<AntintwrkRegion, Boolean>, Pair<Long, Boolean>>()
    fun reachable(region: AntintwrkRegion, address: InetAddress, host: String): Boolean = synchronized(region) {
        val social = host == "antimsg.com"
        val key = region to social
        val now = System.nanoTime()
        synchronized(cache) { cache[key] }?.takeIf { now - it.first < if (it.second) 10_000_000_000L else 2_000_000_000L }
            ?.let { return@synchronized it.second }
        val available = try {
            antintwrkOpenEdge(region, address).use { raw ->
                ((SSLSocketFactory.getDefault() as SSLSocketFactory).createSocket(raw, region.edge, 443, true) as SSLSocket).use { tls ->
                    tls.soTimeout = 2000
                    tls.sslParameters = tls.sslParameters.apply {
                        endpointIdentificationAlgorithm = "HTTPS"
                        serverNames = listOf(SNIHostName(region.edge))
                    }
                    tls.startHandshake()
                    val path = if (social) "/app/social-health" else "/app/health"
                    tls.getOutputStream().write(
                        "GET $path HTTP/1.1\r\nHost: ${region.edge}\r\nUser-Agent: ANTINTWRK transport/1.0\r\nConnection: close\r\n\r\n".toByteArray()
                    )
                    antintwrkReadHeader(tls).substringBefore("\r\n").matches(Regex("HTTP/1\\.[01] 200(?: .*)?"))
                }
            }
        } catch (_: IOException) {
            false
        }
        synchronized(cache) { cache[key] = System.nanoTime() to available }
        available
    }
}
