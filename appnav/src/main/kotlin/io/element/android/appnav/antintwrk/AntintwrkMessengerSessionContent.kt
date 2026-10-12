// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.matrix.api.encryption.AntintwrkRecoveryStatus
import io.element.android.libraries.matrix.api.verification.SessionVerifiedStatus
import io.element.android.libraries.ui.strings.CommonStrings

/** Keep the same mounted messenger while the new session recovers; never show a trust-override prompt. */
@Composable
internal fun AntintwrkMessengerSessionContent(
    status: SessionVerifiedStatus,
    phase: AntintwrkRecoveryStatus.Phase?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier) {
        if (!status.isVerified()) {
            val failed = phase == AntintwrkRecoveryStatus.Phase.Failed
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!failed) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = ElementTheme.colors.iconSecondary,
                    )
                }
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(
                        if (failed) CommonStrings.screen_ant_account_messages_retry else CommonStrings.screen_ant_account_messages_preparing
                    ),
                    style = ElementTheme.typography.fontBodySmRegular,
                    color = ElementTheme.colors.textSecondary,
                )
                if (failed) TextButton(stringResource(CommonStrings.action_retry), onClick = onRetry)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
    }
}

internal data class MessengerSessionPreviewState(
    val status: SessionVerifiedStatus,
    val phase: AntintwrkRecoveryStatus.Phase?,
)

internal class MessengerSessionPreviewProvider : PreviewParameterProvider<MessengerSessionPreviewState> {
    override val values = sequenceOf(
        MessengerSessionPreviewState(SessionVerifiedStatus.Unknown, AntintwrkRecoveryStatus.Phase.Working),
        MessengerSessionPreviewState(SessionVerifiedStatus.NotVerified, AntintwrkRecoveryStatus.Phase.Failed),
        MessengerSessionPreviewState(SessionVerifiedStatus.Verified, AntintwrkRecoveryStatus.Phase.Ready),
    )
}

@PreviewsDayNight
@Composable
internal fun AntintwrkMessengerSessionContentPreview(@PreviewParameter(MessengerSessionPreviewProvider::class) state: MessengerSessionPreviewState) {
    ElementPreview {
        AntintwrkMessengerSessionContent(state.status, state.phase, {}, Modifier.fillMaxSize()) {
            Text("Chats")
        }
    }
}
