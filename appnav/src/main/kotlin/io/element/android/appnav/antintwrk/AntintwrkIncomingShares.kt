// Copyright 2026 Maxim Novichkov
// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.content.IntentCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

internal data class AntintwrkSharedFile(val uri: Uri, val mime: String)
internal data class AntintwrkIncomingShare(
    val id: String,
    val actor: String?,
    val text: String,
    val files: List<AntintwrkSharedFile> = emptyList(),
    val directory: File? = null,
    val failed: Boolean = false,
    val preparing: Boolean = false,
    val draftConflict: Boolean = false,
)

/** A private, bounded staging copy outlives the sender's temporary URI grant. Never sends or publishes anything. */
internal object AntintwrkIncomingShares {
    private val pending = MutableStateFlow<AntintwrkIncomingShare?>(null)
    val requests = pending.asStateFlow()

    suspend fun cleanup(context: Context) = withContext(Dispatchers.IO) {
        File(context.cacheDir, "social-incoming").listFiles()
            ?.filter { System.currentTimeMillis() - it.lastModified() > 86_400_000 }?.forEach { it.deleteRecursively() }
        File(context.cacheDir, "social-camera").listFiles()
            ?.filter { System.currentTimeMillis() - it.lastModified() > 86_400_000 }?.forEach { it.delete() }
        Unit
    }

    suspend fun receive(context: Context, intent: Intent) {
        val preferences = context.getSharedPreferences("antintwrk-account", Context.MODE_PRIVATE)
        val actor = preferences.getString("social-actor", null).takeUnless { preferences.getBoolean("logged-out", false) }
        val id = UUID.randomUUID().toString()
        val epoch = AntintwrkSocialCookieEpoch.current()
        clear()
        val input = runCatching { antintwrkIncomingShareInput(intent, context.packageName) }
        // Prevent redelivery when MainActivity is recreated. Do not mutate the stock Element share handler.
        intent.action = null
        pending.value = AntintwrkIncomingShare(id, actor, "", preparing = true)
        fun eligible() = pending.value?.id == id && (actor == null || AntintwrkSocialCookieEpoch.current() == epoch &&
            preferences.getString("social-actor", null) == actor && !preferences.getBoolean("logged-out", false))
        var directory: File? = null
        try {
            val (text, uris) = input.getOrThrow()
            val files = withContext(Dispatchers.IO) {
                val parent = File(context.cacheDir, "social-incoming").apply { mkdirs() }
                parent.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 86_400_000 }?.forEach { it.deleteRecursively() }
                val target = File(parent, id).apply { check(mkdirs()) }
                directory = target
                var total = 0L
                uris.mapIndexed { index, uri ->
                    currentCoroutineContext().ensureActive()
                    val mime = context.contentResolver.getType(uri) ?: intent.type.orEmpty()
                    val extension = antintwrkSharedExtension(mime) ?: error("Unsupported shared content")
                    val limit = if (mime.startsWith("video/")) 500L * 1024 * 1024 else 20L * 1024 * 1024
                    val file = File(target, "$index.$extension")
                    requireNotNull(context.contentResolver.openInputStream(uri)).use { source ->
                        file.outputStream().use { destination ->
                            total += antintwrkCopySharedStream(source, destination, minOf(limit, 550L * 1024 * 1024 - total))
                        }
                    }
                    require(total > 0)
                    AntintwrkSharedFile(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file), mime)
                }.also { require(it.count { file -> file.mime.startsWith("video/") } <= 1) }
            }
            if (eligible()) {
                pending.value = AntintwrkIncomingShare(id, actor, text, files, directory)
            } else {
                directory?.deleteRecursively()
            }
        } catch (cancelled: CancellationException) {
            directory?.deleteRecursively()
            if (pending.value?.id == id) pending.value = null
            throw cancelled
        } catch (_: Exception) {
            directory?.deleteRecursively()
            if (eligible()) {
                pending.value = AntintwrkIncomingShare(id, actor, "", failed = true)
            } else if (pending.value?.id == id) {
                pending.value = null
            }
        }
    }

    fun failedDelivery(request: AntintwrkIncomingShare, draft: Boolean) {
        pending.value = request.copy(failed = true, draftConflict = draft)
    }

    fun consume(request: AntintwrkIncomingShare) {
        if (pending.value?.id == request.id) pending.value = null
    }
    fun discardOtherActor(actor: String?) {
        if (pending.value?.actor != null && pending.value?.actor != actor) clear()
    }
    fun clear() {
        pending.value?.directory?.deleteRecursively()
        pending.value = null
    }
}

internal fun antintwrkIncomingShareInput(intent: Intent, packageName: String): Pair<String, List<Uri>> {
    require(intent.action in setOf(Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE))
    val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString().orEmpty()
    require(text.length <= 10_000)
    val streams = if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
        IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
    } else {
        listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
    }
    val clip = intent.clipData
    require(streams.size <= 50 && (clip?.itemCount ?: 0) <= 50)
    val uris = (streams + (0 until (clip?.itemCount ?: 0)).mapNotNull { clip?.getItemAt(it)?.uri }).distinct()
    require(uris.size <= 50 && (text.isNotBlank() || uris.isNotEmpty()))
    require(uris.all {
        it.scheme == "content" && !it.authority.isNullOrBlank() &&
        it.authority != packageName && !it.authority.orEmpty().startsWith("$packageName.")
    })
    return text to uris
}

internal fun antintwrkSharedExtension(mime: String): String? = when (mime) {
    "image/jpeg" -> "jpg"
    "image/png" -> "png"
    "image/webp" -> "webp"
    "video/mp4", "video/x-m4v" -> "mp4"
    "video/quicktime" -> "mov"
    "video/webm" -> "webm"
    else -> null
}

internal suspend fun antintwrkCopySharedStream(source: InputStream, destination: OutputStream, limit: Long): Long {
    val buffer = ByteArray(64 * 1024)
    var count = 0L
    while (true) {
        currentCoroutineContext().ensureActive()
        val read = source.read(buffer)
        if (read < 0) break
        count += read
        require(count <= limit)
        destination.write(buffer, 0, read)
    }
    require(count > 0)
    return count
}
