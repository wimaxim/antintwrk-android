// Copyright 2026 Maxim Novichkov; SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.libraries.designsystem.components.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
fun AntintwrkHideChatDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmationDialog(
        title = stringResource(CommonStrings.screen_ant_delete_chat),
        content = stringResource(CommonStrings.screen_ant_delete_chat_explanation),
        submitText = stringResource(CommonStrings.screen_ant_delete_chat),
        destructiveSubmit = true,
        onSubmitClick = onConfirm,
        onDismiss = onDismiss,
    )
}

@PreviewsDayNight
@Composable
internal fun AntintwrkHideChatDialogPreview() = ElementPreview {
    AntintwrkHideChatDialog({}, {})
}
