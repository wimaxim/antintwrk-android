/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import android.webkit.WebChromeClient
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
        val ownAuthority = controller.view?.context?.packageName.orEmpty() + "."
        val selected = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            ?.filter { it.scheme == "content" && it.authority?.startsWith(ownAuthority, ignoreCase = true) != true }
            ?.let { if (controller.allowMultipleFiles) it else it.take(1) }
            ?.toTypedArray()?.takeIf { it.isNotEmpty() }
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
