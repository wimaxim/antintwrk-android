/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import android.webkit.WebChromeClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

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
}
