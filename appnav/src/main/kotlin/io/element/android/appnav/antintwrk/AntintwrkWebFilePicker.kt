// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import io.element.android.appnav.R
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** User chooses gallery or system camera. Only our exact capture output can bypass the private-provider filter. */
@Composable
internal fun AntintwrkWebFilePicker(controller: AntintwrkWebFeedController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var request by remember { mutableStateOf<WebChromeClient.FileChooserParams?>(null) }
    var owner by remember { mutableStateOf<WebView?>(null) }
    var epoch by remember { mutableStateOf(0L) }
    var capture by remember { mutableStateOf<File?>(null) }
    var captureUri by remember { mutableStateOf<Uri?>(null) }
    var failure by remember { mutableStateOf(false) }
    fun complete(uris: Array<Uri>?) {
        val callback = controller.fileCallback
        controller.fileCallback = null
        callback?.onReceiveValue(uris.takeIf { controller.view === owner && epoch == AntintwrkSocialCookieEpoch.current() })
        request = null
        owner = null
    }
    LaunchedEffect(controller.fileCallback) {
        if (controller.fileCallback == null) {
            request = null
            owner = null
            capture?.delete()
            capture = null
            captureUri = null
        }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        complete(selectedSocialFiles(result.resultCode, result.data, controller.allowMultipleFiles, context.packageName))
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val file = capture
        val uri = captureUri
        val accepted =
            result.resultCode == Activity.RESULT_OK && file != null && file.length() in 1..((if (file.extension == "jpg") 20L else 500L) * 1024 * 1024)
        if (!accepted) file?.delete()
        if (result.resultCode == Activity.RESULT_OK && !accepted) {
            failure = true
        } else {
            complete(if (accepted && uri != null) arrayOf(uri) else null)
        }
        capture = null
        captureUri = null
    }
    fun openCamera() {
        val params = request ?: return
        val video = socialCaptureKind(params.acceptTypes) == "video"
        val action = if (video) MediaStore.ACTION_VIDEO_CAPTURE else MediaStore.ACTION_IMAGE_CAPTURE
        val directory = File(context.cacheDir, "social-camera").apply { mkdirs() }
        directory.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 86_400_000 }?.forEach { it.delete() }
        val file = File(directory, "${UUID.randomUUID()}.${if (video) "mp4" else "jpg"}")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        capture = file
        captureUri = uri
        val intent = Intent(action).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
            clipData = ClipData.newRawUri("capture", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            if (video) {
                putExtra(MediaStore.EXTRA_DURATION_LIMIT, 300)
                putExtra(MediaStore.EXTRA_SIZE_LIMIT, 500L * 1024 * 1024)
            }
        }
        runCatching { camera.launch(intent) }.onFailure {
            file.delete()
            capture = null
            captureUri = null
            failure = true
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        if (allowed && controller.fileCallback != null && controller.view === owner && epoch == AntintwrkSocialCookieEpoch.current()) {
            openCamera()
        } else {
            failure = true
        }
    }
    controller.launchFilePicker = { params ->
        owner = controller.view
        epoch = AntintwrkSocialCookieEpoch.current()
        failure = false
        if (socialCaptureKind(params.acceptTypes) == null) {
            runCatching { gallery.launch(params.createIntent()) }.onFailure { complete(null) }
        } else {
            request = params
        }
    }
    request?.let { params ->
        AntintwrkMediaSourceDialog(
            socialCaptureKind(params.acceptTypes) == "video",
            failure,
            onGallery = { runCatching { gallery.launch(params.createIntent()) }.onFailure { complete(null) } },
            onCamera = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    permission.launch(Manifest.permission.CAMERA)
                }
            },
            onCancel = { complete(null) },
        )
    }

    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/html")) { uri ->
        val document = controller.document
        controller.document = null
        if (document != null) {
            scope.launch {
            val saved = uri != null && withContext(Dispatchers.IO) {
                runCatching {
                    requireNotNull(
                    context.contentResolver.openOutputStream(uri)
                ).use { it.write(document.content.toByteArray(Charsets.UTF_8)) }
                }.isSuccess
            }
            document.complete(saved)
        }
        }
    }
    controller.launchDocumentSave = saver::launch
}

internal fun socialCaptureKind(types: Array<String>): String? {
    val accepted = types.flatMap { it.split(',') }.map { it.trim().lowercase() }.filter { it.isNotEmpty() }
    return when {
        accepted.isNotEmpty() && accepted.all { it.startsWith("image/") } -> "image"
        accepted.isNotEmpty() && accepted.all { it.startsWith("video/") } -> "video"
        else -> null
    }
}

/** Photo Picker returns multiple selections in ClipData, without Intent.data. */
internal fun selectedSocialFiles(resultCode: Int, data: Intent?, multiple: Boolean, packageName: String): Array<Uri>? {
    if (resultCode != Activity.RESULT_OK || data == null || packageName.isEmpty()) return null
    val clip = data.clipData
    val uris = if (clip != null) (0 until clip.itemCount.coerceAtMost(50)).mapNotNull { clip.getItemAt(it).uri } else listOfNotNull(data.data)
    return uris.filter {
        val authority = it.authority.orEmpty()
        it.scheme == "content" && authority.isNotEmpty() && !authority.equals(packageName, ignoreCase = true) &&
            !authority.startsWith("$packageName.", ignoreCase = true)
    }.distinct().let { if (multiple) it else it.take(1) }.toTypedArray().takeIf { it.isNotEmpty() }
}

@Composable
private fun AntintwrkMediaSourceDialog(video: Boolean, failed: Boolean, onGallery: () -> Unit, onCamera: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.screen_ant_media_source)) },
        text = {
            Column {
                if (failed) Text(stringResource(R.string.screen_ant_camera_unavailable))
                TextButton(stringResource(R.string.screen_ant_media_gallery), onGallery)
                TextButton(stringResource(if (video) R.string.screen_ant_media_video else R.string.screen_ant_media_photo), onCamera)
            }
        },
        confirmButton = { TextButton(stringResource(io.element.android.libraries.ui.strings.R.string.action_cancel), onCancel) },
    )
}
internal class AntintwrkMediaSourcePreviews : PreviewParameterProvider<Pair<Boolean, Boolean>> {
    override val values = sequenceOf(false to false, true to false, false to true, true to true)
}
@Composable
@PreviewsDayNight
private fun AntintwrkMediaSourcePreview(@PreviewParameter(AntintwrkMediaSourcePreviews::class) state: Pair<Boolean, Boolean>) = ElementPreview {
    AntintwrkMediaSourceDialog(state.first, state.second, {}, {}, {})
}
