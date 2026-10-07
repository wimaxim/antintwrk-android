/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import android.app.Activity
import android.content.Intent
import android.net.Uri
import timber.log.Timber
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Registered above the admission screen so a picker survives foreground session checks. */
@Composable
internal fun AntintwrkWebFilePicker(controller: AntintwrkWebFeedController) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val callback = controller.fileCallback
        controller.fileCallback = null
        val selected = selectedSocialFiles(
            result.resultCode, result.data, controller.allowMultipleFiles,
            controller.view?.context?.packageName.orEmpty(),
        )
        Timber.d("Social file picker callback=%s result=%d selected=%d", callback != null, result.resultCode, selected?.size ?: 0)
        callback?.onReceiveValue(selected)
    }
    controller.launchFilePicker = launcher::launch
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/html")) { uri ->
        val document = controller.document
        controller.document = null
        if (document != null) {
            scope.launch {
                val saved = uri != null && withContext(Dispatchers.IO) {
                    runCatching {
                        requireNotNull(context.contentResolver.openOutputStream(uri)).use { it.write(document.content.toByteArray(Charsets.UTF_8)) }
                    }.isSuccess
                }
                document.complete(saved)
            }
        }
    }
    controller.launchDocumentSave = saver::launch
}

/** Photo Picker returns multiple selections in ClipData, without Intent.data. */
internal fun selectedSocialFiles(resultCode: Int, data: Intent?, multiple: Boolean, packageName: String): Array<Uri>? {
    if (resultCode != Activity.RESULT_OK || data == null || packageName.isEmpty()) return null
    val clip = data.clipData
    val uris = if (clip != null) {
        (0 until clip.itemCount.coerceAtMost(50)).mapNotNull { clip.getItemAt(it).uri }
    } else {
        listOfNotNull(data.data)
    }
    return uris.filter {
        val authority = it.authority.orEmpty()
        it.scheme == "content" && authority.isNotEmpty() &&
            !authority.equals(packageName, ignoreCase = true) && !authority.startsWith("$packageName.", ignoreCase = true)
    }.distinct().let { if (multiple) it else it.take(1) }.toTypedArray().takeIf { it.isNotEmpty() }
}
