// Copyright 2026 Maxim Novichkov
// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.libraries.network.antintwrk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AntintwrkRegionalTransportTest {
    @Test
    fun `region preference changes only future connections`() {
        val seen = mutableListOf<String>()
        val socket = Socket()
        val routes = AntintwrkRegionalRoutes(resolve = {
            seen.add(it)
            arrayOf(InetAddress.getLoopbackAddress())
        }, dial = { _, _ -> socket })
        assertEquals(socket, routes.connect("auth.antimsg.com"))
        routes.preferred = AntintwrkRegion.LA
        assertEquals(socket, routes.connect("matrix.antimsg.com"))
        assertEquals(listOf("connect.ru.antimsg.com", "connect.la.antimsg.com"), seen)
        assertFalse(socket.isClosed)
        socket.close()
    }

    @Test
    fun `unreachable region falls back before sending bytes and recovers after cooldown`() {
        var time = 1L
        var unavailable = true
        val seen = mutableListOf<String>()
        var current = ""
        val socket = Socket()
        val routes = AntintwrkRegionalRoutes(resolve = {
            current = it
            seen.add(it)
            arrayOf(InetAddress.getLoopbackAddress())
        }, dial = { _, _ ->
            if (current.startsWith("connect.ru") && unavailable) throw IOException()
            socket
        }, clock = { time })
        assertEquals(socket, routes.connect("auth.antimsg.com"))
        assertEquals(socket, routes.connect("auth.antimsg.com"))
        unavailable = false
        time += 31_000_000_000L
        assertEquals(socket, routes.connect("auth.antimsg.com"))
        assertEquals(listOf("connect.ru.antimsg.com", "connect.la.antimsg.com", "connect.la.antimsg.com", "connect.ru.antimsg.com"), seen)
        socket.close()
    }

    @Test
    fun `LA failure and DNS failure also fall back to RU`() {
        val seen = mutableListOf<String>()
        val socket = Socket()
        val routes = AntintwrkRegionalRoutes(resolve = {
            seen.add(it)
            if (it.startsWith("connect.la")) throw java.net.UnknownHostException()
            arrayOf(InetAddress.getLoopbackAddress())
        }, dial = { _, _ -> socket })
        routes.preferred = AntintwrkRegion.LA
        assertEquals(socket, routes.connect("matrix.antimsg.com"))
        assertEquals(listOf("connect.la.antimsg.com", "connect.ru.antimsg.com"), seen)
        socket.close()
    }

    @Test(expected = IOException::class)
    fun `no region reachable fails closed`() {
        AntintwrkRegionalRoutes(resolve = { throw java.net.UnknownHostException() }).connect("auth.antimsg.com")
    }

    @Test
    fun `a reachable edge without active service is skipped before credentials are sent`() {
        val dialled = mutableListOf<AntintwrkRegion>()
        val socket = Socket()
        val routes = AntintwrkRegionalRoutes(
            resolve = { arrayOf(InetAddress.getLoopbackAddress()) },
            healthy = { region, _, _ -> region == AntintwrkRegion.LA },
            dial = { region, _ ->
                dialled.add(region)
                socket
            },
        )
        assertEquals(socket, routes.connect("auth.antimsg.com"))
        assertEquals(listOf(AntintwrkRegion.LA), dialled)
        socket.close()
    }

    @Test
    fun `messenger failure does not mark a working social edge unavailable`() {
        val dialled = mutableListOf<AntintwrkRegion>()
        val routes = AntintwrkRegionalRoutes(
            resolve = { arrayOf(InetAddress.getLoopbackAddress()) },
            healthy = { region, _, host -> host == "antimsg.com" || region == AntintwrkRegion.LA },
            dial = { region, _ ->
                dialled.add(region)
                Socket()
            },
        )
        routes.connect("matrix.antimsg.com").close()
        routes.connect("antimsg.com").close()
        assertEquals(listOf(AntintwrkRegion.LA, AntintwrkRegion.RU), dialled)
    }

    @Test
    fun `arbitrary hosts ports credentials and plaintext requests never reach connector`() {
        var dials = 0
        AntintwrkConnectTunnel(setOf("auth.antimsg.com")) {
            dials++
            throw IOException()
        }.use { tunnel ->
            for (line in listOf(
                    "CONNECT evil.example:443 HTTP/1.1",
                    "CONNECT auth.antimsg.com:5432 HTTP/1.1",
                "CONNECT user@auth.antimsg.com:443 HTTP/1.1",
                    "GET https://auth.antimsg.com/ HTTP/1.1",
                "CONNECT 127.0.0.1:443 HTTP/1.1"
                )) {
                Socket("127.0.0.1", tunnel.port).use { client ->
                    client.soTimeout = 2000
                    client.getOutputStream().write((line + "\r\nHost: auth.antimsg.com\r\n\r\n").toByteArray())
                    assertTrue(readHeader(client).startsWith("HTTP/1.1 403"))
                }
            }
        }
        assertEquals(0, dials)
    }

    @Test
    fun `CONNECT forwards opaque TLS bytes unchanged in both directions`() {
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { upstream ->
            val workers = Executors.newSingleThreadExecutor()
            val task = workers.submit<ByteArray> {
                upstream.accept().use { server ->
                    server.soTimeout = 2000
                    val request = ByteArray(5)
                    java.io.DataInputStream(server.getInputStream()).readFully(request)
                    server.getOutputStream().write(byteArrayOf(23, 3, 3, 0, 0))
                    request
                }
            }
            var dials = 0
            AntintwrkConnectTunnel(setOf("auth.antimsg.com")) { host ->
                assertEquals("auth.antimsg.com", host)
                dials++
                Socket("127.0.0.1", upstream.localPort)
            }.use { tunnel ->
                Socket("127.0.0.1", tunnel.port).use { client ->
                    client.soTimeout = 2000
                    client.getOutputStream().write("CONNECT auth.antimsg.com:443 HTTP/1.1\r\nHost: auth.antimsg.com:443\r\n\r\n".toByteArray())
                    assertTrue(readHeader(client).startsWith("HTTP/1.1 200"))
                    client.getOutputStream().write(byteArrayOf(22, 3, 3, 0, 0))
                    val response = ByteArray(5)
                    java.io.DataInputStream(client.getInputStream()).readFully(response)
                    org.junit.Assert.assertArrayEquals(byteArrayOf(23, 3, 3, 0, 0), response)
                    org.junit.Assert.assertArrayEquals(byteArrayOf(22, 3, 3, 0, 0), task.get(3, TimeUnit.SECONDS))
                    assertEquals(-1, client.getInputStream().read())
                }
            }
            assertEquals(1, dials) // An established stream is never dialled/replayed.
            workers.shutdownNow()
        }
    }

    private fun readHeader(socket: Socket): String {
        val result = StringBuilder()
        while (!result.endsWith("\r\n\r\n") && result.length < 8192) {
            val next = socket.getInputStream().read()
            if (next < 0) break
            result.append(next.toChar())
        }
        return result.toString()
    }
}
