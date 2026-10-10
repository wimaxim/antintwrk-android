// Copyright 2026 Maxim Novichkov
// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.appnav.R
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton

/** Each accepted system share opens a new composer; ordinary root drafts are independent. */
@Composable
internal fun AntintwrkIncomingShareView(
    actor: String?,
    controller: AntintwrkWebFeedController,
    language: String,
    onSocial: () -> Unit,
) {
    val request by AntintwrkIncomingShares.requests.collectAsState()
    controller.incomingActor = actor
    LaunchedEffect(actor, request?.id) {
        if (actor != null && request?.actor != null && request?.actor != actor) AntintwrkIncomingShares.clear()
        if (controller.incomingDelivery?.actor != actor) controller.clearIncomingDelivery()
    }
    LaunchedEffect(actor, controller.currentUrl, controller.loading) {
        if (!controller.loading) controller.view?.let { controller.deliverIncomingShare(it) }
    }
    val current = request ?: return
    if (actor == null || current.actor != null && current.actor != actor) return
    if (controller.fileCallback != null || controller.prompt != null || controller.document != null || controller.fullscreen != null) return
    AntintwrkIncomingShareDialog(current, false, controller.profileName.orEmpty(), onCreate = {
        controller.clearIncomingDelivery()
        controller.incomingDelivery = current.copy(actor = actor)
        controller.incomingImportBusy = true
        AntintwrkIncomingShares.consume(current)
        onSocial()
        controller.navigate(AntintwrkWebPolicy(controller.origin).route(language, "hero-compose") + "&nativeShare=${current.id}", forceAdditional = true, allowRouting = false)
    }, onCancel = AntintwrkIncomingShares::clear)
}

@Composable
private fun AntintwrkIncomingShareDialog(
    current: AntintwrkIncomingShare,
    blocked: Boolean,
    profileName: String,
    onCreate: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.screen_ant_share_title)) },
        text = {
            Text(
                when {
                current.preparing -> stringResource(io.element.android.libraries.ui.strings.R.string.common_loading)
                current.draftConflict -> stringResource(R.string.screen_ant_share_draft)
                current.failed -> stringResource(R.string.screen_ant_share_failed)
                blocked -> stringResource(R.string.screen_ant_share_draft)
                else -> stringResource(
                    R.string.screen_ant_share_review,
                    profileName.takeIf { it.isNotBlank() } ?: stringResource(R.string.screen_ant_share_your_profile),
                    current.files.size,
                )
            }
            )
        },
        confirmButton = {
            if (!current.failed && !current.preparing && !blocked) TextButton(stringResource(R.string.screen_ant_share_create), onCreate)
        },
        dismissButton = { TextButton(stringResource(io.element.android.libraries.ui.strings.R.string.action_cancel), onCancel) },
    )
}

internal class AntintwrkSharePreviewProvider : PreviewParameterProvider<AntintwrkIncomingShare> {
    override val values = sequenceOf(
        AntintwrkIncomingShare("preview", null, "Shared text"),
        AntintwrkIncomingShare("preview", null, "", preparing = true),
        AntintwrkIncomingShare("preview", null, "", failed = true),
        AntintwrkIncomingShare("preview", null, "", failed = true, draftConflict = true),
    )
}

@PreviewsDayNight
@Composable
internal fun AntintwrkIncomingSharePreview(@PreviewParameter(AntintwrkSharePreviewProvider::class) state: AntintwrkIncomingShare) = ElementPreview {
    AntintwrkIncomingShareDialog(state, blocked = false, profileName = "QA profile", onCreate = {}, onCancel = {})
}
