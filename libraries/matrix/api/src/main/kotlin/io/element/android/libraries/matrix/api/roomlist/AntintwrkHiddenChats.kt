// Copyright 2026 Maxim Novichkov; SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.libraries.matrix.api.roomlist

import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.timeline.item.event.MessageContent
import io.element.android.libraries.matrix.api.timeline.item.event.PollContent
import io.element.android.libraries.matrix.api.timeline.item.event.StickerContent
import io.element.android.libraries.matrix.api.timeline.item.event.UnableToDecryptContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.WeakHashMap

/** Device-local, account-scoped list visibility. Never leaves/forgets rooms, deletes history or changes receipts. */
class AntintwrkHiddenChats private constructor(private val client: MatrixClient) {
    private val file = File(client.sessionPaths.fileDirectory, "antintwrk-hidden-dms.json")
    private val lock = Mutex()
    private val hidden = MutableStateFlow<Map<String, Long>>(runCatching {
        if (file.isFile && file.length() <= 1_000_000) Json.decodeFromString<Map<String, Long>>(file.readText()) else emptyMap()
    }.getOrDefault(emptyMap()))
    val state = hidden.asStateFlow()
    private var observation: Job? = null

    fun observe(scope: CoroutineScope) {
        if (observation?.isActive == true) return
        observation = combine(client.roomListService.allRooms.summaries, state) { rooms, _ -> rooms }
            .onEach { rooms ->
                lock.withLock {
                    val next = hidden.value.filter { (id, timestamp) ->
                        !shouldRestore(rooms.firstOrNull { it.roomId.value == id }, timestamp)
                    }
                    if (next != hidden.value) {
                        try {
                            save(next)
                        } catch (cancelled: kotlinx.coroutines.CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            hidden.value = next
                        }
                    }
                }
            }.launchIn(scope)
        observation?.invokeOnCompletion { synchronized(instances) { instances.remove(client) } }
    }

    suspend fun hide(roomId: RoomId): Result<Unit> = try {
        lock.withLock {
            val room = client.roomListService.allRooms.summaries.replayCache.lastOrNull()?.firstOrNull { it.roomId == roomId }
            require(room?.isDm == true && room.info.activeMembersCount <= 2) { "Only personal chats may be hidden" }
            save(hidden.value + (roomId.value to (room.latestEventTimestamp ?: 0L)))
        }
        Result.success(Unit)
    } catch (cancelled: kotlinx.coroutines.CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    suspend fun reveal(roomId: RoomId) {
        lock.withLock {
            if (roomId.value in hidden.value) {
                val next = hidden.value - roomId.value
                try { save(next) } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (_: Exception) { hidden.value = next }
            }
        }
    }

    private suspend fun save(value: Map<String, Long>) = withContext(Dispatchers.IO) {
        check(file.parentFile?.isDirectory == true || file.parentFile?.mkdirs() == true)
        val temporary = File.createTempFile("hidden-dms", ".tmp", file.parentFile)
        try {
            temporary.outputStream().use { stream ->
                stream.write(Json.encodeToString(value).toByteArray(Charsets.UTF_8))
                stream.fd.sync()
            }
            check(temporary.renameTo(file)) { "Unable to save chat visibility" }
            hidden.value = value
        } finally {
            temporary.delete()
        }
    }

    companion object {
        private val instances = WeakHashMap<MatrixClient, AntintwrkHiddenChats>()
        private val opens = MutableSharedFlow<SessionId>(extraBufferCapacity = 16)
        val openChats = opens.asSharedFlow()
        fun returnToChats(actor: SessionId) {
            opens.tryEmit(actor)
        }
        fun forClient(client: MatrixClient): AntintwrkHiddenChats = synchronized(instances) {
            instances.getOrPut(client) { AntintwrkHiddenChats(client) }
        }

        fun shouldRestore(room: RoomSummary?, hiddenAt: Long): Boolean {
            val event = room?.latestEvent as? LatestEventValue.Remote ?: return false
            return room.isDm && !event.isOwn && event.timestamp > hiddenAt &&
                (event.content is MessageContent || event.content is StickerContent || event.content is PollContent || event.content is UnableToDecryptContent)
        }
    }
}
