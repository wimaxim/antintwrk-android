/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import android.webkit.JsResult
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton

internal data class AntintwrkWebPrompt(val message: String, val result: JsResult, val confirm: Boolean)

@Composable
internal fun AntintwrkWebDialogs(controller: AntintwrkWebFeedController) {
    BackHandler(controller.fullscreen != null) { controller.closeFullscreen() }
    controller.prompt?.let { prompt ->
        fun finish(accepted: Boolean) {
            controller.prompt = null
            if (accepted) prompt.result.confirm() else prompt.result.cancel()
        }
        AlertDialog(
            onDismissRequest = { finish(false) },
            text = { Text(prompt.message) },
            confirmButton = { TextButton(stringResource(android.R.string.ok), { finish(true) }) },
            dismissButton = if (prompt.confirm) {
                { TextButton(stringResource(android.R.string.cancel), { finish(false) }) }
            } else {
                null
            },
        )
    }
}
